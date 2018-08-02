package cn.chinatelecom.dp;

import com.alibaba.fastjson.JSON;
//import com.github.dockerjava.api.DockerClient;
//import com.github.dockerjava.api.command.CreateContainerResponse;
//import com.github.dockerjava.api.command.InspectContainerResponse;
//import com.github.dockerjava.api.exception.DockerException;
//import com.github.dockerjava.core.DefaultDockerClientConfig;
//import com.github.dockerjava.core.DockerClientBuilder;
//import com.github.dockerjava.core.DockerClientConfig;
//import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.http.HTTPBinding;


public class DockerStart {


    protected Logger logger = LoggerFactory.getLogger(DockerStart.class);


    private Set<String> availableConnections =
            new HashSet<>();
    private Set<String> usedConnections = new HashSet<String>();

    private Integer MAX_CONNECTIONs;

    private static String availableConn ="availableConn";

    private static String usedConn ="usedConn";

    private Jedis jedis ;

    private String dockerHost ="http://localhost:1111";

    private String containerCreateUrl = String.format("%s/containers/create",dockerHost);

    private String imageName;

    public DockerStart(Integer number, String imageName) throws InterruptedException, ExecutionException {
        this.MAX_CONNECTIONs = number;
        this.imageName = imageName;
        jedis = JedisPoolUtil.getJedis();

    }


//
    public Boolean isRunning(String containerId) {
        String isRunningUrl = String.format("%s/containers/%s/json",dockerHost,containerId);

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(new MediaType("application","json"));

        ParameterizedTypeReference<ContainerCreateRespBean>  parameterizedTypeReference =
                new ParameterizedTypeReference<ContainerCreateRespBean>(){};

        ResponseEntity<ContainerCreateRespBean> exchange = sendRequest(isRunningUrl,null,parameterizedTypeReference,HttpMethod.GET);
        if(exchange.getStatusCode().value() == 200){
            ContainerCreateRespBean containerCreateRespBean =  exchange.getBody();
            return containerCreateRespBean.getId() == containerId ? true:false;
        }else{
            return false;
        }
    }

//
    public boolean startContainer(String containerId) {
         String startContainerUrl = String.format("%s/containers/%s/start",dockerHost,containerId);

        Boolean isRun = isRunning(containerId);
        if (!isRun) {

            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.setContentType(new MediaType("application", "json"));

            HttpEntity<String> requestEntity = new HttpEntity<String>(containerId, requestHeaders);
            ParameterizedTypeReference<ContainerCreateRespBean> parameterizedTypeReference =
                    new ParameterizedTypeReference<ContainerCreateRespBean>() {
                    };

            ResponseEntity<ContainerCreateRespBean> exchange = sendRequest(startContainerUrl, null, parameterizedTypeReference,HttpMethod.POST);
            if (exchange.getStatusCode().value() == 204) {
                return true;
            }
        }
        return true;
    }
//
    public Boolean stopContainer(String containerId) {
        String stopContainerUrl = String.format("%s/containers/%s/stop",dockerHost,containerId);

        Boolean isRun = isRunning(containerId);
        System.out.println(isRun);

        if (isRun) {
            // Stop container
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.setContentType(new MediaType("application", "json"));

            HttpEntity<String> requestEntity = new HttpEntity<String>(containerId, requestHeaders);
            ParameterizedTypeReference<ContainerCreateRespBean> parameterizedTypeReference =
                    new ParameterizedTypeReference<ContainerCreateRespBean>() {
                    };

            ResponseEntity<ContainerCreateRespBean> exchange = sendRequest(stopContainerUrl, requestEntity, parameterizedTypeReference,HttpMethod.GET);
            if (exchange.getStatusCode().value() == 204) {
                return true;
            }
            return false;

        }
        return true;
    }


    private void removeActiveContainer(){

        Set<String> tempAvailableConnections = JSON.parse(jedis.get(availableConn)) != null ? new HashSet<String>((List<String>) JSON.parse(jedis.get(availableConn))):new HashSet<>();

        Set<String> tempUsedConnections = JSON.parse(jedis.get(usedConn)) !=null ? new HashSet<String>((List<String>) JSON.parse(jedis.get(usedConn))):new HashSet<>();
        for(String containerId:tempAvailableConnections){
            stopContainer(containerId);
        }

        for(String containerId:tempUsedConnections){
            stopContainer(containerId);
        }

        jedis.del(availableConn);
        jedis.del(usedConn);

    }

