package jssc.bootpath;

import jssc.SerialNativeInterface;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ManualBootLibraryPathFailedTest {
    @Test
    public void testBootPathOverride() {
        /**
         * This must be in its own class to run in a separate JVM
         * - See also: https://stackoverflow.com/questions/68657855
         * - NativeLoader.loadLibrary(...) calls System.loadLibrary(...) which is static
         * - maven-surefire-plugin must be configured with reuseForks=false to use a new JVM for each class
         * - TODO: If JUnit adds JVM unloading between methods, this class can be consolidated
         */
        String nativeLibDir = "/"; // This should be valid on all platforms
        System.setProperty("jssc.boot.library.path", nativeLibDir);
        try {
            SerialNativeInterface.getNativeLibraryVersion();
            fail("Library loading should fail if path provided exists but does not contain a native library");
        } catch (UnsatisfiedLinkError ignore) {
            assertTrue("Library loading failed as expected with an invalid jssc.boot.library.path", true);
        }
    }
}
