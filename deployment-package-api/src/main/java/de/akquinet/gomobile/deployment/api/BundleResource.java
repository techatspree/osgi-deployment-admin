package de.akquinet.gomobile.deployment.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import de.akquinet.gomobile.deployment.api.internals.Handle;
import de.akquinet.gomobile.deployment.api.internals.Store;

public class BundleResource {

    private String name;

    private String symbolicName;

    private String version;

    private boolean isMissing;

    private boolean isCustomizer;

    private Handle handle;

    public BundleResource setPath(String value) {
        name = value;
        return this;
    }

    public BundleResource setURL(URL value) throws IOException {
        handle = Store.STORE.store(value.openStream());
        analyze();
        return this;
    }

    public BundleResource setSymbolicName(String value) {
        symbolicName = value;
        return this;
    }

    public BundleResource setVersion(String value) {
        version = value;
        return this;
    }

    public BundleResource setMissing(boolean value) {
       isMissing = value;
       return this;
    }

    public BundleResource setCustomizer(boolean value) {
        isCustomizer = value;
        return this;
     }

    public String getSymbolicName() {
        return symbolicName;
    }


    public String getName() {
        return name;
    }


    public String getVersion() {
        return version;
    }

    public boolean isMissing() {
        return isMissing;
    }

    public boolean isCustomizer() {
        return isCustomizer;
    }

    private void analyze() throws IOException {
        if (handle == null) {
            return;
        }

        JarFile jar = new JarFile(new File(Store.STORE.getLocation(handle)));
        Manifest man = jar.getManifest();
        //TODO Does this really fit the spec ?

        // Remove the :=singleton=true part is present.
        String symb = man.getMainAttributes().getValue(
                Constants.BUNDLE_SYMBOLICNAME);
        int index = symb.indexOf(";singleton:=");
        if (index != -1) {
            setSymbolicName(symb.substring(0, index));
        } else {
            setSymbolicName(symb);
        }

        setVersion(man.getMainAttributes().getValue(Constants.BUNDLE_VERSION));

        if (name == null  && symbolicName != null) {
            name = symbolicName +  ".jar";
        }

    }

    public InputStream getInputStream() throws IOException {
        return Store.STORE.load(handle);
    }

    public boolean exists() throws IOException {
        if (handle == null) {
            return false;
        } else {
            return new File(Store.STORE.getLocation(handle)).exists();
        }
    }

    public Object getSHA1() {
       return handle.getSHA1();
    }


}
