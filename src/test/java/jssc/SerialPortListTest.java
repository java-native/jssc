package jssc;

import jssc.junit.rules.DisplayMethodNameRule;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;


public class SerialPortListTest extends DisplayMethodNameRule {

    private static final String BASIC_LINUX_PORTNAME_REGEXP = "(ttyS|ttyUSB|ttyACM|ttyAMA|rfcomm|ttyO|ttyM|ttyMXUSB|ttyMUE%s)[0-9]{1,3}";
    private static final String BASIC_MAC_OS_PORTNAME_REGEXP = "(tty|cu%s)\\..*";

    private void prepareEnv() {
        System.clearProperty(SerialNativeInterface.PROPERTY_JSSC_ALLOW_EXOTIC_NAMES);
        System.clearProperty(SerialNativeInterface.PROPERTY_JSSC_EXOTIC_NAMES);
    }

    @Test
    public void checkSerialPortNameRegExpOnLinux() {
        MockedStatic<SerialNativeInterface> serialNativeInterface = null;
        prepareEnv();

        try {
            serialNativeInterface = Mockito.mockStatic(SerialNativeInterface.class);
            serialNativeInterface.when(SerialNativeInterface::getOsType).thenReturn(SerialNativeInterface.OS_LINUX);
            SerialPortList.initSearchParameters();
            Assert.assertEquals(String.format(BASIC_LINUX_PORTNAME_REGEXP, ""), SerialPortList.getPortnamesRegexp().pattern());

            System.setProperty(SerialNativeInterface.PROPERTY_JSSC_EXOTIC_NAMES, "abc");
            SerialPortList.initSearchParameters();
            Assert.assertEquals(String.format(BASIC_LINUX_PORTNAME_REGEXP, ""), SerialPortList.getPortnamesRegexp().pattern());

            System.setProperty(SerialNativeInterface.PROPERTY_JSSC_ALLOW_EXOTIC_NAMES, "true");
            SerialPortList.initSearchParameters();
            Assert.assertEquals(String.format(BASIC_LINUX_PORTNAME_REGEXP, "|abc"), SerialPortList.getPortnamesRegexp().pattern());

            System.setProperty(SerialNativeInterface.PROPERTY_JSSC_ALLOW_EXOTIC_NAMES, "false");
            SerialPortList.initSearchParameters();
            Assert.assertEquals(String.format(BASIC_LINUX_PORTNAME_REGEXP, ""), SerialPortList.getPortnamesRegexp().pattern());

            System.setProperty(SerialNativeInterface.PROPERTY_JSSC_ALLOW_EXOTIC_NAMES, "true");
            System.setProperty(SerialNativeInterface.PROPERTY_JSSC_EXOTIC_NAMES, "abc1d|bvc");
            SerialPortList.initSearchParameters();
            Assert.assertEquals(String.format(BASIC_LINUX_PORTNAME_REGEXP, "|abc|d|bvc"), SerialPortList.getPortnamesRegexp().pattern());

            System.clearProperty(SerialNativeInterface.PROPERTY_JSSC_EXOTIC_NAMES);
            SerialPortList.initSearchParameters();
            Assert.assertEquals(String.format(BASIC_LINUX_PORTNAME_REGEXP, ""), SerialPortList.getPortnamesRegexp().pattern());
        } finally {
            if(serialNativeInterface != null) {
                serialNativeInterface.close();
            }
        }
    }

    @Test
    public void checkSerialPortNameRegExpOnMacOS() {
        MockedStatic<SerialNativeInterface> serialNativeInterface = null;
        prepareEnv();

        try {
            serialNativeInterface = Mockito.mockStatic(SerialNativeInterface.class);
            serialNativeInterface.when(SerialNativeInterface::getOsType).thenReturn(SerialNativeInterface.OS_MAC_OS_X);
            SerialPortList.initSearchParameters();
            Assert.assertEquals(String.format(BASIC_MAC_OS_PORTNAME_REGEXP, ""), SerialPortList.getPortnamesRegexp().pattern());

            System.setProperty(SerialNativeInterface.PROPERTY_JSSC_EXOTIC_NAMES, "abc");
            SerialPortList.initSearchParameters();
            Assert.assertEquals(String.format(BASIC_MAC_OS_PORTNAME_REGEXP, ""), SerialPortList.getPortnamesRegexp().pattern());

            System.setProperty(SerialNativeInterface.PROPERTY_JSSC_ALLOW_EXOTIC_NAMES, "true");
            SerialPortList.initSearchParameters();
            Assert.assertEquals(String.format(BASIC_MAC_OS_PORTNAME_REGEXP, "|abc"), SerialPortList.getPortnamesRegexp().pattern());

            System.setProperty(SerialNativeInterface.PROPERTY_JSSC_ALLOW_EXOTIC_NAMES, "false");
            SerialPortList.initSearchParameters();
            Assert.assertEquals(String.format(BASIC_MAC_OS_PORTNAME_REGEXP, ""), SerialPortList.getPortnamesRegexp().pattern());

            System.setProperty(SerialNativeInterface.PROPERTY_JSSC_ALLOW_EXOTIC_NAMES, "true");
            System.setProperty(SerialNativeInterface.PROPERTY_JSSC_EXOTIC_NAMES, "abc1d|bvc");
            SerialPortList.initSearchParameters();
            Assert.assertEquals(String.format(BASIC_MAC_OS_PORTNAME_REGEXP, "|abc|d|bvc"), SerialPortList.getPortnamesRegexp().pattern());

            System.clearProperty(SerialNativeInterface.PROPERTY_JSSC_EXOTIC_NAMES);
            SerialPortList.initSearchParameters();
            Assert.assertEquals(String.format(BASIC_MAC_OS_PORTNAME_REGEXP, ""), SerialPortList.getPortnamesRegexp().pattern());
        } finally {
            if(serialNativeInterface != null) {
                serialNativeInterface.close();
            }
        }
    }
}
