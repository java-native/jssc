package jssc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads the version maven filters into <code>jssc-version.properties</code> next to this class.
 */
final class LibraryVersion {
    private static final String RESOURCE = "jssc-version.properties";
    private static final String UNKNOWN = "0.0.0-UNKNOWN";

    private LibraryVersion() {}

    static String get() {
        final InputStream in = LibraryVersion.class.getResourceAsStream(RESOURCE);
        if (in == null) {
            return UNKNOWN;
        }
        final Properties properties = new Properties();
        try {
            try {
                properties.load(in);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            return UNKNOWN;
        }
        return properties.getProperty("version", UNKNOWN);
    }
}
