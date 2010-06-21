package de.akquinet.gomobile.deployment.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import de.akquinet.gomobile.deployment.api.internals.Handle;
import de.akquinet.gomobile.deployment.api.internals.Store;


public class Resource {

    private String name;

    private String processor;

    private Handle handle;

    public Resource setPath(String value) {
        name = value;
        return this;
    }

    public Resource setProcessor(String value) {
        processor = value;
        return this;
    }


    public String getName() {
        return name;
    }


    public String getProcessor() {
        return processor;
    }

    public Resource setURL(URL value) throws IOException {
        handle = Store.STORE.store(value.openStream());
        analyze(value);
        return this;
    }

    private void analyze(URL value) throws IOException {
        String url = value.toExternalForm();
        int index = url.lastIndexOf('/'); //TODO OS Specific
        if (name == null  && index != -1) {
            name = url.substring(index +1);
        }
    }

    public InputStream getInputStream() throws IOException {
        return Store.STORE.load(handle);
    }

    public boolean exists() throws IOException {
        return (handle != null) &&
            ((new File(Store.STORE.getLocation(handle))).exists());
    }

    public Object getSHA1() {
        return handle.getSHA1();
    }




}
