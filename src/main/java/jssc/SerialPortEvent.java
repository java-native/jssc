/* jSSC (Java Simple Serial Connector) - serial port communication library.
 * © Alexey Sokolov (scream3r), 2010-2014.
 *
 * This file is part of jSSC.
 *
 * jSSC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jSSC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with jSSC.  If not, see <http://www.gnu.org/licenses/>.
 *
 * If you use jSSC in public project you can inform me about this by e-mail,
 * of course if you want it.
 *
 * e-mail: scream3r.org@gmail.com
 * web-site: http://scream3r.org | http://code.google.com/p/java-simple-serial-connector/
 */
package jssc;

/**
 *
 * @author scream3r
 */
public class SerialPortEvent {


    private SerialPort port;
    private int eventType;
    private int eventValue;

    @Deprecated
    private String portName;

    public static final int RXCHAR = 1;
    public static final int RXFLAG = 2;
    public static final int TXEMPTY = 4;
    public static final int CTS = 8;
    public static final int DSR = 16;
    public static final int RLSD = 32;
    public static final int BREAK = 64;
    public static final int ERR = 128;
    public static final int RING = 256;

    public SerialPortEvent(SerialPort port, int eventType, int eventValue){
        this.port = port;
        this.eventType = eventType;
        this.eventValue = eventValue;
    }

    @Deprecated
    public SerialPortEvent(String portName, int eventType, int eventValue){
        this.portName = portName;
        this.eventType = eventType;
        this.eventValue = eventValue;
    }

    /**
     * Getting the port that set off this event
     */
    public SerialPort getPort(){
        return port;
    }

    /**
     * Getting port name which sent the event
     */
    @Deprecated
    public String getPortName() {
        return port.getPortName();
    }

    /**
     * Getting event type
     */
    public int getEventType() {
        return eventType;
    }

    /**
     * Getting event value
     * <br>
     * <br><u><b>Event values depending on their types:</b></u>
     * <br><b>RXCHAR</b> - bytes count in input buffer
     * <br><b>RXFLAG</b> - bytes count in input buffer (Not supported in Linux)
     * <br><b>TXEMPTY</b> - bytes count in output buffer
     * <br><b>CTS</b> - state of CTS line (0 - OFF, 1 - ON)
     * <br><b>DSR</b> - state of DSR line (0 - OFF, 1 - ON)
     * <br><b>RLSD</b> - state of RLSD line (0 - OFF, 1 - ON)
     * <br><b>BREAK</b> - 0
     * <br><b>RING</b> - state of RING line (0 - OFF, 1 - ON)
     * <br><b>ERR</b> - mask of errors
     */
    public int getEventValue() {
        return eventValue;
    }

    /**
     * Method returns true if event of type <b>"RXCHAR"</b> is received and otherwise false
     */
    public boolean isRXCHAR() {
        return eventType == RXCHAR;
    }

    /**
     * Method returns true if event of type <b>"RXFLAG"</b> is received and otherwise false
     */
    public boolean isRXFLAG() {
        return eventType == RXFLAG;
    }

    /**
     * Method returns true if event of type <b>"TXEMPTY"</b> is received and otherwise false
     */
    public boolean isTXEMPTY() {
        return eventType == TXEMPTY;
    }

    /**
     * Method returns true if event of type <b>"CTS"</b> is received and otherwise false
     */
    public boolean isCTS() {
        return eventType == CTS;
    }

    /**
     * Method returns true if event of type <b>"DSR"</b> is received and otherwise false
     */
    public boolean isDSR() {
        return eventType == DSR;
    }

    /**
     * Method returns true if event of type <b>"RLSD"</b> is received and otherwise false
     */
    public boolean isRLSD() {
        return eventType == RLSD;
    }

    /**
     * Method returns true if event of type <b>"BREAK"</b> is received and otherwise false
     */
    public boolean isBREAK() {
        return eventType == BREAK;
    }

    /**
     * Method returns true if event of type <b>"ERR"</b> is received and otherwise false
     */
    public boolean isERR() {
        return eventType == ERR;
    }

    /**
     * Method returns true if event of type <b>"RING"</b> is received and otherwise false
     */
    public boolean isRING() {
        return eventType == RING;
    }
}
