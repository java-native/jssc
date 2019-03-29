package jssc;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SerialNativeInterfaceTest {


    @Test
    public void testInitNativeInterface() {
        SerialNativeInterface serial = new SerialNativeInterface();

        String version = SerialNativeInterface.getNativeLibraryVersion();

        assertThat(version, is("2.8.1"));

    }

}
