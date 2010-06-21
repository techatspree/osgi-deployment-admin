package de.akquinet.gomobile.deploymentadmin.tests;

import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;

import de.akquinet.gomobile.deployment.api.BundleResource;
import de.akquinet.gomobile.deployment.api.CheckingException;


@RunWith(JUnit4TestRunner.class)
public class UpdateBundleTest extends Helper {

    @Inject
    private BundleContext context;

    @Configuration
    public static Option[] configure() throws Exception {

        String orig = createDPVersion1().toExternalForm();
        String updatebundle = createDPBundleUpdate().toExternalForm();
        String addbundle = createDPAddBundle().toExternalForm();
        String rembundle = createDPRemoveBundle().toExternalForm();
        String missingbundle = createDPMissingBundle().toExternalForm();


           Option[] opt =  options(
                   felix(),
                   systemProperty( "org.osgi.framework.storage.clean" ).value( "onFirstInit" ),
                   systemProperty("dpv1").value(orig),
                   systemProperty("dpv2").value(updatebundle),
                   systemProperty("add").value(addbundle),
                   systemProperty("rem").value(rembundle),
                   systemProperty("missing").value(missingbundle),
                   Helper.getDPBundles()
                   );
           return opt;
       }


    @Test
    public void updateWithANewVersion() throws DeploymentException, IOException {
        DeploymentAdmin admin = (DeploymentAdmin) context.getService(context.getServiceReference(DeploymentAdmin.class.getName()));
        Assert.assertNotNull(admin);

        URL d = new URL((String)context.getProperty("dpv1"));
        DeploymentPackage dp = admin.installDeploymentPackage(d.openStream());
        Assert.assertEquals("1.0.0", dp.getVersion().toString());

        // Check installed bundle
        Bundle b1 = getBundleByName(context, "org.apache.felix.bundlerepository");
        Assert.assertNotNull(b1);

        Bundle b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());
        Assert.assertEquals(Bundle.ACTIVE, b1.getState());

        Assert.assertEquals("1.0.0", b2.getHeaders().get(Constants.BUNDLE_VERSION));

        URL d2 = new URL((String)context.getProperty("dpv2"));
        DeploymentPackage dp2 = admin.installDeploymentPackage(d2.openStream());
        Assert.assertEquals("1.1.0", dp2.getVersion().toString());

        b1 = getBundleByName(context, "org.apache.felix.bundlerepository");
        Assert.assertNotNull(b1);

