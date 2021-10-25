package ask.me.again.dockerbridge;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.google.common.base.CharMatcher;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DockerBridgeUtils {

  static String getContainerState(String containerId, DockerClient instance) {
    return instance.inspectContainerCmd(containerId).exec().getState().getStatus();
  }

  static DockerClient getInstance() {
    var config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    var httpClient = new ApacheDockerHttpClient.Builder()
        .dockerHost(config.getDockerHost())
        .sslConfig(config.getSSLConfig())
        .build();
    return DockerClientImpl.getInstance(config, httpClient);
  }

  static void createTty(String containerId, String startCommand, ConcurrentLinkedQueue<String> log, AtomicBoolean shutdown, PipedInputStream stdin) throws InterruptedException {
    var dockerClient = DockerBridgeUtils.getInstance();
    var execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
        .withCmd(startCommand.split(" "))
        .withAttachStdout(true)
        .withAttachStderr(true)
        .withTty(true)
        .withAttachStdin(true)
        .exec();

    dockerClient.execStartCmd(execCreateCmdResponse.getId())
        .withStdIn(stdin)
        .withTty(false)
        .exec(new ResultCallback.Adapter<>() {
          @Override
          public void onNext(Frame frame) {
            log.add(new String(frame.getPayload(), StandardCharsets.UTF_8));
          }

          @Override
          public void onComplete() {
            shutdown.set(true);
          }
        })
        .awaitStarted();
  }

}
