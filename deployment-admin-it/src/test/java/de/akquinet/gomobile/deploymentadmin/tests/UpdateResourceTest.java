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
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;

import de.akquinet.gomobile.deployment.api.BundleResource;
import de.akquinet.gomobile.deployment.api.CheckingException;


@RunWith(JUnit4TestRunner.class)
public class UpdateResourceTest extends Helper {

    @Inject
    private BundleContext context;

    @Configuration
    public static Option[] configure() throws Exception {

        String orig = createDPConfigurableVersion1().toExternalForm();
        String addresource = createDPAddResource().toExternalForm();
        String remresource = createDPRemoveResource().toExternalForm();
        String addcustomizer = createDPAddCustomizer().toExternalForm();
        String updatecustomizer = createDPUpdateCustomizer().toExternalForm();



           Option[] opt =  options(
                   felix(),
                   systemProperty( "org.osgi.framework.storage.clean" ).value( "onFirstInit" ),
                   systemProperty("dpv1").value(orig),
                   systemProperty("add").value(addresource),
                   systemProperty("rem").value(remresource),
                   systemProperty("addc").value(addcustomizer),
                   systemProperty("upc").value(updatecustomizer),
                   Helper.getDPBundles()
                   );
           return opt;
       }


    @Test
    public void addResource() throws DeploymentException, IOException, InterruptedException, InvalidSyntaxException {
        DeploymentAdmin admin = (DeploymentAdmin) context.getService(context.getServiceReference(DeploymentAdmin.class.getName()));
        Assert.assertNotNull(admin);

        URL d = new URL((String)context.getProperty("dpv1"));
        DeploymentPackage dp = admin.installDeploymentPackage(d.openStream());
        Assert.assertEquals("1.0.0", dp.getVersion().toString());

        // Check installed bundle
        Bundle b1 = getBundleByName(context, "de.akquinet.gomobile.configurable");
        Assert.assertNotNull(b1);

        Bundle b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());
        Assert.assertEquals(Bundle.ACTIVE, b1.getState());

        URL d2 = new URL((String)context.getProperty("add"));
        DeploymentPackage dp2 = admin.installDeploymentPackage(d2.openStream());
        Assert.assertEquals("1.1.0", dp2.getVersion().toString());

        b1 = getBundleByName(context, "de.akquinet.gomobile.configurable");
        Assert.assertNotNull(b1);