        b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());
        Assert.assertEquals(Bundle.ACTIVE, b1.getState());

        Assert.assertEquals("2.0.0", b2.getHeaders().get(Constants.BUNDLE_VERSION));

        dp2.uninstall();

        b1 = getBundleByName(context, "org.apache.felix.bundlerepository");
        Assert.assertNull(b1);

        b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNull(b2);
    }

    @Test
    public void updateWithAnOlderVersion() throws DeploymentException, IOException {
        DeploymentAdmin admin = (DeploymentAdmin) context.getService(context.getServiceReference(DeploymentAdmin.class.getName()));
        Assert.assertNotNull(admin);

        URL d = new URL((String)context.getProperty("dpv2"));
        DeploymentPackage dp = admin.installDeploymentPackage(d.openStream());
        Assert.assertEquals("1.1.0", dp.getVersion().toString());

        // Check installed bundle
        Bundle b1 = getBundleByName(context, "org.apache.felix.bundlerepository");
        Assert.assertNotNull(b1);

        Bundle b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());
        Assert.assertEquals(Bundle.ACTIVE, b1.getState());

        Assert.assertEquals("2.0.0", b2.getHeaders().get(Constants.BUNDLE_VERSION));

        URL d2 = new URL((String)context.getProperty("dpv1"));
        DeploymentPackage dp2 = admin.installDeploymentPackage(d2.openStream());
        Assert.assertEquals("1.0.0", dp2.getVersion().toString());

        b1 = getBundleByName(context, "org.apache.felix.bundlerepository");
        Assert.assertNotNull(b1);

        b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());
        Assert.assertEquals(Bundle.ACTIVE, b1.getState());

        Assert.assertEquals("1.0.0", b2.getHeaders().get(Constants.BUNDLE_VERSION));

        dp2.uninstall();

        b1 = getBundleByName(context, "org.apache.felix.bundlerepository");
        Assert.assertNull(b1);

        b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNull(b2);
    }

    @Test
    public void updateWithTheSameVersion() throws DeploymentException, IOException {
        DeploymentAdmin admin = (DeploymentAdmin) context.getService(context.getServiceReference(DeploymentAdmin.class.getName()));
        Assert.assertNotNull(admin);

        URL d = new URL((String)context.getProperty("dpv2"));
        DeploymentPackage dp = admin.installDeploymentPackage(d.openStream());
        Assert.assertEquals("1.1.0", dp.getVersion().toString());

        // Check installed bundle
        Bundle b1 = getBundleByName(context, "org.apache.felix.bundlerepository");
        Assert.assertNotNull(b1);

        Bundle b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());
        Assert.assertEquals(Bundle.ACTIVE, b1.getState());

        Assert.assertEquals("2.0.0", b2.getHeaders().get(Constants.BUNDLE_VERSION));

        URL d2 = new URL((String)context.getProperty("dpv2"));
        DeploymentPackage dp2 = admin.installDeploymentPackage(d2.openStream());
        Assert.assertEquals("1.1.0", dp2.getVersion().toString());

        b1 = getBundleByName(context, "org.apache.felix.bundlerepository");
        Assert.assertNotNull(b1);

        b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());
        Assert.assertEquals(Bundle.ACTIVE, b1.getState());

        Assert.assertEquals("2.0.0", b2.getHeaders().get(Constants.BUNDLE_VERSION));

        dp2.uninstall();

        b1 = getBundleByName(context, "org.apache.felix.bundlerepository");
        Assert.assertNull(b1);

        b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNull(b2);
    }


    @Test
    public void addBundle() throws DeploymentException, IOException {
        DeploymentAdmin admin = (DeploymentAdmin) context.getService(context.getServiceReference(DeploymentAdmin.class.getName()));
        Assert.assertNotNull(admin);

        URL d = new URL((String)context.getProperty("dpv1"));
        DeploymentPackage dp = admin.installDeploymentPackage(d.openStream());
        Assert.assertEquals("1.0.0", dp.getVersion().toString());

        // Check installed bundle
        Bundle b1 = getBundleByName(context, "org.apache.felix.bundlerepository");
        Assert.assertNotNull(b1);

        Bundle b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNotNull(b2);

        Bundle b3 = getBundleByName(context, "org.apache.felix.log");
        Assert.assertNull(b3);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());
        Assert.assertEquals(Bundle.ACTIVE, b1.getState());


        URL d2 = new URL((String)context.getProperty("add"));
        DeploymentPackage dp2 = admin.installDeploymentPackage(d2.openStream());
        Assert.assertEquals("1.1.0", dp2.getVersion().toString());

        b1 = getBundleByName(context, "org.apache.felix.bundlerepository");
        Assert.assertNotNull(b1);

        b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNotNull(b2);

        b3 = getBundleByName(context, "org.apache.felix.log");
        Assert.assertNotNull(b3);


        Assert.assertEquals(Bundle.ACTIVE, b2.getState());
        Assert.assertEquals(Bundle.ACTIVE, b1.getState());
        Assert.assertEquals(Bundle.ACTIVE, b3.getState());

        dp2.uninstall();

        b1 = getBundleByName(context, "org.apache.felix.bundlerepository");
        Assert.assertNull(b1);

        b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNull(b2);

        b3 = getBundleByName(context, "org.apache.felix.log");
        Assert.assertNull(b3);
    }

    @Test
    public void removeBundle() throws DeploymentException, IOException {
        DeploymentAdmin admin = (DeploymentAdmin) context.getService(context.getServiceReference(DeploymentAdmin.class.getName()));
        Assert.assertNotNull(admin);

        URL d = new URL((String)context.getProperty("dpv1"));
        DeploymentPackage dp = admin.installDeploymentPackage(d.openStream());
        Assert.assertEquals("1.0.0", dp.getVersion().toString());

        // Check installed bundle
        Bundle b1 = getBundleByName(context, "org.apache.felix.bundlerepository");
        Assert.assertNotNull(b1);

        Bundle b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());
        Assert.assertEquals(Bundle.ACTIVE, b1.getState());


        URL d2 = new URL((String)context.getProperty("rem"));
        DeploymentPackage dp2 = admin.installDeploymentPackage(d2.openStream());
        Assert.assertEquals("1.1.0", dp2.getVersion().toString());

        b1 = getBundleByName(context, "org.apache.felix.bundlerepository");
        Assert.assertNull(b1);

        b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());

        dp2.uninstall();

        b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNull(b2);

    }

    @Test
    public void missingBundle() throws DeploymentException, IOException {
        DeploymentAdmin admin = (DeploymentAdmin) context.getService(context.getServiceReference(DeploymentAdmin.class.getName()));
        Assert.assertNotNull(admin);

        URL d = new URL((String)context.getProperty("dpv1"));
        DeploymentPackage dp = admin.installDeploymentPackage(d.openStream());
        Assert.assertEquals("1.0.0", dp.getVersion().toString());

        // Check installed bundle
        Bundle b1 = getBundleByName(context, "org.apache.felix.bundlerepository");
        Assert.assertNotNull(b1);

        Bundle b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());
        Assert.assertEquals(Bundle.ACTIVE, b1.getState());


        URL d2 = new URL((String)context.getProperty("missing"));
        DeploymentPackage dp2 = admin.installDeploymentPackage(d2.openStream());
        Assert.assertEquals("1.1.0", dp2.getVersion().toString());

        b1 = getBundleByName(context, "org.apache.felix.bundlerepository");
        Assert.assertNull(b1);

        b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());

        dp2.uninstall();

        b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNull(b2);

    }

    public static URL createDPVersion1() throws IOException, CheckingException {
        File bundle = createBundleV1();
        de.akquinet.gomobile.deployment.api.DeploymentPackage dp
        	= new de.akquinet.gomobile.deployment.api.DeploymentPackage();
        dp
            .addBundle(bundle.toURI().toURL())
            .addBundle(BUNDLE1)
            .setSymbolicName("my.dp").setVersion("1.0.0");

        File dpf = new File("target/dp-update/dp-v1.dp");

        dp.build(dpf);

        return dpf.toURI().toURL();
    }

    public static URL createDPBundleUpdate() throws IOException, CheckingException {
        File bundle = createBundleV2();
        de.akquinet.gomobile.deployment.api.DeploymentPackage dp
        	= new de.akquinet.gomobile.deployment.api.DeploymentPackage();
        dp
            .addBundle(bundle.toURI().toURL())
            .addBundle(BUNDLE1)
            .setSymbolicName("my.dp").setVersion("1.1.0");

        File dpf = new File("target/dp-update/dp-bundle_update.dp");

        dp.build(dpf);

        return dpf.toURI().toURL();
    }

    public static URL createDPAddBundle() throws IOException, CheckingException {
        File bundle = createBundleV1();
        de.akquinet.gomobile.deployment.api.DeploymentPackage dp
        	= new de.akquinet.gomobile.deployment.api.DeploymentPackage();
        dp
            .addBundle(bundle.toURI().toURL())
            .addBundle(BUNDLE1)
            .addBundle(BUNDLE2) // Add a bundle
            .setSymbolicName("my.dp").setVersion("1.1.0");

        File dpf = new File("target/dp-update/dp-add_bundle.dp");

        dp.build(dpf);

        return dpf.toURI().toURL();
    }

    public static URL createDPRemoveBundle() throws IOException, CheckingException {
        File bundle = createBundleV1();
        de.akquinet.gomobile.deployment.api.DeploymentPackage dp
        	= new de.akquinet.gomobile.deployment.api.DeploymentPackage();
        dp
            .addBundle(bundle.toURI().toURL())
            //.addBundle(BUNDLE1) // Remove OBR
            .setSymbolicName("my.dp").setVersion("1.1.0");

        File dpf = new File("target/dp-update/dp-remove_bundle.dp");

        dp.build(dpf);

        return dpf.toURI().toURL();
    }

    public static URL createDPMissingBundle() throws IOException, CheckingException {
        File bundle = createBundleV1();
        de.akquinet.gomobile.deployment.api.DeploymentPackage dp
        	= new de.akquinet.gomobile.deployment.api.DeploymentPackage();
        dp
            .addBundle(new BundleResource().setMissing(true).setURL(bundle.toURI().toURL()))
            //.addBundle(BUNDLE1) // Remove OBR
            .setSymbolicName("my.dp")
            .setVersion("1.1.0")
            .setFixPackage("[1.0.0, 1.1.0)");

        File dpf = new File("target/dp-update/dp-missing_bundle.dp");

        dp.build(dpf);

        return dpf.toURI().toURL();
    }



}
