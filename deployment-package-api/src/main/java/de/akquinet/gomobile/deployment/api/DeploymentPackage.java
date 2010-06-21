package de.akquinet.gomobile.deployment.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import de.akquinet.gomobile.deployment.api.internals.OrderedManifest;
import de.akquinet.gomobile.deployment.api.internals.StreamUtils;


public class DeploymentPackage {

    private String symbolicName;
    private String version;
    private String fixPack;
    private long requiredStorage;
    private String license;
    private String vendor;
    private URL icon;
    private URL docURL;
    private String description;
    private String address;
    private String name;
    private String copyright;
    private List<BundleResource> bundles = new ArrayList<BundleResource>();
    private List<Resource> resources = new ArrayList<Resource>();
    private Map<String,String> entries = new HashMap<String,String>();

    public DeploymentPackage setSymbolicName(String value) {
        symbolicName = value;
        entries.put(Constants.DEPLOYMENTPACKAGE_SYMBOLICMAME, value);
        return this;
    }

    public DeploymentPackage setVersion(String value) {
        version = value;
        entries.put(Constants.DEPLOYMENTPACKAGE_VERSION, value);
        return this;
    }

    public DeploymentPackage setFixPackage(String fix) {
        fixPack = fix;
        entries.put(Constants.DEPLOYMENTPACKAGE_FIXPACK, fix);
        return this;
    }

    public DeploymentPackage setName(String value) {
        name = value;
        entries.put(Constants.DEPLOYMENTPACKAGE_NAME, value);
        return this;
    }

    public DeploymentPackage setCopyright(String value) {
        copyright = value;
        entries.put(Constants.DEPLOYMENTPACKAGE_COPYRIGHT, value);
        return this;
    }

    public DeploymentPackage setContactAddress(String value) {
        address = value;
        entries.put(Constants.DEPLOYMENTPACKAGE_CONTACTADDRESS, value);
        return this;
    }

    public DeploymentPackage setDescription(String value) {
        description = value;
        entries.put(Constants.DEPLOYMENTPACKAGE_DESCRIPTION, value);
        return this;
    }

    public DeploymentPackage setDocURL(URL value) {
        docURL = value;
        entries.put(Constants.DEPLOYMENTPACKAGE_DOCURL, value.toExternalForm());
        return this;
    }

    public DeploymentPackage setIcon(URL value) {
        icon = value;
        entries.put(Constants.DEPLOYMENTPACKAGE_ICON, value.toExternalForm());
        return this;
    }

    public DeploymentPackage setVendor(String value) {
        vendor = value;
        entries.put(Constants.DEPLOYMENTPACKAGE_VENDOR, value);
        return this;
    }

    public DeploymentPackage setLicense(String value) {
        license = value;
        entries.put(Constants.DEPLOYMENTPACKAGE_LICENSE, value);
        return this;
    }



    public DeploymentPackage addManifestEntry(String n, String v) {
        entries.put(n, v);
        return this;
    }

    public DeploymentPackage setRequiredStorage(long value) {
        requiredStorage = value;
        entries.put(Constants.DEPLOYMENTPACKAGE_REQUIREDSTORAGE, "" +value);
        return this;
    }

    public DeploymentPackage addBundle(BundleResource bundle) {
        bundles.add(bundle);
        return this;
    }

    public DeploymentPackage addBundle(URL url) throws IOException {
        BundleResource bundle = new BundleResource()
            .setURL(url);
        bundles.add(bundle);
        return this;
    }

    public DeploymentPackage addBundle(URL url, String name) throws IOException {
        BundleResource bundle = new BundleResource()
            .setURL(url).setPath(name);
        bundles.add(bundle);
        return this;
    }

    public DeploymentPackage removeBundle(BundleResource bundle) {
        bundles.remove(bundle);
        return this;
    }

    public DeploymentPackage removeBundle(String sn) {
        for (BundleResource br : bundles) {
            if (br.getSymbolicName().equals(sn)) {
                bundles.remove(br);
                return this;
            }
        }
        return this;
    }

    public DeploymentPackage removeBundleByName(String name) {
        for (BundleResource br : bundles) {
            if (br.getName().equals(name)) {
                bundles.remove(br);
                return this;
            }
        }
        return this;
    }

