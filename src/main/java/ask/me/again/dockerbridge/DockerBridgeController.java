package ask.me.again.dockerbridge;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;


@RestController
@RequestMapping("/container")
public class DockerBridgeController {

  @GetMapping("/{containerId}/log-stream")
  public void logsStream(@PathVariable("containerId") String containerId, HttpServletResponse response) throws InterruptedException, IOException {
    var instance = DockerBridgeUtils.getInstance();
    var outputStream = response.getOutputStream();

    instance.logContainerCmd(containerId)
        .withFollowStream(true)
        .withStdOut(true)
        .withStdErr(true)
        .exec(new ExecStartResultCallback(outputStream, outputStream))
        .awaitCompletion();
  }

  @PostMapping("/{containerId}/tty")
  public String tty(@PathVariable("containerId") String containerId, @RequestBody List<String> commands) throws InterruptedException, IOException {
    try (var outputStream = new ByteArrayOutputStream()) {
      DockerBridgeUtils.createTty(containerId, commands, outputStream);
      return outputStream.toString();
    }
  }

  @GetMapping("/list")
  public List<Container> list() {
    return DockerBridgeUtils.getInstance()
        .listContainersCmd()
        .withShowAll(true)
        .exec();
  }

  @PostMapping("/{containerId}/command")
  public String command(@PathVariable("containerId") String containerId, @RequestBody String command) {
    var instance = DockerBridgeUtils.getInstance();

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

    return DockerBridgeUtils.getContainerState(containerId, instance);
  }
}
