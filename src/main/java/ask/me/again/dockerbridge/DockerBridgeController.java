package ask.me.again.dockerbridge;

import com.github.dockerjava.api.model.Container;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;


@RestController
@RequestMapping("/container")
public class DockerBridgeController {

  private final HashMap<String, OutputStreamContainer> streams = new HashMap<>();

  public record OutputStreamContainer(OutputStream userWritable, AtomicBoolean shutdown) {
  }

  @PostMapping("/{containerId}/session")
  public ResponseEntity<StreamingResponseBody> openSession(
      @PathVariable("containerId") String containerId,
      @RequestBody String startCommand
  ) throws IOException, InterruptedException {

    if (streams.containsKey(containerId)) {
      var outputStreamContainer = streams.get(containerId);
      outputStreamContainer.shutdown().set(true);
      outputStreamContainer.userWritable().close();
    }

    var inUser = new PipedInputStream();
    var userWritable = new PipedOutputStream(inUser);

    var shutdown = new AtomicBoolean(false);
    var tobeFlushed = new ConcurrentLinkedQueue<String>();

    streams.put(containerId, new OutputStreamContainer(userWritable, shutdown));

    DockerBridgeUtils.createTty(containerId, startCommand, tobeFlushed, shutdown, inUser);

    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_EVENT_STREAM)
        .body(outputStream -> {
          while (!shutdown.get()) {
            var item = tobeFlushed.poll();
            if (item != null) {
              outputStream.write(item
                  .replaceAll("\u001B\\[[;\\d]*m", "")
                  .replaceAll("]0;", "")
                  .getBytes());
              outputStream.flush();
            }
          }
        });
  }

  @PostMapping("/{containerId}/write")
  public void write(
      @PathVariable("containerId") String containerId,
      @Nullable @RequestBody String command
  ) throws IOException {
    streams.get(containerId).userWritable().write((command + "\n").getBytes(StandardCharsets.UTF_8));
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
