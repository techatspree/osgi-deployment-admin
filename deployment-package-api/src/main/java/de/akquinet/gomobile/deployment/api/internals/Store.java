package de.akquinet.gomobile.deployment.api.internals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Store {

    public static final Store STORE = new Store();

    private File dir;

    private Store() {
        dir = new File((new StringBuilder()).append(
                System.getProperty("java.io.tmpdir")).append("/dp").toString());
        dir.mkdirs();

    }

    public Handle store(InputStream is) throws IOException {

        final File intermediate = File.createTempFile("dp_", ".tmp");

        FileOutputStream fis = null;
        final String h;

        fis = new FileOutputStream(intermediate);
        h = hash(is, fis);

        fis.close();
        //if (!getLocation(h).exists()) {
            StreamUtils.copyStream(new FileInputStream(intermediate),
                    new FileOutputStream(getLocation(h)), true);
        //}

        intermediate.delete();

        Handle handle = new Handle() {

            public String getSHA1() {
                return h;
            }
        };


        return handle;
    }

    public InputStream load(Handle handle) throws IOException {
        return new FileInputStream(getLocation(handle.getSHA1()));
    }

    public URI getLocation(Handle handle) throws IOException {
        return getLocation(handle.getSHA1()).toURI();
    }

    private File getLocation(String id) {
        return new File(dir, "dp_" + id + ".bin");
    }

    public String hash(final InputStream is, OutputStream storeHere)
            throws IOException {

        byte[] sha1hash;

        try {
            MessageDigest md;
            md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = new byte[1024];
            int numRead = 0;
            while ((numRead = is.read(bytes)) >= 0)

            {
                md.update(bytes, 0, numRead);
                storeHere.write(bytes, 0, numRead);
            }
            sha1hash = md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return convertToHex(sha1hash);
    }

    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

}
