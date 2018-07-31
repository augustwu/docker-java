package cn.chinatelecom.dp;

import com.alibaba.fastjson.JSON;
import com.coreos.jetcd.Client;
import com.coreos.jetcd.KV;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.kv.GetResponse;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.SearchImagesCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.SearchItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.apache.commons.io.output.ByteArrayOutputStream;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


public class DockerStart {


    private String imageName;

    private Set<String> availableConnections =
            new HashSet<>();
    private Set<String> usedConnections = new HashSet<String>();

    private Integer MAX_CONNECTIONs;

    private static String availableConn ="availableConn";

    private static String usedConn ="usedConn";

    private Jedis jedis ;


    public DockerStart(Integer number, String imageName) throws InterruptedException, ExecutionException {
        this.MAX_CONNECTIONs = number;
        this.imageName = imageName;
        jedis = JedisPoolUtil.getJedis();


    }


    public static DockerClient getDockerClient() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();
        return dockerClient;
    }

    public String getContainerIdUsingName(String containerName) {
        DockerClient dockerClient = getDockerClient();
        InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(containerName).exec();
        return containerInfo.getId();
    }


    public Boolean isRunning(String containerId) {
        DockerClient dockerClient = getDockerClient();
        try {
            return containerId != null && dockerClient.inspectContainerCmd(containerId).exec().getState().getRunning();
        } catch (DockerException e) {
            return false;
        }
    }


    public void startContainer(String dockerName) {

        DockerClient dockerClient = getDockerClient();
        String containerId = getContainerIdUsingName(dockerName);


        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        Boolean isRun = isRunning(containerId);
        if (!isRun) {
            // Start container
            dockerClient.startContainerCmd(containerId).exec();
        }
    }

    public void stopContainer(String containerId) {

        DockerClient dockerClient = getDockerClient();
        Boolean isRun = isRunning(containerId);
        System.out.println(isRun);
        if (isRun) {
            // Stop container
            dockerClient.stopContainerCmd(containerId).exec();
        }
    }


    private void removeActiveContainer(){

        Set<String> tempAvailableConnections = JSON.parse(jedis.get(availableConn)) != null ? (Set<String>) JSON.parse(jedis.get(availableConn)):new HashSet<>();

        Set<String> tempUsedConnections = JSON.parse(jedis.get(usedConn)) !=null ? (Set<String>) JSON.parse(jedis.get(usedConn)):new HashSet<>();
        for(String containerId:tempAvailableConnections){
            System.out.println(containerId);
            stopContainer(containerId);
        }

        for(String containerId:tempUsedConnections){
            System.out.println(containerId);
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

    public String createConnection() {
        DockerClient dockerClient = DockerStart.getDockerClient();
        CreateContainerResponse container = dockerClient.createContainerCmd(this.imageName).exec();
        dockerClient.startContainerCmd(container.getId()).exec();
        return container.getId();
    }



    public String getConnection() {

        this.availableConnections = JSON.parse(jedis.get(availableConn)) != null ? (Set<String>) JSON.parse(jedis.get(availableConn)):new HashSet<>();
        this.usedConnections = JSON.parse(jedis.get(usedConn)) !=null ? (Set<String>) JSON.parse(jedis.get(usedConn)):new HashSet<>();
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


//    public static KV getKvClient() {
//        Client client = Client.builder().endpoints(ETC_ADDR).build();
//        KV kvClient = client.getKVClient();
//        return kvClient;
//    }
//
//    private void setValueToEtcd(String kid, List<String> containerList) throws InterruptedException, ExecutionException {
//        KV kvClient = DockerStart.getKvClient();
//        ByteSequence key = ByteSequence.fromString(kid);
//        ByteSequence value = ByteSequence.fromString(containerList.toString());
//
//        // put the key-value
//        kvClient.put(key, value).get();
//
//    }

//
//    public String getRandomContainerId(List<String> containerList) {
//        Random random = new Random();
//        String randomId = containerList.get(random.nextInt(containerList.size()));
//
//        return randomId;
//    }

//
//    public List<KeyValue> getValueFromEtcd(String key) throws InterruptedException, ExecutionException{
//        KV kvClient = DockerStart.getKvClient();
//        ByteSequence byteSequence = ByteSequence.fromString(key);
//
//
//        CompletableFuture<GetResponse> getFuture = kvClient.get(byteSequence);
//
//        // get the value from CompletableFuture
//        GetResponse response = getFuture.get();
//        List<KeyValue> kvs = response.getKvs();
//
//        return kvs ;
//    }
//
//    private Boolean isKeyExists(String key) throws InterruptedException, ExecutionException {
//        KV kvClient = DockerStart.getKvClient();
//
//        ByteSequence byteSequence = ByteSequence.fromString(key);
//        CompletableFuture<GetResponse> futureResponse =
//                kvClient.get(byteSequence);
//
//
//        GetResponse response = futureResponse.get();
//
//        if (response.getKvs().isEmpty()) {
//            return false;
//        }
//        return true;
//
//    }
//
//    private void deleteKey(String key) throws InterruptedException, ExecutionException {
//        KV kvClient = DockerStart.getKvClient();
//        ByteSequence byteSequence = ByteSequence.fromString(key);
//
//        kvClient.delete(byteSequence).get();
//
//    }

    public boolean releaseConn(String containerId) throws InterruptedException, ExecutionException{
        if(null != containerId){
            this.availableConnections = JSON.parse(jedis.get(availableConn)) != null ? (Set<String>) JSON.parse(jedis.get(availableConn)):new HashSet<String>();
            this.usedConnections = JSON.parse(jedis.get(usedConn)) !=null ? (Set<String>) JSON.parse(jedis.get(usedConn)):new HashSet<String>();

            this.usedConnections.remove(containerId);
            this.availableConnections.add(containerId);
            jedis.set(availableConn,JSON.toJSONString(this.availableConnections));
            jedis.set(usedConn,JSON.toJSONString(this.usedConnections));

            //stopContainer(containerId);
            return true;
        }


        return  false;
    }


    public static void main(String args[]) throws InterruptedException, ExecutionException {
        String imageName = "hub.chinatelecom.cn/public/mlp:0.2";
        DockerStart dockerStart = new DockerStart(2, imageName);
      dockerStart.startMultiContainer();
      String containerId =  dockerStart.getConnection();
//        System.out.println(containerId);

    //dockerStart.releaseConn("4e7980f04cf6fc692da33658f29253edcc6fe6be0e49f95093f536297da132bd");
    }

}
