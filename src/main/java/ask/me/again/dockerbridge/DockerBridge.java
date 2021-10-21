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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


@RequestMapping("/container")
@RestController
public class DockerBridge {

  @GetMapping("/{containerId}/exec")
  public String exec(@PathVariable("containerId") String containerId, @RequestBody String command) throws InterruptedException {

    var dockerClient = getInstance();

    var execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
        .withCmd("bash", "-c", command)
        .withAttachStdout(true)
        .exec();

    var outputStream = new ByteArrayOutputStream();

    dockerClient.execStartCmd(execCreateCmdResponse.getId())
        .exec(new ExecStartResultCallback(outputStream, outputStream))
        .awaitCompletion();

    return outputStream.toString();
  }

  @GetMapping("/list")
  public List<Container> list() {
    return getInstance()
        .listContainersCmd()
        .withShowAll(true)
        .exec();
  }

  @GetMapping("/{containerId}/stop")
  public String stop(@PathVariable("containerId") String containerId) {
    var instance = getInstance();

    instance.stopContainerCmd(containerId)
        .exec();

    return getContainerState(containerId, instance);
  }

  @GetMapping("/{containerId}/start")
  public String start(@PathVariable("containerId") String containerId) {
    var instance = getInstance();

    instance.startContainerCmd(containerId)
        .exec();

    return getContainerState(containerId, instance);
  }

  @GetMapping("/{containerId}/restart")
  public String restart(@PathVariable("containerId") String containerId) {
    var instance = getInstance();

    instance.restartContainerCmd(containerId)
        .exec();

    return getContainerState(containerId, instance);
  }

  @GetMapping("/{containerId}/remove")
  public String remove(@PathVariable("containerId") String containerId) {
    var instance = getInstance();

    instance.removeContainerCmd(containerId)
        .withForce(true)
        .exec();

    return "";
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
