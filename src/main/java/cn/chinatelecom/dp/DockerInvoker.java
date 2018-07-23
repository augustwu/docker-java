package cn.chinatelecom.dp;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.EventsResultCallback;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.*;
import java.util.List;

import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

public class DockerInvoker {


    String fromImageRes = "hub.chinatelecom.cn/public/mlp:0.1";
    String localPythonFile = "/home/wu/Downloads/model2.py";

    String trainFile = "/home/wu/Downloads/train.csv";
    String testFile = "/home/wu/Downloads/test.csv";

    String cmd = "/usr/bin/python /tmp/model2.py";

    String destFile = "/home/wu/Downloads/sub.csv";




    public static void unTar(TarArchiveInputStream tis, File destFile)
            throws IOException {
        TarArchiveEntry tarEntry = null;
        while ((tarEntry = tis.getNextTarEntry()) != null) {
            if (tarEntry.isDirectory()) {
                if (!destFile.exists()) {
                    destFile.mkdirs();
                }
            } else {
                FileOutputStream fos = new FileOutputStream(destFile);
                IOUtils.copy(tis, fos);
                fos.close();
            }
        }
        tis.close();
    }

    public String getContainerIdUsingName(String containerName) {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();

        InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(containerName).exec();
        return containerInfo.getId();
    }

    public void invoke() throws InterruptedException, FileNotFoundException,IOException {


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



        EventsResultCallback callback = new EventsResultCallback() {
            @Override
            public void onNext(Event event) {
                System.out.println("Event: " + event);
                super.onNext(event);
            }
        };


        String containerId =  getContainerIdUsingName("lucid_morse");
        dockerClient.copyArchiveToContainerCmd(containerId).withRemotePath("/tmp").withHostResource(trainFile).withNoOverwriteDirNonDir(false).exec();
        System.out.println("Copying");

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();


        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true)
               .withAttachStderr(true)
                .withAttachStdin(true)
                .withCmd("/usr/bin/python", "/tmp/model2.py").exec();

        System.out.println(execCreateCmdResponse);

        dockerClient.execStartCmd(execCreateCmdResponse.getId())
                            .withDetach(false).
        exec(new ExecStartResultCallback(stdout, stderr)).awaitCompletion();

        System.out.println(stdout);
        System.out.println("-----------------");
        System.out.println(stderr);


        // Copy file from container
        TarArchiveInputStream tarStream = new TarArchiveInputStream(
                dockerClient.copyArchiveFromContainerCmd(containerId, "/tmp/temp/submission_1532335035.csv").exec());
        try {
            unTar(tarStream, new File(destFile));
        } finally {
            tarStream.close();
        }


//        // Copy file from container
//        InputStream inputStream = dockerClient
//                .copyArchiveFromContainerCmd(containerId, "/tmp/temp/submission_1532335035.csv")
//                .withHostPath("/tmp")
//                .exec();
//        inputStream.available();
//        copyInputStreamToFile(inputStream, new File("/home/wu/Downloads/submission_1532335035.csv"));



//        dockerClient.startContainerCmd(containerId).exec();
//
//        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
//        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
//
//        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
//                .withAttachStdout(true)
//                .withAttachStderr(true)
//                .withCmd("ping", "8.8.8.8", "-c", "30")
//                .exec();
//        dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(
//                new ExecStartResultCallback(stdout, stderr)).awaitCompletion();
//



//        CreateContainerResponse container = dockerClient.copyArchiveFromContainerCmd(containerResp.getId(),"/home/wu/hello.txt")
//                .withHostPath("/tmp").exec();
//        dockerClient.startContainerCmd(container.getId()).exec();

//        dockerClient.startContainerCmd(container.getId()).exec();
//
//        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(container.getId())
//                .withAttachStdout(true)
//                .withAttachStderr(true)
//                .withCmd("ping", "8.8.8.8", "-c", "30")
//                .exec();
//        dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(
//                new ExecStartResultCallback(stdout, stderr)).awaitCompletion();


    }

    public static void main(String[] args) throws InterruptedException, FileNotFoundException,IOException {

        DockerInvoker dockerInvoker = new DockerInvoker();
        dockerInvoker.invoke();

    }

}