        b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());
        Assert.assertEquals(Bundle.ACTIVE, b1.getState());

        ConfigurationAdmin ca = (ConfigurationAdmin) context.getService(context.getServiceReference(ConfigurationAdmin.class.getName()));
        Assert.assertNotNull(ca);

        // Check if the configuration was installed
        Thread.sleep(1000); // Wait for processing.
        //TODO Change that by a waitForConfiguration...

        org.osgi.service.cm.Configuration[] configurations = ca.listConfigurations(null);
        boolean found = false;
        for(org.osgi.service.cm.Configuration c : configurations) {
            if (c.getPid().equals("de.akquinet.gomobile.x")) {
                found = true;
            }
        }

        if (! found) {
            Assert.fail("Configuration not found...");
        }

        dp2.uninstall();

        configurations = ca.listConfigurations(null);
        if (configurations != null) { // Must be removed...
            Assert.fail("Unexpected Configuration found...");
        }

        b1 = getBundleByName(context, "de.akquinet.gomobile.configurable");
        Assert.assertNull(b1);

        b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNull(b2);

        Assert.assertTrue(dp2.isStale());
    }

    @Test
    public void removeResource() throws DeploymentException, IOException, InterruptedException, InvalidSyntaxException {
        DeploymentAdmin admin = (DeploymentAdmin) context.getService(context.getServiceReference(DeploymentAdmin.class.getName()));
        Assert.assertNotNull(admin);

        URL d = new URL((String)context.getProperty("add"));
        DeploymentPackage dp = admin.installDeploymentPackage(d.openStream());
        Assert.assertEquals("1.1.0", dp.getVersion().toString());

        // Check installed bundle
        Bundle b1 = getBundleByName(context, "de.akquinet.gomobile.configurable");
        Assert.assertNotNull(b1);

        Bundle b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());
        Assert.assertEquals(Bundle.ACTIVE, b1.getState());

        ConfigurationAdmin ca = (ConfigurationAdmin) context.getService(context.getServiceReference(ConfigurationAdmin.class.getName()));
        Assert.assertNotNull(ca);

        // Check if the configuration was installed
        Thread.sleep(1000); // Wait for processing.
        //TODO Change that by a waitForConfiguration...

        org.osgi.service.cm.Configuration[] configurations = ca.listConfigurations(null);
        boolean found = false;
        for(org.osgi.service.cm.Configuration c : configurations) {
            if (c.getPid().equals("de.akquinet.gomobile.x")) {
                found = true;
            }
        }

        if (! found) {
            Assert.fail("Configuration not found...");
        }


        URL d2 = new URL((String)context.getProperty("rem"));
        DeploymentPackage dp2 = admin.installDeploymentPackage(d2.openStream());
        Assert.assertEquals("1.1.1", dp2.getVersion().toString());

        b1 = getBundleByName(context, "de.akquinet.gomobile.configurable");
        Assert.assertNotNull(b1);

        b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());
        Assert.assertEquals(Bundle.ACTIVE, b1.getState());

        Thread.sleep(1000);

        configurations = ca.listConfigurations(null);
        if (configurations != null) { // Must be removed...
            Assert.fail("Unexpected Configuration found...");
        }

        dp2.uninstall();


        b1 = getBundleByName(context, "de.akquinet.gomobile.configurable");
        Assert.assertNull(b1);

        b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNull(b2);

        Assert.assertTrue(dp2.isStale());
    }

    @Test
    public void addcustomizer() throws DeploymentException, IOException, InterruptedException, InvalidSyntaxException {
        DeploymentAdmin admin = (DeploymentAdmin) context.getService(context.getServiceReference(DeploymentAdmin.class.getName()));
        Assert.assertNotNull(admin);

        URL d = new URL((String)context.getProperty("dpv1"));
        DeploymentPackage dp = admin.installDeploymentPackage(d.openStream());
        Assert.assertEquals("1.0.0", dp.getVersion().toString());

        // Check installed bundle
        Bundle b1 = getBundleByName(context, "de.akquinet.gomobile.configurable");
        Assert.assertNotNull(b1);

        Bundle b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNotNull(b2);

        Bundle b3 = getBundleByName(context, "de.akquinet.gomobile.rp");
        Assert.assertNull(b3);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());
        Assert.assertEquals(Bundle.ACTIVE, b1.getState());

        URL d2 = new URL((String)context.getProperty("addc"));
        DeploymentPackage dp2 = admin.installDeploymentPackage(d2.openStream());
        Assert.assertEquals("1.1.0", dp2.getVersion().toString());

        b1 = getBundleByName(context, "de.akquinet.gomobile.configurable");
        Assert.assertNotNull(b1);

        b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNotNull(b2);

        b3 = getBundleByName(context, "de.akquinet.gomobile.rp");
        Assert.assertNotNull(b3);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());
        Assert.assertEquals(Bundle.ACTIVE, b1.getState());
        Assert.assertEquals(Bundle.ACTIVE, b3.getState());

        URL d3 = new URL((String)context.getProperty("upc"));
        DeploymentPackage dp3 = admin.installDeploymentPackage(d3.openStream());
        Assert.assertEquals("1.1.1", dp3.getVersion().toString());


        dp3.uninstall();


        b1 = getBundleByName(context, "de.akquinet.gomobile.configurable");
        Assert.assertNull(b1);

        b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNull(b2);

        b3 = getBundleByName(context, "de.akquinet.gomobile.rp");
        Assert.assertNull(b3);

        Assert.assertTrue(dp3.isStale());
    }

    public static URL createDPConfigurableVersion1() throws IOException, CheckingException {
        File bundle = createBundleV1();
        File bundle2 = createConfigurableBundle();
        de.akquinet.gomobile.deployment.api.DeploymentPackage dp
        	= new de.akquinet.gomobile.deployment.api.DeploymentPackage();
        dp
            .addBundle(bundle.toURI().toURL())
            .addBundle(bundle2.toURI().toURL())
            .setSymbolicName("my.configurable.dp").setVersion("1.0.0");

        File dpf = new File("target/dp-update/dp-configurable-v1.dp");

        dp.build(dpf);

        return dpf.toURI().toURL();
    }

    public static URL createDPAddResource() throws IOException, CheckingException {
    	 File bundle = createBundleV1();
         File bundle2 = createConfigurableBundle();
         de.akquinet.gomobile.deployment.api.DeploymentPackage dp
         	= new de.akquinet.gomobile.deployment.api.DeploymentPackage();


        dp
            .addResource(RESOURCE, RESOURCE_PROCESSOR)
            .addBundle(bundle.toURI().toURL())
            .addBundle(bundle2.toURI().toURL())
            .setSymbolicName("my.configurable.dp").setVersion("1.1.0");

        File dpf = new File("target/dp-update/dp-configurable-addconf.dp");

        dp.build(dpf);

        return dpf.toURI().toURL();
    }

    public static URL createDPRemoveResource() throws IOException, CheckingException {
   	 File bundle = createBundleV1();
        File bundle2 = createConfigurableBundle();
        de.akquinet.gomobile.deployment.api.DeploymentPackage dp
        	= new de.akquinet.gomobile.deployment.api.DeploymentPackage();

        // This is an update of the dp-configurable-addconf.dp package

       dp
           //.addResource(RESOURCE, RESOURCE_PROCESSOR) // Remove the resource
           .addBundle(bundle.toURI().toURL())
           .addBundle(bundle2.toURI().toURL())
           .setSymbolicName("my.configurable.dp").setVersion("1.1.1")
           .setFixPackage("[1.0.0, 1.2.0)");

       File dpf = new File("target/dp-update/dp-configurable-remconf.dp");

       dp.build(dpf);

       return dpf.toURI().toURL();
   }

    public static URL createDPAddCustomizer() throws IOException, CheckingException {
    	File bundle = createBundleV1();
        File bundle2 = createConfigurableBundle();
        File rp = createRPBundle();
        de.akquinet.gomobile.deployment.api.DeploymentPackage dp
            = new de.akquinet.gomobile.deployment.api.DeploymentPackage();

        dp
            .addResource(MY_RESOURCE, MY_RESOURCE_PROCESSOR, "props/prop.properties")
            .addBundle(bundle.toURI().toURL())
            .addBundle(bundle2.toURI().toURL())
            .addBundle(new BundleResource().setURL(rp.toURI().toURL()).setCustomizer(true))
            .setSymbolicName("my.configurable.dp").setVersion("1.1.0");

        File dpf = new File("target/dp-update/dp-configurable-addcustomizer.dp");

        dp.build(dpf);

        return dpf.toURI().toURL();
    }

    public static URL createDPUpdateCustomizer() throws IOException, CheckingException {
    	File bundle = createBundleV1();
        File bundle2 = createConfigurableBundle();
        File rp = createRPBundleV2();
        de.akquinet.gomobile.deployment.api.DeploymentPackage dp
            = new de.akquinet.gomobile.deployment.api.DeploymentPackage();

        dp
            .addResource(MY_RESOURCE, MY_RESOURCE_PROCESSOR, "props/prop.properties")
            .addBundle(bundle.toURI().toURL())
            .addBundle(bundle2.toURI().toURL())
            .addBundle(new BundleResource().setURL(rp.toURI().toURL()).setCustomizer(true))
            .setSymbolicName("my.configurable.dp").setVersion("1.1.1");

        File dpf = new File("target/dp-update/dp-configurable-updatecustomizer.dp");

        dp.build(dpf);

        return dpf.toURI().toURL();
    }




}
