package ask.me.again.dockerbridge;

import java.util.List;

public class DockerCommand {

  List<String> startCommand;
  List<String> inputCommands;

  public DockerCommand(List<String> startCommand, List<String> inputCommands) {
    this.startCommand = startCommand;
    this.inputCommands = inputCommands;
  }

}
