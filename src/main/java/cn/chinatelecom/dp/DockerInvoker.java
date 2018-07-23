package cn.chinatelecom.dp;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.DockerException;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

public class DockerInvoker {


    String fromImageRes = "hub.chinatelecom.cn/public/mlp:0.1";
    String localPythonFile = "/home/wu/Downloads/model2.py";

    String trainFile = "/home/wu/Downloads/train.csv";
    String testFile = "/home/wu/Downloads/test.csv";

    String cmd = "/usr/bin/python /tmp/model2.py";


    String dockerName = "mlp";




    public void copyFiletoContainer(Map<String,String> files,String dockerName){
        DockerClient dockerClient = getDockerClient();
        String containerId = getContainerIdUsingName(dockerName);

        for(Map.Entry<String, String> entry : files.entrySet()){
            String source = entry.getKey();
            String target = entry.getValue();
            System.out.println(source);
            System.out.println(target);
            dockerClient.copyArchiveToContainerCmd(containerId).withRemotePath(target).withHostResource(source).withNoOverwriteDirNonDir(false).exec();
        }

        System.out.println("Copying Success");

    }

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

    public void invoke(String dockerName,String[] command,String destFile) throws InterruptedException, FileNotFoundException, IOException {

        DockerClient dockerClient = getDockerClient();
        String containerId = getContainerIdUsingName(dockerName);


        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        Boolean isRun = isRunning(containerId);
        if (!isRun) {
            // Start container
            dockerClient.startContainerCmd(containerId).exec();
        }

        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withAttachStdin(true)
                .withCmd(command).exec();

        System.out.println(execCreateCmdResponse);

        dockerClient.execStartCmd(execCreateCmdResponse.getId())
                .withDetach(false).
                exec(new ExecStartResultCallback(stdout, stderr)).awaitCompletion();

        System.out.println(stdout);
        System.out.println("-----------------");
        System.out.println(stderr);
        System.out.println("Exec Python Success");


        // Copy file from container
        TarArchiveInputStream tarStream = new TarArchiveInputStream(
                dockerClient.copyArchiveFromContainerCmd(containerId, "/tmp/temp/submission_1532335035.csv").exec());
        try {
            unTar(tarStream, new File(destFile));
        } finally {
            tarStream.close();
        }

        // Stop container
        dockerClient.killContainerCmd(containerId).exec();

        //  Remove container
        dockerClient.removeContainerCmd(containerId).exec();

    }

    public static void main(String[] args) throws InterruptedException, FileNotFoundException, IOException {
        Map files = new HashMap<String,String>();
        files.put("/home/wu/Downloads/model2.py","/tmp");
        files.put("/home/wu/Downloads/test.csv","/tmp/temp");
        files.put("/home/wu/Downloads/train.csv","/tmp/temp");


        String dockerName = "mlp";
        final String[] command = {"/usr/bin/python", "/tmp/model2.py"};
        String destFile = "/home/wu/Downloads/sub.csv";

        DockerInvoker dockerInvoker = new DockerInvoker();

        dockerInvoker.copyFiletoContainer(files,dockerName);
        dockerInvoker.invoke(dockerName,command,destFile);

    }

}
