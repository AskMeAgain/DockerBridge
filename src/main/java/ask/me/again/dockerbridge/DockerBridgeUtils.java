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
  static void spawnInputThread(List<String> commands, PipedInputStream in, PipedOutputStream out) {
    new Thread(() -> {
      try (in; out) {
        for (int i = 1; i < commands.size(); i++) {

          String command = commands.get(i);

          if (CharMatcher.anyOf("_").matchesAllOf(command)) {
            Thread.sleep(command.length() * 100);
          } else {
            out.write((command + "\n").getBytes(StandardCharsets.UTF_8));
          }

        }
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
      }
    }).start();
  }

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
    var tty = true;
    var execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
        .withCmd(startCommand.split(" "))
        .withAttachStdout(true)
        .withAttachStderr(true)
        .withTty(tty)
        .withAttachStdin(true)
        .exec();

    dockerClient.execStartCmd(execCreateCmdResponse.getId())
        .withStdIn(stdin)
        .withTty(tty)
        .exec(new ResultCallback.Adapter<>() {
          @Override
          public void onNext(Frame frame) {
            log.add(new String(frame.getPayload()));
          }

          @Override
          public void onComplete() {
            shutdown.set(true);
          }
        })
        .awaitStarted();
  }

}