    public void startMultiContainer() {
         removeActiveContainer();
        for (int i = 0; i < this.MAX_CONNECTIONs; i++) {
            String containerId = createConnection();
            this.availableConnections.add(containerId);
        }

        jedis.set(availableConn,JSON.toJSONString(this.availableConnections));
    }


    public <T> ResponseEntity<T> sendRequest(String url,HttpEntity httpEntity,ParameterizedTypeReference<T> responseType,HttpMethod method){

        RestTemplateConfig restTemplateConfig = new RestTemplateConfig();
        RestTemplate restTemplate = restTemplateConfig.createRestTemplate();

        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());


        return restTemplate.exchange(url, method, httpEntity, responseType);
    }

    public  String createConnection() {

        ContainerCreateRequestBean containerCreateBean = new ContainerCreateRequestBean();
        containerCreateBean.setImage(imageName);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(new MediaType("application","json"));

        HttpEntity<ContainerCreateRequestBean> requestEntity = new HttpEntity<ContainerCreateRequestBean>(containerCreateBean, requestHeaders);
        ParameterizedTypeReference<ContainerCreateRespBean>  parameterizedTypeReference =
                new ParameterizedTypeReference<ContainerCreateRespBean>(){};

        ResponseEntity<ContainerCreateRespBean> exchange = sendRequest(containerCreateUrl,requestEntity,parameterizedTypeReference,HttpMethod.POST);
        if(exchange.getStatusCode().value() == 201){
            ContainerCreateRespBean containerCreateRespBean =  exchange.getBody();
            startContainer( containerCreateRespBean.getId());
            return containerCreateRespBean.getId();
        }else{
            return null;
        }
    }



    public synchronized String getConnection() {

        this.availableConnections = JSON.parse(jedis.get(availableConn)) != null ? new HashSet<String>((List<String>)  JSON.parse(jedis.get(availableConn))):new HashSet<>();
        this.usedConnections = JSON.parse(jedis.get(usedConn)) !=null ? new HashSet<String>((List<String>) JSON.parse(jedis.get(usedConn))):new HashSet<>();
        String containerId;
        if (this.availableConnections.size() == 0) {
            containerId = createConnection();
            this.usedConnections.add(containerId);
            jedis.set(usedConn,JSON.toJSONString(this.availableConnections));

        } else {
            List<String> availableConnectionsList = new ArrayList(availableConnections);
             containerId  = availableConnectionsList.remove(availableConnectionsList.size()-1);
             availableConnections.remove(containerId);
            this.usedConnections.add(containerId);
        }

        jedis.set(usedConn,JSON.toJSONString(this.usedConnections));
        jedis.set(availableConn,JSON.toJSONString(this.availableConnections));
        return containerId;

    }


    public boolean releaseConn(String containerId) throws InterruptedException, ExecutionException{
        if(null != containerId){
            this.availableConnections = JSON.parse(jedis.get(availableConn)) != null ? new HashSet<String>((List<String>)  JSON.parse(jedis.get(availableConn))):new HashSet<String>();
            this.usedConnections = JSON.parse(jedis.get(usedConn)) !=null ? new HashSet<String>((List<String>)  JSON.parse(jedis.get(usedConn))):new HashSet<String>();

            this.usedConnections.remove(containerId);
            this.availableConnections.add(containerId);
            jedis.set(availableConn,JSON.toJSONString(this.availableConnections));
            jedis.set(usedConn,JSON.toJSONString(this.usedConnections));

            return true;
        }


        return  false;
    }


    public void destroyConnection(){
        removeActiveContainer();
    }


    public static void main(String args[]) throws InterruptedException, ExecutionException {

         String imageName = "hub.chinatelecom.cn/public/mlp:0.2";

        DockerStart dockerStart = new DockerStart(2, imageName);
     // dockerStart.startMultiContainer();
        String containerId =  dockerStart.getConnection();
      System.out.println(containerId);

 // dockerStart.releaseConn("d2bd09ffdd039fae7ebf5945ccf7fe4a9e6d3d4307393bbfaf7c740287435156");
     //  dockerStart.destroyConnection();
    }

}
