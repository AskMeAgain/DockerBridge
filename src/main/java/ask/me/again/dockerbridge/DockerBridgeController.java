package ask.me.again.dockerbridge;

import com.github.dockerjava.api.model.Container;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;


@RestController
@RequestMapping("/container")
public class DockerBridgeController {

  private final HashMap<String, OutputStreamContainer> streams = new HashMap<>();

  @GetMapping("/{containerId}/session")
  public ResponseEntity<InputStreamResource>  openSession(
      @PathVariable("containerId") String containerId
      //@RequestBody String startCommand
  ) throws IOException, InterruptedException {
    var startCommand = "bash";
    System.out.println("Opened session");
    if (streams.containsKey(containerId)) {
      streams.get(containerId).responseStream().close();
      streams.get(containerId).userWritable().close();
    }

    var inUser = new PipedInputStream();
    var userWritable = new PipedOutputStream(inUser);

    var inLog = new PipedInputStream();
    var logWritable = new PipedOutputStream(inLog);

    streams.put(containerId, new OutputStreamContainer(userWritable, logWritable));

    DockerBridgeUtils.createTty(containerId, startCommand, logWritable, inUser);

    System.out.println("returned!");

    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_PLAIN)
        .body(new InputStreamResource(inLog));

  }

  @PostMapping("/{containerId}/write")
  public void write(
      @PathVariable("containerId") String containerId,
      @Nullable @RequestBody String command
  ) throws IOException {
    System.out.println("Writing command: " + command);
    streams.get(containerId).userWritable().write((command+"\n").getBytes(StandardCharsets.UTF_8));
    streams.get(containerId).responseStream().write("\n------------------\n".getBytes(StandardCharsets.UTF_8));
    streams.get(containerId).userWritable().flush();
    streams.get(containerId).responseStream().flush();
  }

//  @GetMapping("/{containerId}/log-stream")
//  public void logsStream(@PathVariable("containerId") String containerId, HttpServletResponse response) throws InterruptedException, IOException {
//    var instance = DockerBridgeUtils.getInstance();
//    var outputStream = response.getOutputStream();
//
//    instance.logContainerCmd(containerId)
//        .withFollowStream(true)
//        .withStdOut(true)
//        .withStdErr(true)
//        .exec(new ExecStartResultCallback(outputStream, outputStream))
//        .awaitCompletion();
//  }
//
//  @PostMapping("/{containerId}/tty")
//  public String tty(@PathVariable("containerId") String containerId, @RequestBody List<String> commands) throws InterruptedException, IOException {
//    try (var outputStream = new ByteArrayOutputStream()) {
//      DockerBridgeUtils.createTty(containerId, commands, outputStream);
//      return outputStream.toString();
//    }
//  }

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
