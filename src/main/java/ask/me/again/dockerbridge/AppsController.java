package ask.me.again.dockerbridge;

import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/apps")
public class AppsController {

  @ResponseBody
  @GetMapping("/shell")
  public byte[] shellApp() throws IOException {

    return IOUtils.toByteArray(this.getClass().getClassLoader().getResourceAsStream("shell.js"));
  }

}
