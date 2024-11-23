package jssc;

import jssc.junit.rules.DisplayMethodNameRule;
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

public class SerialNativeInterfaceTest extends DisplayMethodNameRule {

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

    @Test(expected = java.lang.NullPointerException.class)
    public void throwsNpeIfPassedBufferIsNull() throws Exception {
        new SerialNativeInterface().writeBytes(1, null);
    }

    @Test
    public void throwsIaeIfCountNegative() throws Exception {
        SerialNativeInterface testTarget = new SerialNativeInterface();
        byte[] ret;
        try{
            ret = testTarget.readBytes(0, -42);
            fail("Where's the exception?");
        }catch( IllegalArgumentException ex ){
            assertTrue(ex.getMessage().contains("-42"));
        }
    }

    @Test
    @org.junit.Ignore("This test only makes sense if it is run in a situation"
        +" where large memory allocations WILL fail (for example you could use"
        +" a virtual machine with low memory available). Because on regular"
        +" machines allocating 2GiB of RAM is not a problem at all and so the"
        +" test run will just happily wait infinitely for those 2GiB to arrive"
        +" at the stdin fd. Feel free to remove this test if you think it"
        +" doesn't make sense to have it here.")
    public void throwsOOMExIfRequestTooLarge() throws Exception {
        SerialNativeInterface testTarget = new SerialNativeInterface();
        try{
            byte[] ret = testTarget.readBytes(0, Integer.MAX_VALUE);
            fail("Where's the exception?");
        }catch( OutOfMemoryError ex ){
            assertTrue(ex.getMessage().contains(String.valueOf(Integer.MAX_VALUE)));
        }
    }

}