    public DeploymentPackage addResource(Resource res) {
        resources.add(res);
        return this;
    }

    public DeploymentPackage addResource(URL url, String resourceProcessor) throws IOException {
        resources.add(new Resource().setURL(url).setProcessor(resourceProcessor));
        return this;
    }

    public DeploymentPackage addResource(URL url, String resourceProcessor, String name) throws IOException {
        resources.add(new Resource().setURL(url).setProcessor(resourceProcessor).setPath(name));
        return this;
    }

    public DeploymentPackage removeResource(Resource res) {
        resources.remove(res);
        return this;
    }

    public DeploymentPackage removeResource(String n) {
        for (Resource res : resources) {
            if (res.getName().equals(n)) {
                resources.remove(res);
                return this;
            }
        }
        return this;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public String getVersion() {
        return version;
    }

    public String getFixPack() {
        return fixPack;
    }

    public long getRequiredStorage() {
        return requiredStorage;
    }

    public String getLicense() {
        return license;
    }

    public String getVendor() {
        return vendor;
    }

    public URL getIcon() {
        return icon;
    }

    public URL getDocURL() {
        return docURL;
    }

    public String getDescription() {
        return description;
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public String getCopyright() {
        return copyright;
    }

    public List<BundleResource> getBundles() {
        return bundles;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public Map<String, String> getEntries() {
        return entries;
    }

    public void check() throws CheckingException {
        // Symbolic Name set (114.3.4.1)
        if (symbolicName == null) {
            throw new CheckingException(Constants.DEPLOYMENTPACKAGE_SYMBOLICMAME + " missing");
        }

        // Symbolic Name is an unique-name (114.3.4.1)
        if (! Checker.checkUniqueName(symbolicName)) {
            throw new CheckingException(symbolicName + " is not an unique name");
        }

        // Version set (114.3.4.2)
        if (version == null) {
            throw new CheckingException(Constants.DEPLOYMENTPACKAGE_VERSION + " missing");
        }

        //Version is a Version (114.3.4.2)
        if (! Checker.checkVersion(version)) {
            throw new CheckingException(version + " is not an unique name");
        }

        // Fix-Pack is a version-range or absent ( (114.3.4.3)
        if (fixPack != null  && Checker.checkVersionRange(fixPack)) {
            throw new CheckingException(fixPack + " is not a valid version range");
        }

        for (BundleResource br : bundles) {
            // Must exist ... Check the Handle
            try {
                if (! br.exists()) {
                    throw new CheckingException("The bundle file does not exist : " + br.getSymbolicName());
                }
            } catch (IOException e) {
                throw new CheckingException("The bundle cannot be loaded correctly : " + br.getSymbolicName());
            }

            if (br.getSymbolicName() == null) { // 114.3.4.7
                throw new CheckingException(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE + " is missing for a bundle resource");
            }

            if (! Checker.checkBundleSymbolicName(br.getSymbolicName())) { // 114.3.4.7
                throw new CheckingException(br.getSymbolicName() + " is not a valid symbolic name");
            }

            if (br.getVersion() == null) { // 114.3.4.8
                throw new CheckingException(Constants.BUNDLE_VERSION_ATTRIBUTE + " is missing");
            }

            if (! Checker.checkVersion(br.getVersion())) { // 114.3.4.8
                throw new CheckingException(br.getVersion() + " is not a valid version");
            }

            if (br.getName() == null) { // 114.3.3
                throw new CheckingException("Section name is missing for " + br.getSymbolicName());
            }

            //TODO Check path consistency
//            if (! Checker.checkPathName(br.getName())) { // 114.3.2
//                throw new CheckingException("Section name '" + br.getName() + "' is not a valid path");
//            }
        }

        for (Resource res : resources) {
            // Must exist ... Check the Handle
            try {
                if (! res.exists()) {
                    throw new CheckingException("The resource file does not exist : " + res.getName());
                }
            } catch (IOException e) {
                throw new CheckingException("The resource cannot be loaded correctly : " + res.getName());
            }

            if (res.getName() == null) { // 114.3.3
                throw new CheckingException("Section name is missing for a resource");
            }

            //TODO Check path consistency
//            if (! Checker.checkPathName(res.getName())) { // 114.3.2
//                throw new CheckingException("Section name '" + res.getName() + "' is not a valid path");
//            }

            if (res.getProcessor() == null) { // 114.3.4.9
                throw new CheckingException(Constants.RESOURCE_PROCESSOR + " is missing for '" + res.getName() + "'");
            }

            if (! Checker.checkPID(res.getProcessor())) { // 114.3.4.9
                throw new CheckingException(res.getProcessor() + "'" + res.getProcessor() + "' is not a valid pid");
            }
        }

    }

    public void build(File to) throws IOException, CheckingException {
        to.mkdirs();
        if (to.exists()) {
            to.delete();
        }

        OutputStream out=new FileOutputStream(to);
        InputStream is = build();
        byte buf[]=new byte[1024];
        int len;
        while((len=is.read(buf))>0) {
            out.write(buf,0,len);
        }
        out.close();
        is.close();
    }

    public InputStream build() throws CheckingException, IOException {
        check();

        // 1. Manifest
        Manifest manifestDP = new OrderedManifest();
        // defaults
        manifestDP.getMainAttributes().putValue( "Manifest-Version", "1.0" );
        manifestDP.getMainAttributes().putValue( "Content-Type", "application/vnd.osgi.dp" );

        // user defined
        for( String key : entries.keySet()) {
            manifestDP.getMainAttributes().putValue( key, entries.get( key ) );
        }

        /*
         * First META-INF resources: META-INF/*.SF, META-INF/*.DSA, META-INF/*.RS
         * Then OSGi-INF
         * Then Bundles
         * Then Resources
         */
        final Map<String, Attributes> entries = manifestDP.getEntries();
        
        for (BundleResource br : bundles) {
            // extract meta data..
            Attributes attr = new Attributes();
            attr.putValue(Constants.BUNDLE_SYMBOLICNAME, br.getSymbolicName());
            attr.putValue(Constants.SHA_ATTRIBUTE, br.getSHA1().toString());
            attr.putValue(Constants.BUNDLE_VERSION, br.getVersion());
            if (fixPack != null) {
                attr.putValue(Constants.DEPLOYMENTPACKAGE_MISSING, Boolean.toString((br.isMissing())));
            } else {
            	if (br.isMissing()) {
            		throw new CheckingException("Cannot use missing bundle if the package does not define the "
            				+ Constants.DEPLOYMENTPACKAGE_FIXPACK + " entry");
            	}
            }
            if (br.isCustomizer()) {
                attr.putValue(Constants.DEPLOYMENTPACKAGE_CUSTOMIZER, Boolean.toString((br.isCustomizer())));
            }
            entries.put( br.getName(), attr );
        }

        for (Resource res : resources) {
            // extract meta data..
            Attributes attr = new Attributes();
            attr.putValue(Constants.SHA_ATTRIBUTE, res.getSHA1().toString());
            attr.putValue(Constants.RESOURCE_PROCESSOR, res.getProcessor());

            entries.put( res.getName(), attr );
        }
        
        // Manifest done, create the DP
        //final PipedInputStream pin = new PipedInputStream();
        //final PipedOutputStream pout = new PipedOutputStream(pin);
        
        File tmp = File.createTempFile("dp-", ".dp");
        FileOutputStream t = new FileOutputStream(tmp);
        final JarOutputStream jarOut = new JarOutputStream(t, manifestDP );

        try {

            // Start file bundles
            for (BundleResource br : bundles) {
                if (!br.isMissing()) {
                    copy(br.getName(), br.getInputStream(), jarOut);
                }
            }

            for (Resource res : resources) {
                copy(res.getName(), res.getInputStream(), jarOut);
            }

        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            try {
                if (jarOut != null) {
                    jarOut.close();
                }
                // pout.close();
                t.close();

            }
            catch (Exception e) {
                // be quiet.
            }
        }

        return new FileInputStream(tmp);
    }

    private void copy(String nameSection, InputStream inputStream,
            JarOutputStream jarOut) throws IOException {
        ZipEntry zipEntry = new JarEntry(nameSection);
        jarOut.putNextEntry(zipEntry);
        StreamUtils.copyStream(inputStream, jarOut, false);
        jarOut.closeEntry();
    }


}
