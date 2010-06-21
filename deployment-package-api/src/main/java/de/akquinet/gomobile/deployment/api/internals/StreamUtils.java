package de.akquinet.gomobile.deployment.api.internals;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;

public final class StreamUtils {

    /**
     * Buffer size.
     */
    private static final int BUFFER_SIZE = 102400;

    /**
     * Private constructor to ensure no instances are created.
     */
    private StreamUtils() {
    }

    /**
     * Copy a stream.
     * @param src the source input stream
     * @param dest the destination output stream
     * @param closeStreams TRUE if the streams should be closed on completion
     * @throws IOException if an IO error occurs
     */
    public static void copyStream(InputStream src, OutputStream dest,
            boolean closeStreams) throws IOException {
        copyStream(null, 0, src, dest, closeStreams);
    }

    /**
     * Copy a stream.
     * @param sourceURL the source url
     * @param expected the expected size in bytes
     * @param source the source input stream
     * @param destination the destination output stream
     * @param closeStreams TRUE if the streams should be closed on completion
     * @throws IOException if an IO error occurs
     */
    public static void copyStream(URL sourceURL, int expected,
            InputStream source, OutputStream destination, boolean closeStreams)
            throws IOException {
        int length;
        int count = 0; // cumulative total read
        byte[] buffer = new byte[BUFFER_SIZE];
        BufferedOutputStream dest;
        if (destination instanceof BufferedOutputStream) {
            dest = (BufferedOutputStream) destination;
        } else {
            dest = new BufferedOutputStream(destination);
        }
        BufferedInputStream src;
        if (source instanceof BufferedInputStream) {
            src = (BufferedInputStream) source;
        } else {
            src = new BufferedInputStream(source);
        }
        try {
            while ((length = src.read(buffer)) >= 0) {
                count = count + length;
                dest.write(buffer, 0, length);
            }
            dest.flush();
        } finally {
            if (closeStreams) {
                closeStreams(src, dest);
            }
        }
    }

    /**
     * Closes the streams and reports Exceptions to System.err
     * @param src The InputStream to close.
     * @param dest The OutputStream to close.
     */
    private static void closeStreams(InputStream src, OutputStream dest) {
        try {
            src.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            dest.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Compares if two streams are identical in their contents.
     * @param in1 The first stream.
     * @param in2 The second stream.
     * @return true if both streams contains the same byte data, including having
     *         the same number of characters in them.
     * @throws IOException If an underlying I/O problem occured.
     */
    public static boolean compareStreams(InputStream in1, InputStream in2)
            throws IOException {
        boolean moreOnIn1;
        do {
            int v1 = in1.read();
            int v2 = in2.read();
            if (v1 != v2) {
                return false;
            }
            moreOnIn1 = v1 != -1;
        } while (moreOnIn1);
        boolean noMoreOnIn2Either = in2.read() == -1;
        return noMoreOnIn2Either;
    }

    /**
     * Copies the Reader to the Writer.
     * @param input The input characters.
     * @param output The destination of the characters.
     * @param close true if the reader and writer should be closed after the
     *        operation is completed.
     * @throws IOException If an underlying I/O problem occured.
     */
    public static void copyReaderToWriter(Reader input, Writer output,
            boolean close) throws IOException {
        try {
            BufferedReader in = bufferInput(input);
            BufferedWriter out = bufferOutput(output);
            int ch = in.read();
            while (ch != -1) {
                out.write(ch);
                ch = in.read();
            }
            out.flush();
            out.close();
        } finally {
            if (close) {
                input.close();
                output.close();
            }
        }
    }

    /**
     * Wraps the Writer in a BufferedWriter, unless it already is a
     * BufferedWriter.
     * @param writer The Writer to check and possibly wrap.
     * @return A BufferedWriter.
     */
    private static BufferedWriter bufferOutput(Writer writer) {
        BufferedWriter out;
        if (writer instanceof BufferedWriter) {
            out = (BufferedWriter) writer;
        } else {
            out = new BufferedWriter(writer);
        }
        return out;
    }

    /**
     * Wraps the Reader in a BufferedReaderm unless it already is a
     * BufferedReader.
     * @param reader The Reader to check and possibly wrap.
     * @return A BufferedReader.
     */
    private static BufferedReader bufferInput(Reader reader) {
        BufferedReader in;
        if (reader instanceof BufferedReader) {
            in = (BufferedReader) reader;
        } else {
            in = new BufferedReader(reader);
        }
        return in;
    }

    /**
     * Copies an InputStream to a Writer.
     * @param in The input byte stream of data.
     * @param out The Writer to receive the streamed data as characters.
     * @param encoding The encoding used in the byte stream.
     * @param close true if the Reader and OutputStream should be closed after
     *        the completion.
     * @throws IOException If an underlying I/O Exception occurs.
     */
    public static void copyStreamToWriter(InputStream in, Writer out,
            String encoding, boolean close) throws IOException {
        InputStreamReader reader = new InputStreamReader(in, encoding);
        copyReaderToWriter(reader, out, close);
    }

    /**
     * Copies the content of the Reader to the provided OutputStream using the
     * provided encoding.
     * @param in The character data to convert.
     * @param out The OutputStream to send the data to.
     * @param encoding The character encoding that should be used for the byte
     *        stream.
     * @param close true if the Reader and OutputStream should be closed after
     *        the completion.
     * @throws IOException If an underlying I/O Exception occurs.
     */
    public static void copyReaderToStream(Reader in, OutputStream out,
            String encoding, boolean close) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(out, encoding);
        copyReaderToWriter(in, writer, close);
    }
}
