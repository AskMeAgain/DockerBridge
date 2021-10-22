package ask.me.again.dockerbridge;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.InvocationBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.websocket.server.PathParam;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


@RequestMapping("/container")
@RestController
public class DockerBridgeController {

  @PostMapping("/{containerId}/bash")
  public String bash(@PathVariable("containerId") String containerId, @RequestBody String command) throws InterruptedException {

    var dockerClient = getInstance();

    var execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
        .withCmd("bash", "-c", command)
        .withAttachStdout(true)
        .exec();

    var outputStream = new ByteArrayOutputStream();
    var outputStream2 = new ByteArrayOutputStream();

    dockerClient.execStartCmd(execCreateCmdResponse.getId())
        .exec(new ExecStartResultCallback(outputStream, outputStream2))
        .awaitCompletion();

    return outputStream2.toString() + outputStream.toString();
  }

  @PostMapping("/{containerId}/exec")
  public String exec(@PathVariable("containerId") String containerId, @RequestBody DockerCommand command) throws InterruptedException, IOException {
    var dockerClient = getInstance();

    var execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
        .withCmd(command.startCommand.toArray(String[]::new))
        .withAttachStdout(true)
        .withAttachStderr(true)
        .withTty(true)
        .withAttachStdin(true)
        .exec();

    try (var outputStream = new ByteArrayOutputStream()) {
      var in = new PipedInputStream();
      var out = new PipedOutputStream(in);

      spawnInputThread(command, outputStream, in, out);

      dockerClient.execStartCmd(execCreateCmdResponse.getId())
          .withStdIn(in)
          .withTty(true)
          .exec(new ExecStartResultCallback(outputStream, outputStream))
          .awaitCompletion(10, TimeUnit.SECONDS);

      return outputStream.toString();
    }

  }

  private void spawnInputThread(DockerCommand command, ByteArrayOutputStream outputStream, PipedInputStream in, PipedOutputStream out) {
    new Thread(() -> {
      try {
        for (String x : command.inputCommands) {
          if (x.equals("__")) {
            Thread.sleep(1000);
          } else {
            out.write((x + "\n").getBytes(StandardCharsets.UTF_8));
          }
        }
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
      } finally {
        try {
          in.close();
          out.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }).start();
  }

  @GetMapping("/list")
  public List<Container> list() {
    return getInstance()
        .listContainersCmd()
        .withShowAll(true)
        .exec();
  }

  @PostMapping("/{containerId}/command")
  public String command(@PathVariable("containerId") String containerId, @RequestBody String command) {
    var instance = getInstance();

    switch (command) {
      case "stop" -> instance.stopContainerCmd(containerId)
          .exec();
      case "start" -> instance.startContainerCmd(containerId)
          .exec();
      case "remove" -> {
        instance.removeContainerCmd(containerId)
            .withForce(true)
            .exec();
        return "";
      }
      case "restart" -> instance.restartContainerCmd(containerId)
          .exec();
    }

    return getContainerState(containerId, instance);
  }

  private String getContainerState(String containerId, DockerClient instance) {
    return instance.inspectContainerCmd(containerId).exec().getState().getStatus();
  }

  private DockerClient getInstance() {
    var config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    var httpClient = new ApacheDockerHttpClient.Builder()
        .dockerHost(config.getDockerHost())
        .sslConfig(config.getSSLConfig())
        .build();
    return DockerClientImpl.getInstance(config, httpClient);
  }
}
