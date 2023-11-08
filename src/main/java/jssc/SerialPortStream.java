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
 *
 * SerialPortStream.java is written and contributed under the same license
 * to the jSSC project by FactorIT B.V. in 2022 (factoritbv on github)
 *
 */
package jssc;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;

public class SerialPortStream {
    
    private SerialPort m_serial_port;
    private SerialPortInputStream m_serial_port_input_stream;
    private SerialPortOutputStream m_serial_port_output_stream;
    private volatile boolean m_closed;

    private class SerialPortInputStream extends InputStream {
        private byte[] m_buffer;
        private int m_buffer_pos = 0;
        private int m_buffer_len = 0;

        public SerialPortInputStream() {
            m_buffer_len = 0;
            m_buffer_pos = 0;
        }

        public void close() throws IOException {
            SerialPortInputStream.this.close();
        }

        public int read() throws IOException {
            try {
                // See if we run out of available bytes, and try to re-fill the buffer
                if (m_buffer_pos >= m_buffer_len) {
                    m_buffer_len = m_serial_port.getInputBufferBytesCount();
                    if (m_buffer_len == 0) {
                        // Nothing available, just block until the first byte comes available and return directly
                        return (int)m_serial_port.readBytes(1)[0];
                    }
                    // Fetch the available bytes at once
                    m_buffer = m_serial_port.readBytes(m_buffer_len);
                    // Reset the position in the buffer
                    m_buffer_pos = 0;
                }
                return m_buffer[m_buffer_pos++];
            } catch (SerialPortException ex) {
                throw new IOException(ex);
            }
        }

        public int available() throws IOException {
            try {
                return m_serial_port.getInputBufferBytesCount();
            } catch (SerialPortException ex) {
                throw new IOException(ex);
            }
        }
    }

    private class SerialPortOutputStream extends OutputStream {
        
        public void close() throws IOException {
            SerialPortStream.this.close();
        }
        
        public void write(byte[] b) throws IOException {
            try {
                m_serial_port.writeBytes(b);
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        }
    
        public void write(byte[] b, int off, int len) throws IOException {
            byte[] buffer = Arrays.copyOfRange(b,off,off+len);
            try {
                m_serial_port.writeBytes(buffer);
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        }

        public void write(int b) throws IOException {
            try {
                m_serial_port.writeBytes(new byte[]{(byte)b});
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        }
    }
    
    public SerialPortStream(SerialPort serial_port, int baudrate) throws IOException {
        super();
        m_serial_port = serial_port;
        try {
            // Open the serial port if it is still closed
            if (!m_serial_port.isOpened()) {
                if (!m_serial_port.openPort()) {
                    throw new IOException("Could not open serial port [" + m_serial_port.getPortName() + "]");
                }
                // For the USB CDC class seems to be mandatory to set the line coding.
                // So we use the most common values (9600 baud, 8 bits chars, 1 close bit, no parity)
                if (!m_serial_port.setParams(baudrate,8,1,0)) {
                    throw new IOException("Could not set params for serial port [" + m_serial_port.getPortName() + "]");
                }
            }
        } catch (SerialPortException ex) {
            // Redirect exeption to an IO exception
            throw new IOException(ex);
        }
        m_serial_port_input_stream = new SerialPortInputStream();
        m_serial_port_output_stream = new SerialPortOutputStream();
    }

    public SerialPortStream(SerialPort serial_port) throws IOException {
        this(serial_port,9600);
    }

    public SerialPortStream(String serial_port_name, int baudrate) throws IOException {
        this(new SerialPort(serial_port_name),baudrate);
    }

    public SerialPortStream(String serial_port_name) throws IOException {
        this(serial_port_name,9600);
    }

    public SerialPort getSerialPort() {
        return m_serial_port;
    }

    public String getName() {
        return m_serial_port.getPortName();
    }

    public InputStream getInputStream() {
        return m_serial_port_input_stream;
    }
    
    public OutputStream getOutputStream() {
        return m_serial_port_output_stream;
    }

    public synchronized void close() throws IOException {
        // Immidiatly return when connection was already lost and cleaned up in the past
        if (m_closed) return;

        // Update the connection status
        m_closed = true;

        boolean failure = false;

        // Try to close and clean up the input stream
        try {
            m_serial_port_input_stream.close();
        } catch (IOException ex) {
            failure = true;
        }

        // Try to close and clean up the output stream
        try {
            m_serial_port_output_stream.close();
        } catch (IOException ex) {
            failure = true;
        }

        if (failure) {
            throw new IOException("Error occured while closing and cleaning up the in/output streams");
        }

        // Only try to close the serial port if it is still open
        if (m_serial_port.isOpened()) {
            try {
                m_serial_port.closePort();
            } catch (SerialPortException ex) {
                throw new IOException(ex);
            }
        }
    }

    public synchronized boolean isClosed() {
        if (!m_closed) {
            try {
                m_serial_port_input_stream.available();
            } catch (IOException ex) {
                try {
                    this.close();
                } catch (IOException ex2) {
                    // ignore
                }
            }
        }
        return m_closed;
    }
}

