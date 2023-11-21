package jssc;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

public class SerialNativeInterfaceTest {

    @Test
    public void testInitNativeInterface() {
        SerialNativeInterface serial = new SerialNativeInterface();

        long handle = -1;
        try {
            handle = serial.openPort("ttyS0",false);
            assertThat(handle, is(not(-1L)));
        } finally {
            if (handle != -1) {
                serial.closePort(handle);
            }
        }
    }

    @Test
    public void testPrintVersion() {
        try {
            final String nativeLibraryVersion = SerialNativeInterface.getNativeLibraryVersion();
            assertThat(nativeLibraryVersion, is(not(nullValue())));
            assertThat(nativeLibraryVersion, is(not("")));
        } catch (UnsatisfiedLinkError linkError) {
            linkError.printStackTrace();
            fail("Should be able to call method!");
        }

    }

    @Test(expected = java.io.IOException.class)
    public void reportsWriteErrorsAsIOException() throws Exception {
        Assume.assumeFalse(SerialNativeInterface.getOsType() == SerialNativeInterface.OS_WINDOWS);

        long fd = -1; /*bad file by intent*/
        byte[] buf = new byte[]{ 0x6A, 0x73, 0x73, 0x63, 0x0A };
        SerialNativeInterface testTarget = new SerialNativeInterface();
        testTarget.writeBytes(fd, buf);
    }

    @Test
    public void throwsIllegalArgumentExceptionIfPortHandleIllegal() throws Exception {
        assumeFalse(SerialNativeInterface.getOsType() == SerialNativeInterface.OS_MAC_OS_X);

        SerialNativeInterface testTarget = new SerialNativeInterface();
        try{
            testTarget.readBytes(999, 42);
            fail("Where is the exception?");
        }catch( IllegalArgumentException ex ){
            assertTrue(ex.getMessage().contains("EBADF"));
        }
    }

    /**
     * <p>This is a duplicate of {@link #throwsIllegalArgumentExceptionIfPortHandleIllegal()}
     * but targets osx only. Not yet analyzed why osx (using select) hangs here. See also <a
     * href="https://github.com/java-native/jssc/pull/155">PR 155</a>. Where this
     * was discovered.</p>
     *
     * <p>TODO: Go down that rabbit hole and get rid of that "bug".</p>
     */
    @Test
    @org.junit.Ignore("TODO analyze where this osx hang comes from")
    public void throwsIllegalArgumentExceptionIfPortHandleIllegalOsx() throws Exception {
        assumeTrue(SerialNativeInterface.getOsType() == SerialNativeInterface.OS_MAC_OS_X);

        SerialNativeInterface testTarget = new SerialNativeInterface();
        try{
            testTarget.readBytes(999, 42);
            fail("Where is the exception?");
        }catch( IllegalArgumentException ex ){
            assertTrue(ex.getMessage().contains("EBADF"));
        }
    }

}
