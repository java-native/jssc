package jssc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Reads the <code>JSSC-IDENT</code> line (see <code>src/main/cpp/ident.cpp</code>) of a native library without
 * loading it
 */
final class NativeIdent {

    static final List<String> LIBRARY_NAMES = Arrays.asList("libjssc.so", "libjssc.dylib", "jssc.dll");
    private static final String IDENT_MARKER = "JSSC-IDENT ";

    private NativeIdent() {
    }

    /**
     * Finds the one printable <code>JSSC-IDENT key=value ...</code> line in the binary
     */
    static Map<String, String> parse(File library) throws IOException {
        final InputStream in = new FileInputStream(library);
        final String content;
        try {
            content = new String(readFully(in), "ISO-8859-1");
        } finally {
            in.close();
        }
        final int start = content.indexOf(IDENT_MARKER);
        assertTrue("No " + IDENT_MARKER + "in " + library, start >= 0);
        assertEquals("More than one " + IDENT_MARKER + "in " + library, -1, content.indexOf(IDENT_MARKER, start + 1));
        int end = start;
        while (end < content.length() && content.charAt(end) >= 0x20 && content.charAt(end) < 0x7f) {
            end++;
        }
        final String line = content.substring(start + IDENT_MARKER.length(), end);
        final Map<String, String> fields = new HashMap<String, String>();
        for (String field : line.split(" ")) {
            final int eq = field.indexOf('=');
            assertTrue("Malformed field '" + field + "' in " + line, eq > 0);
            fields.put(field.substring(0, eq), field.substring(eq + 1));
        }
        return fields;
    }

    static byte[] readFully(InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
