/**
 * License: https://opensource.org/licenses/BSD-3-Clause
 */
package jssc;

import org.scijava.nativelib.DefaultJniExtractor;
import org.scijava.nativelib.JniExtractor;

import java.io.File;
import java.io.IOException;

/**
 * @author A. Tres Finocchiaro
 *
 * Wrapper around DefaultJniExtractor class to allow native-lib-loader to conditionally
 * use a statically defined native search path when provided.
 */
public class DefaultJniExtractorWrapper {
    private File bootPath;
    private String library;
    private JniExtractor extractor;

    public DefaultJniExtractorWrapper(String library, String bootPath) throws IOException {
        if(bootPath != null) {
            File bootTest = new File(bootPath);
            if(bootTest.exists()) {
                this.bootPath = bootTest;
                this.library = library;
                extractor = new StubJniExtractor();
                return;
            }
        }
        extractor = new DefaultJniExtractor(null);
    }

    public JniExtractor getExtractor() {
        return extractor;
    }

    public class StubJniExtractor implements JniExtractor {
        @Override
        public File extractJni(String s, String s1) {
            switch(SerialNativeInterface.getOsType()) {
                case SerialNativeInterface.OS_WINDOWS:
                    return new File(bootPath, library + ".dll");
                case SerialNativeInterface.OS_MAC_OS_X:
                    return new File(bootPath, "lib" + library + ".dylib");
                default:
                    return new File(bootPath, "lib" + library + ".so");
            }
        }

        @Override
        public void extractRegistered() {} // no-op
    }
}
