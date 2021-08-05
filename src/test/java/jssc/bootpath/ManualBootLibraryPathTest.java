package jssc.bootpath;

import jssc.SerialNativeInterface;
import org.junit.Test;
import org.scijava.nativelib.NativeLibraryUtil;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ManualBootLibraryPathTest {
    @Test
    public void testBootPathOverride() {
        /**
         * This must be in its own class to run in a separate JVM
         * - See also: https://stackoverflow.com/questions/68657855
         * - NativeLoader.loadLibrary(...) calls System.loadLibrary(...) which is static
         * - maven-surefire-plugin must be configured with reuseForks=false to use a new JVM for each class
         * - TODO: If JUnit adds JVM unloading between methods, this class can be consolidated
         */
        String nativeLibDir = NativeLibraryUtil.getPlatformLibraryPath(System.getProperty("user.dir") + "/target/cmake/natives/");
        System.setProperty("jssc.boot.library.path", nativeLibDir);
        try {
            final String nativeLibraryVersion = SerialNativeInterface.getNativeLibraryVersion();
            assertThat(nativeLibraryVersion, is(not(nullValue())));
            assertThat(nativeLibraryVersion, is(not("")));
        } catch (UnsatisfiedLinkError linkError) {
            linkError.printStackTrace();
            fail("Should be able to call method!");
        }
    }
}
