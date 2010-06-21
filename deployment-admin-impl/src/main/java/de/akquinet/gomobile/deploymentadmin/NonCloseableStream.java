package de.akquinet.gomobile.deploymentadmin;

import java.io.IOException;
import java.io.InputStream;

/**
 * Stream that does nothing when close() is invoked, calls to one of the read methods will throw an <code>IOException</code>
 * after close() is called. Also, mark/reset is not supported. Deployment Admin can use this class to pass on as an <code>InputStream</code>
 * to a resource processor.
 *
 */
public class NonCloseableStream extends InputStream {

    private final InputStream m_input;
    private boolean m_closed;

    public NonCloseableStream(InputStream m_input) {
        this.m_input = m_input;
    }


    // stream should not be actually closed, subsequent calls to read methods will throw an exception though

    public void close() throws IOException {
        if (m_closed) {
            throw new IOException("Unable to read, stream is closed.");
        }
        m_closed = true;
    }

    public int read() throws IOException {
        return m_input.read();
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (m_closed) {
            throw new IOException("Unable to read, stream is closed.");
        }
        return m_input.read(b, off, len);
    }

    public int read(byte[] b) throws IOException {
        if (m_closed) {
            throw new IOException("Unable to read, stream is closed.");
        }
        return m_input.read(b);
    }


    // No mark & reset support

    public boolean markSupported() {
        return false;
    }

    public void mark(int readlimit) {
        // do nothing
    }

    public void reset() throws IOException {
        throw new IOException("Mark and reset are not available on this type of stream.");
    }


    // Unaffected methods

    public int available() throws IOException {
        return m_input.available();
    }

    public int hashCode() {
        return m_input.hashCode();
    }

    public long skip(long n) throws IOException {
        return m_input.skip(n);
    }

    public boolean equals(Object obj) {
        return m_input.equals(obj);
    }

    public String toString() {
        return m_input.toString();
    }

}
