package jssc;

import jssc.junit.rules.DisplayMethodNameRule;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.slf4j.LoggerFactory.getLogger;;

public class SerialNativeInterfaceTest extends DisplayMethodNameRule {

    private static final Logger log = getLogger(SerialNativeInterfaceTest.class);

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
    public void throwsIfCountNegative() throws Exception {
        SerialNativeInterface testTarget = new SerialNativeInterface();
        byte[] ret;
        try{
            ret = testTarget.readBytes(0, -42);
            fail("Where's the exception?");
        }catch( IllegalArgumentException ex ){
            assertTrue(ex.getMessage().contains("-42"));
        }
    }

    /**
     * I think this case should just throw an exception, as trying to read zero
     * bytes doesn't make much sense to me. But it seems we need to accept a
     * "read of zero bytes" as a legal case. So jssc will respond with an empty
     * array, exactly as caller did request.
     * See also "https://github.com/java-native/jssc/issues/192".
     * 
     * Update: According to
     * https://github.com/java-native/jssc/issues/192#issuecomment-2960137775
     * there seems to exist some other issue related to events, which occasionly
     * provocates zero-length reads. So as long this other issue exists, jssc
     * probably should handle zero-length reads, as it seems to cause them
     * itself. */
    @Test
    public void returnsAnEmptyArrayIfCountIsZero() throws Exception {
        SerialNativeInterface testTarget = new SerialNativeInterface();
        byte[] ret = testTarget.readBytes(0, 0);
        assertNotNull(ret);
        assertTrue(ret.length == 0);
    }

    @Test
    @org.junit.Ignore("This test only makes sense if it is run in a situation"
        +" where large memory allocations WILL fail (for example you could use"
        +" a virtual machine with low memory available). Because on regular"
        +" machines allocating 2GiB of RAM is not a problem at all and so the"
        +" test run will just happily wait infinitely for those 2GiB to arrive"
        +" at the stdin fd. Feel free to remove this test if you think it"
        +" doesn't make sense to have it here.")
    public void throwsIfRequestTooLarge() throws Exception {
        SerialNativeInterface testTarget = new SerialNativeInterface();
        int tooLargeSize = Integer.MAX_VALUE;
        try{
            byte[] ret = testTarget.readBytes(0, tooLargeSize);
            fail("Where's the exception?");
        }catch( RuntimeException ex ){
            log.debug("Thrown, as expected :)", ex);
            assertTrue(ex.getMessage().contains(String.valueOf(tooLargeSize)));
        }
    }

}
