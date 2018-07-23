package cn.chinatelecom.dp;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

public class DockerInvoker {



    String fromImageRes = "hub.chinatelecom.cn/public/mlp:0.1";
    String localFile = "/home/wu/Downloads/model2.py";




    public String getContainerIdUsingName(String containerName) {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();

        InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(containerName).exec();
        return containerInfo.getId();
    }

        public void invoke( ) throws InterruptedException,FileNotFoundException {


        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

            DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();

//        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(fromImageRes);
//        PullImageResultCallback callback = new PullImageResultCallback() {
//            @Override
//            public void onNext(PullResponseItem item) {
//                super.onNext(item);
//            }
//
//            @Override
//            public void onError(Throwable throwable) {
//                super.onError(throwable);
//            }
//        };
//        pullImageCmd.exec(callback).awaitSuccess();


          File file = new File(localFile);
            InputStream input = new FileInputStream(file);
            TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(input,"UTF-8");

          CreateContainerResponse container = dockerClient.createContainerCmd(fromImageRes).exec();
            dockerClient.copyArchiveToContainerCmd(getContainerIdUsingName("distracted_nobel")).withRemotePath("/tmp").withHostResource(localFile).withNoOverwriteDirNonDir(false).exec();
            System.out.println("Copying");

       // dockerClient.startContainerCmd(container.getId()).exec();

//        dockerClient.copyArchiveToContainerCmd(container.getId()).withRemotePath("/tmp").withTarInputStream(tarArchiveInputStream) .exec();
//        System.out.println("Copying");




//        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
//        ByteArrayOutputStream stderr = new ByteArrayOutputStream();


//        CreateContainerResponse container = dockerClient.copyArchiveFromContainerCmd(containerResp.getId(),"/home/wu/hello.txt")
//                .withHostPath("/tmp").exec();
//        dockerClient.startContainerCmd(container.getId()).exec();


//        String containerId = containerResp.getId();
//        dockerClient.startContainerCmd(containerId).exec();
//
//
//        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
//                .withAttachStdout(true)
//                .withAttachStderr(true)
//                .withCmd("ping", "8.8.8.8", "-c", "30")
//                .exec();
//        dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(
//                new ExecStartResultCallback(stdout, stderr)).awaitCompletion();


//        CreateContainerResponse container = dockerClient.createContainerCmd("hub.chinatelecom.cn/public/mlp:0.1")
//                .withImage("hub.chinatelecom.cn/public/mlp")
//                .withCmd("sleep", "9999")
//                .withName("container1")
//                .exec();
//
//        dockerClient.startContainerCmd(container.getId()).exec();
//
//        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(container.getId())
//                .withAttachStdout(true)
//                .withAttachStderr(true)
//                .withCmd("ping", "8.8.8.8", "-c", "30")
//                .exec();
//        dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(
//                new ExecStartResultCallback(stdout, stderr)).awaitCompletion();


//        DockerClient client = DockerClientBuilder.getInstance(config).build();
//        CreateContainerResponse containerResp = client.createContainerCmd("busybox")
//                .withImage("192.168.4.12:5000/my-ubuntu")
//                .withCmd("sleep", "9999")
//                .withAttachStderr(true)
//                .withAttachStdout(true)
//                .exec();
//        String containerId = containerResp.getId();
//        client.startContainerCmd(containerId).exec();
//
//        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
//        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
//
//        ExecCreateCmdResponse execCreateCmdResponse = client.execCreateCmd(containerId)
//                .withAttachStdout(true)
//                .withAttachStderr(true)
//                .withCmd("ping", "8.8.8.8", "-c", "30")
//                .exec();
//        client.execStartCmd(execCreateCmdResponse.getId()).exec(
//                new ExecStartResultCallback(stdout, stderr)).awaitCompletion();
    }

    public static  void main(String []args) throws InterruptedException,FileNotFoundException{

        DockerInvoker dockerInvoker = new DockerInvoker();
        dockerInvoker.invoke();

    }

}
