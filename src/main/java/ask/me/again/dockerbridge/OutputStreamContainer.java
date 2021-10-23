package ask.me.again.dockerbridge;

import java.io.InputStream;
import java.io.OutputStream;

public record OutputStreamContainer(OutputStream userWritable, OutputStream responseStream) {
}
