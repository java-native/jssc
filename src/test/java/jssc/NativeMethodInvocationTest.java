package jssc;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import static org.mockito.Mockito.*;

/**
 * Tests if Java logic around native invocations does not prevent
 * critical calls from happening when opening and closing ports
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "jssc.*")
public class NativeMethodInvocationTest extends TestCase {

    @Mock(name = "serialInterface")
    private SerialNativeInterface serialInterface;

    private final long mockHandle = 0xDeadDeefL;

    @Test
    public void nativeMethodIsNotCalledWhenAttemptingToOpenAlreadyOpenPort() {
        // given
        SerialPort serialPort = newSerialPort();

        // when
        try{
            serialPort.openPort();
            serialPort.openPort();
            Assert.fail("Expected to throw a SerialPortException");
        } catch (SerialPortException expected) {
            // TODO: Is it really expected or should this method return false as javadoc states?
        }

        // then
        verify(serialInterface, times(1)).openPort(anyString(), anyBoolean());
    }

    @Test
    public void nativeMethodIsNotCalledWhenAttemptingToClosePortThatIsNotYetOpen() {
        // given
        SerialPort serialPort = newSerialPort();

        // when
        try{
            serialPort.closePort();
            Assert.fail("Expected to throw a SerialPortException");
        } catch (SerialPortException expected) {
            // TODO: Is it really expected or should this method return false as javadoc states?
        }

        // then
        verify(serialInterface, never()).setEventsMask(anyLong(), anyInt());
        verify(serialInterface, never()).closePort(anyLong());
    }

    @Test
    public void nativeMethodIsNotCalledWhenAttemptingCloseAlreadyClosedPort() {
        // given
        SerialPort serialPort = newSerialPort();

        // when
        try{
            serialPort.openPort();
            serialPort.closePort();
            serialPort.closePort();
            Assert.fail("Expected to throw a SerialPortException");
        } catch (SerialPortException expected) {
            // TODO: Is it really expected or should this method return false as javadoc states?
        }

        // then
        verify(serialInterface, times(1)).openPort(anyString(), anyBoolean());
        verify(serialInterface, times(1)).closePort(anyLong());
    }

    @Test
    public void nativeMethodIsCalledWhenClosingOpenAndReopeningClosedPort() throws SerialPortException {
        // given
        SerialPort serialPort = newSerialPort();

        // when
        serialPort.openPort();
        serialPort.closePort();
        serialPort.openPort();
        serialPort.closePort();

        // then
        verify(serialInterface, times(2)).openPort(anyString(), anyBoolean());
        verify(serialInterface, times(2)).closePort(mockHandle);
    }

    @Test
    public void nativeMethodIsCalledWhenClosingOpenPortWithEventListenerOnPosix() throws SerialPortException {
        // given
        setOs(SerialNativeInterface.OS_LINUX);
        SerialPort serialPort = newSerialPort();
        when(serialInterface.setEventsMask(anyLong(), anyInt())).thenReturn(true);

        // when
        serialPort.openPort();
        serialPort.addEventListener(someListener(), SerialPort.MASK_RXCHAR);
        serialPort.closePort();

        // then
        verify(serialInterface, times(1)).openPort(anyString(), anyBoolean());
        verify(serialInterface, never()).setEventsMask(anyLong(), anyInt());
        verify(serialInterface, times(1)).closePort(mockHandle);
    }

    @Test
    public void nativeMethodIsCalledWhenClosingOpenPortWithEventListenerOnWindows() throws SerialPortException {
        // given
        setOs(SerialNativeInterface.OS_WINDOWS);
        SerialPort serialPort = newSerialPort();
        when(serialInterface.setEventsMask(anyLong(), anyInt())).thenReturn(true);

        // when
        serialPort.openPort();
        serialPort.addEventListener(someListener(), SerialPort.MASK_RXCHAR);
        serialPort.closePort();

        // then
        verify(serialInterface, times(1)).openPort(anyString(), anyBoolean());
        verify(serialInterface, times(1)).setEventsMask(mockHandle, SerialPort.MASK_RXCHAR);
        verify(serialInterface, times(1)).setEventsMask(mockHandle, 0);
        verify(serialInterface, times(1)).closePort(mockHandle);
    }

    /**
     * Simulates conditions as described in issue #107: user attempts to close the serial port that has been removed
     * from the system (e.g. unplugged usb serial adapter) without notifying the java code about it.
     */
    @Test
    public void nativeMethodIsCalledWhenClosingOpenPortWithEventListenerIfSetMaskFails() {
        // given
        setOs(SerialNativeInterface.OS_WINDOWS);
        SerialPort serialPort = newSerialPort();
        when(serialInterface.setEventsMask(anyLong(), anyInt())).thenReturn(true);

        // when
        try {
            serialPort.openPort();
            serialPort.addEventListener(someListener());
            when(serialInterface.setEventsMask(anyLong(), anyInt())).thenReturn(false);
            serialPort.closePort();
        } catch (SerialPortException expected) {
            // TODO: Is it really expected or should this method return false as javadoc states?
        }

        // then
        verify(serialInterface, times(1)).openPort(anyString(), anyBoolean());
        verify(serialInterface, times(1)).setEventsMask(mockHandle, SerialPort.MASK_RXCHAR);
        verify(serialInterface, times(1)).setEventsMask(mockHandle, 0);
        verify(serialInterface, times(1)).closePort(mockHandle);
    }

    private void setOs(int os) {
        PowerMockito.mockStatic(SerialNativeInterface.class);
        when(SerialNativeInterface.getOsType()).thenReturn(os);
    }

    private SerialPort newSerialPort() {
        String portName = "dummy";
        SerialPort serialPort = new SerialPort(portName);
        Whitebox.setInternalState(serialPort, "serialInterface", serialInterface);
        when(serialInterface.openPort(anyString(), anyBoolean())).thenReturn(mockHandle);
        when(serialInterface.closePort(anyLong())).thenReturn(true);
        when(serialInterface.waitEvents(anyLong())).thenReturn(new int[][]{});
        return serialPort;
    }

    private SerialPortEventListener someListener() {
        return new SerialPortEventListener() {
            @Override
            public void serialEvent(SerialPortEvent serialPortEvent) {

            }
        };
    }
}
