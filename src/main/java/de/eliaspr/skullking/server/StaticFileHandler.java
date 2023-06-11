package de.eliaspr.skullking.server;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class StaticFileHandler {

    // TODO Add cache for file content
    public static byte[] readeFileContents(String file) throws IOException {
        try (var stream = SkullKingServer.class.getResourceAsStream("/" + file)) {
            if (stream == null) {
                throw new FileNotFoundException(file);
            }

            var buffer = new ByteArrayOutputStream();
            int nRead;
            var data = new byte[1024];
            while ((nRead = stream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
            return buffer.toByteArray();
        } catch (IOException e) {
            if (e instanceof FileNotFoundException) {
                throw e;
            } else {
                throw new IOException("Could not load file from classpath: " + file, e);
            }
        }
    }

    public static String readFileAsString(String file) throws IOException {
        return new String(readeFileContents(file));
    }
}
