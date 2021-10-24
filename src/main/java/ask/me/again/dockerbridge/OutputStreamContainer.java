package ask.me.again.dockerbridge;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public record OutputStreamContainer(OutputStream userWritable, AtomicBoolean shutdown) {
}
