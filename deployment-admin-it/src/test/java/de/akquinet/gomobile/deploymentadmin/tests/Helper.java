package de.akquinet.gomobile.deploymentadmin.tests;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.asFile;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.newBundle;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.withBnd;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import junit.framework.Assert;

import org.ops4j.pax.exam.Option;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import de.akquinet.gomobile.deploymentadmin.tests.configurablebundle.ConfigurableActivator;
import de.akquinet.gomobile.deploymentadmin.tests.dummybundle.MyActivator;
import de.akquinet.gomobile.deploymentadmin.tests.myprocessor.MyProcessor;
import de.akquinet.gomobile.deploymentadmin.tests.myprocessor.ProcessorActivator;


public class Helper {

    public static final String BUNDLE1_SN = "org.apache.felix.bundlerepository";
    public static final String BUNDLE1_V = "1.4.2";
    public static final String BUNDLE2_SN = "org.apache.felix.log";
    public static final String BUNDLE2_V = "1.0.0";

    public static final String RESOURCE_PROCESSOR = "org.osgi.deployment.rp.autoconf";
    public static final String RESOURCE_NAME = "ms.xml";

    public static final String MY_RESOURCE_PROCESSOR = ProcessorActivator.PID;
    public static final String MY_RESOURCE_NAME = "foo.properties";

    public static URL BUNDLE1;
    public static URL BUNDLE2;
    public static URL RESOURCE;
    public static URL MY_RESOURCE;


    static {
        try {
            BUNDLE1 = new URL("file:src/test/resources/bundles/org.apache.felix.bundlerepository-1.4.2.jar");
            BUNDLE2 = new URL("file:src/test/resources/bundles/org.apache.felix.log-1.0.0.jar");
            RESOURCE = new URL("file:src/test/resources/conf/ms.xml");
            MY_RESOURCE = new URL("file:src/test/resources/props/foo.properties");
        } catch (MalformedURLException e) {
            Assert.fail("Cannot create bundle url");
        }
    }

    public static Option getDPBundles() {
       return  provision(
               mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.configadmin").versionAsInProject(),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo").versionAsInProject(),
                wrappedBundle(mavenBundle().groupId("de.akquinet.gomobile").artifactId("deployment-package-api").versionAsInProject()),
                mavenBundle().groupId("de.akquinet.gomobile").artifactId("deployment-admin-impl").versionAsInProject(),
                mavenBundle().groupId("de.akquinet.gomobile").artifactId("autoconf-resource-processor").versionAsInProject()
        );
    }

    public static File createConfigurableBundle() {
        File file = new File("target/dp-test/configurable.jar");
        if (file.exists()) {
            return file;
        } else {
            return newBundle()
            .addClass( ConfigurableActivator.class )
            .prepare(
                    withBnd()
                    .set( Constants.BUNDLE_SYMBOLICNAME, "de.akquinet.gomobile.configurable" )
                    .set( Constants.IMPORT_PACKAGE, "*" )
                    .set( Constants.BUNDLE_ACTIVATOR, ConfigurableActivator.class.getName() )
            ).build( asFile(file));
        }
    }

    public static File createRPBundle() {
        File file = new File("target/dp-test/resource-processor.jar");
        if (file.exists()) {
            return file;
        } else {
            return newBundle()
            .addClass( ProcessorActivator.class )
            .addClass( MyProcessor.class )
            .prepare(
                    withBnd()
                    .set( Constants.BUNDLE_SYMBOLICNAME, "de.akquinet.gomobile.rp" )
                    .set( Constants.IMPORT_PACKAGE, "*" )
                    .set( Constants.BUNDLE_ACTIVATOR, ProcessorActivator.class.getName() )
                    .set( Constants.BUNDLE_VERSION, "1.0.0" )
            ).build( asFile(file));
        }
    }

    public static File createRPBundleV2() {
        File file = new File("target/dp-test/resource-processor.jar");
        if (file.exists()) {
            return file;
        } else {
            return newBundle()
            .addClass( ProcessorActivator.class )
            .addClass( MyProcessor.class )
            .prepare(
                    withBnd()
                    .set( Constants.BUNDLE_SYMBOLICNAME, "de.akquinet.gomobile.rp" )
                    .set( Constants.IMPORT_PACKAGE, "*" )
                    .set( Constants.BUNDLE_ACTIVATOR, ProcessorActivator.class.getName() )
                    .set( Constants.BUNDLE_VERSION, "2.0.0" )
            ).build( asFile(file));
        }
    }

    public static File createBundleV1() {
        File file = new File("target/dp-test/bundle-v1.jar");
        if (file.exists()) {
            return file;
        } else {
            file.getParentFile().mkdirs();
            return newBundle()
            .addClass( MyActivator.class )
            .prepare(
                    withBnd()
                    .set( Constants.BUNDLE_SYMBOLICNAME, "de.akquinet.gomobile.bundle" )
                    .set( Constants.IMPORT_PACKAGE, "*" )
                    .set (Constants.BUNDLE_VERSION, "1.0.0")
                    .set( Constants.BUNDLE_ACTIVATOR, MyActivator.class.getName() )
            ).build( asFile(file));
        }
    }

    public static File createBundleV2() {
        File file = new File("target/dp-test/bundle-v2.jar");
        if (file.exists()) {
            return file;
        } else {
            file.getParentFile().mkdirs();
            return newBundle()
            .addClass( MyActivator.class )
            .prepare(
                    withBnd()
                    .set( Constants.BUNDLE_SYMBOLICNAME, "de.akquinet.gomobile.bundle" )
                    .set( Constants.IMPORT_PACKAGE, "*" )
                    .set (Constants.BUNDLE_VERSION, "2.0.0")
                    .set( Constants.BUNDLE_ACTIVATOR, MyActivator.class.getName() )
            ).build( asFile(file));
        }
    }


    public Bundle getBundleByName(BundleContext context, String n) {
        Bundle[] bundles = context.getBundles();
        for(Bundle b : bundles) {
            if(b.getSymbolicName().contains(n)) {
                return b;
            }
        }
        return null;
    }

}
