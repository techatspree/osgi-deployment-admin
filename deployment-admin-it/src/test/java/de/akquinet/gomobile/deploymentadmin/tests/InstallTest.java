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
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;

import de.akquinet.gomobile.deployment.api.BundleResource;
import de.akquinet.gomobile.deployment.api.CheckingException;


@RunWith(JUnit4TestRunner.class)
public class InstallTest extends Helper {

    @Inject
    private BundleContext context;

    @Configuration
    public static Option[] configure() throws Exception {

        String dp1 = createDPWithOneBundle().toExternalForm();
        String dp11 = createDPWithAPath().toExternalForm();
        String dp2 = createDPWithTwoBundles().toExternalForm();
        String dp3 = createDPWithAResource().toExternalForm();
        String dp31 = createDPWithAResourceInAPath().toExternalForm();
        String empty = createEmptyDP().toExternalForm();
        String dp4 = createDPWithACustomizer().toExternalForm();


           Option[] opt =  options(
                   felix(),
                   systemProperty( "org.osgi.framework.storage.clean" ).value( "onFirstInit" ),
                   systemProperty("dp1").value(dp1),
                   systemProperty("dp11").value(dp11),
                   systemProperty("dp2").value(dp2),
                   systemProperty("dp3").value(dp3),
                   systemProperty("dp31").value(dp31),
                   systemProperty("empty").value(empty),
                   systemProperty("dp4").value(dp4),
                   Helper.getDPBundles()
                   );
           return opt;
       }


    @Test
    public void installSimpleDP() throws DeploymentException, IOException {
        DeploymentAdmin admin = (DeploymentAdmin) context.getService(context.getServiceReference(DeploymentAdmin.class.getName()));
        Assert.assertNotNull(admin);

        URL d = new URL((String)context.getProperty("dp1"));
        DeploymentPackage dp = admin.installDeploymentPackage(d.openStream());

        // Check installed bundle
        Bundle b2 = getBundleByName("org.apache.felix.bundlerepository");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());

        dp.uninstall();

        b2 = getBundleByName("org.apache.felix.bundlerepository");
        Assert.assertNull(b2);
    }

    @Test
    public void installDPWithPath() throws DeploymentException, IOException {
        DeploymentAdmin admin = (DeploymentAdmin) context.getService(context.getServiceReference(DeploymentAdmin.class.getName()));
        Assert.assertNotNull(admin);

        URL d = new URL((String)context.getProperty("dp11"));
        DeploymentPackage dp = admin.installDeploymentPackage(d.openStream());

        // Check installed bundle
        Bundle b2 = getBundleByName("org.apache.felix.bundlerepository");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());

        dp.uninstall();

        b2 = getBundleByName("org.apache.felix.bundlerepository");
        Assert.assertNull(b2);
    }

    @Test
    public void installWithTwoBundles() throws DeploymentException, IOException {
        DeploymentAdmin admin = (DeploymentAdmin) context.getService(context.getServiceReference(DeploymentAdmin.class.getName()));
        Assert.assertNotNull(admin);

        URL d = new URL((String)context.getProperty("dp2"));
        DeploymentPackage dp = admin.installDeploymentPackage(d.openStream());

        // Check installed bundle
        Bundle b1 = getBundleByName("org.apache.felix.log");
        Assert.assertNotNull(b1);
        Bundle b2 = getBundleByName("org.apache.felix.bundlerepository");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());
        Assert.assertEquals(Bundle.ACTIVE, b1.getState());

        dp.uninstall();

        b1 = getBundleByName("org.apache.felix.log");
        b2 = getBundleByName("org.apache.felix.bundlerepository");
        Assert.assertNull(b1);
        Assert.assertNull(b2);
    }

    @Test
    public void installWithAResource() throws DeploymentException, Exception, InvalidSyntaxException {
        DeploymentAdmin admin = (DeploymentAdmin) context.getService(context.getServiceReference(DeploymentAdmin.class.getName()));
        Assert.assertNotNull(admin);

        ConfigurationAdmin ca = (ConfigurationAdmin) context.getService(context.getServiceReference(ConfigurationAdmin.class.getName()));
        Assert.assertNotNull(ca);

        URL d = new URL((String)context.getProperty("dp3"));
        DeploymentPackage dp = admin.installDeploymentPackage(d.openStream());

        // Check installed bundle
        Bundle b2 = getBundleByName("org.apache.felix.bundlerepository");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());


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

        dp.uninstall();

        Thread.sleep(1000); // Wait for processing.
        //TODO Change that by a waitForConfiguration...


        b2 = getBundleByName("org.apache.felix.bundlerepository");
        Assert.assertNull(b2);

        configurations = ca.listConfigurations(null);
        if (configurations != null) { // Must be removed...
            Assert.fail("Unexpected Configuration found...");
        }
    }

    @Test
    public void installWithAResourceWithAPath() throws DeploymentException, Exception, InvalidSyntaxException {
        DeploymentAdmin admin = (DeploymentAdmin) context.getService(context.getServiceReference(DeploymentAdmin.class.getName()));
        Assert.assertNotNull(admin);

        ConfigurationAdmin ca = (ConfigurationAdmin) context.getService(context.getServiceReference(ConfigurationAdmin.class.getName()));
        Assert.assertNotNull(ca);

        URL d = new URL((String)context.getProperty("dp31"));
        DeploymentPackage dp = admin.installDeploymentPackage(d.openStream());

        // Check installed bundle
        Bundle b2 = getBundleByName("org.apache.felix.bundlerepository");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());


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

        dp.uninstall();

        Thread.sleep(1000); // Wait for processing.
        //TODO Change that by a waitForConfiguration...


        b2 = getBundleByName("org.apache.felix.bundlerepository");
        Assert.assertNull(b2);

        configurations = ca.listConfigurations(null);
        if (configurations != null) { // Must be removed...
            Assert.fail("Unexpected Configuration found...");
        }
    }

    @Test
    public void installWithAResourceWithoutCA() throws DeploymentException, Exception, InvalidSyntaxException {
        // Stop CA:
        Bundle b = getBundleByName("org.apache.felix.configadmin");
        Assert.assertNotNull(b);
        b.stop();

        DeploymentAdmin admin = (DeploymentAdmin) context.getService(context.getServiceReference(DeploymentAdmin.class.getName()));
        Assert.assertNotNull(admin);

        ServiceReference ca = context.getServiceReference(ConfigurationAdmin.class.getName());
        Assert.assertNull(ca); // Uninstalled...

        URL d = new URL((String)context.getProperty("dp31"));

        try {
            admin.installDeploymentPackage(d.openStream());
            Assert.fail("Must fail : No CA");
        } catch (DeploymentException exp) {
            // Ok
        }

        // Check installed bundle
        Bundle b2 = getBundleByName("org.apache.felix.bundlerepository");
        Assert.assertNull(b2);
        Bundle b1 = getBundleByName("de.akquinet.gomobile.configurable");
        Assert.assertNull(b1);

    }

    @Test
    public void installEmptyDP() throws DeploymentException, Exception, InvalidSyntaxException {
        int count = context.getBundles().length;
        DeploymentAdmin admin = (DeploymentAdmin) context.getService(context.getServiceReference(DeploymentAdmin.class.getName()));
        Assert.assertNotNull(admin);

       URL d = new URL((String)context.getProperty("empty"));

        DeploymentPackage dp = admin.installDeploymentPackage(d.openStream());

        Assert.assertEquals(count, context.getBundles().length);

        dp.uninstall();

        Assert.assertEquals(count, context.getBundles().length);
    }

    @Test
    public void uninstallForcedOK() throws DeploymentException, Exception, InvalidSyntaxException {
        DeploymentAdmin admin = (DeploymentAdmin) context.getService(context.getServiceReference(DeploymentAdmin.class.getName()));
        Assert.assertNotNull(admin);

        ConfigurationAdmin ca = (ConfigurationAdmin) context.getService(context.getServiceReference(ConfigurationAdmin.class.getName()));
        Assert.assertNotNull(ca);

        URL d = new URL((String)context.getProperty("dp3"));
        DeploymentPackage dp = admin.installDeploymentPackage(d.openStream());

        // Check installed bundle
        Bundle b2 = getBundleByName("org.apache.felix.bundlerepository");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());


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

        boolean result = dp.uninstallForced();


        Thread.sleep(1000); // Wait for processing.
        //TODO Change that by a waitForConfiguration...


        b2 = getBundleByName("org.apache.felix.bundlerepository");
        Assert.assertNull(b2);

        configurations = ca.listConfigurations(null);
        if (configurations != null) { // Must be removed...
            Assert.fail("Unexpected Configuration found...");
        }

        // No problem expected, result = true
        Assert.assertTrue(result);
    }



    @Test
    public void uninstallForcedKO() throws DeploymentException, Exception, InvalidSyntaxException {
        DeploymentAdmin admin = (DeploymentAdmin) context.getService(context.getServiceReference(DeploymentAdmin.class.getName()));
        Assert.assertNotNull(admin);

        ConfigurationAdmin ca = (ConfigurationAdmin) context.getService(context.getServiceReference(ConfigurationAdmin.class.getName()));
        Assert.assertNotNull(ca);

        URL d = new URL((String)context.getProperty("dp3"));
        DeploymentPackage dp = admin.installDeploymentPackage(d.openStream());

        // Check installed bundle
        Bundle b2 = getBundleByName("org.apache.felix.bundlerepository");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());


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

        // Stop the CA
        Bundle b = getBundleByName("org.apache.felix.configadmin");
        Assert.assertNotNull(b);
        b.stop();

        boolean result = dp.uninstallForced();


        Thread.sleep(1000); // Wait for processing.
        //TODO Change that by a waitForConfiguration...


        b2 = getBundleByName("org.apache.felix.bundlerepository");
        Assert.assertNull(b2);

        // Problem expected (no CA), result = false
        Assert.assertFalse(result);
    }

    @Test
    public void installWithACustomizer() throws DeploymentException, Exception, InvalidSyntaxException {
        DeploymentAdmin admin = (DeploymentAdmin) context.getService(context.getServiceReference(DeploymentAdmin.class.getName()));
        Assert.assertNotNull(admin);

        ConfigurationAdmin ca = (ConfigurationAdmin) context.getService(context.getServiceReference(ConfigurationAdmin.class.getName()));
        Assert.assertNotNull(ca);

        URL d = new URL((String)context.getProperty("dp4"));
        DeploymentPackage dp = admin.installDeploymentPackage(d.openStream());

        // Check installed bundle
        Bundle b2 = getBundleByName("org.apache.felix.bundlerepository");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());


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

        dp.uninstall();

        Thread.sleep(1000); // Wait for processing.
        //TODO Change that by a waitForConfiguration...


        b2 = getBundleByName("org.apache.felix.bundlerepository");
        Assert.assertNull(b2);

        configurations = ca.listConfigurations(null);
        if (configurations != null) { // Must be removed...
            Assert.fail("Unexpected Configuration found...");
        }
    }



    private Bundle getBundleByName(String n) {
        Bundle[] bundles = context.getBundles();
        for(Bundle b : bundles) {
            if(b.getSymbolicName().contains(n)) {
                return b;
            }
        }
        return null;
    }


    public static URL createDPWithOneBundle() throws IOException, CheckingException {
        de.akquinet.gomobile.deployment.api.DeploymentPackage dp
            = new de.akquinet.gomobile.deployment.api.DeploymentPackage();

        dp.addBundle(BUNDLE1).setSymbolicName("my.first.dp").setVersion("1.0.0");

        File dpf = new File("target/dp-test/dp1.dp");

        dp.build(dpf);

        return dpf.toURI().toURL();
    }

    public static URL createDPWithTwoBundles() throws Exception {
        de.akquinet.gomobile.deployment.api.DeploymentPackage dp
        = new de.akquinet.gomobile.deployment.api.DeploymentPackage();

        dp.addBundle(BUNDLE1).addBundle(BUNDLE2)
            .setSymbolicName("my.second.dp").setVersion("1.0.0");

        File dpf = new File("target/dp-test/dp2.dp");

        dp.build(dpf);

        return dpf.toURI().toURL();
    }

    public static URL createDPWithAPath() throws Exception {
        de.akquinet.gomobile.deployment.api.DeploymentPackage dp
        = new de.akquinet.gomobile.deployment.api.DeploymentPackage();

        dp.addBundle(BUNDLE1, "bundle/b1.jar").setSymbolicName("my.first2.dp").setVersion("1.0.0");

        File dpf = new File("target/dp-test/dp1-1.dp");

        dp.build(dpf);

        return dpf.toURI().toURL();
    }

    public static URL createDPWithAResource() throws IOException, CheckingException {
        File bundle = createConfigurableBundle();

        de.akquinet.gomobile.deployment.api.DeploymentPackage dp
            = new de.akquinet.gomobile.deployment.api.DeploymentPackage();

        dp
            .addResource(RESOURCE, RESOURCE_PROCESSOR)
            .addBundle(BUNDLE1)
            .addBundle(bundle.toURI().toURL())
            .setSymbolicName("my.third.dp").setVersion("1.0.0");

        File dpf = new File("target/dp-test/dp3.dp");

        dp.build(dpf);

        return dpf.toURI().toURL();
    }


    public static URL createDPWithAResourceInAPath() throws IOException, CheckingException {
        File bundle = createConfigurableBundle();

        de.akquinet.gomobile.deployment.api.DeploymentPackage dp
            = new de.akquinet.gomobile.deployment.api.DeploymentPackage();

        dp
            .addResource(RESOURCE, RESOURCE_PROCESSOR, "conf/ms.xml")
            .addBundle(BUNDLE1)
            .addBundle(bundle.toURI().toURL())
            .setSymbolicName("my.third.dp").setVersion("1.0.0");

        File dpf = new File("target/dp-test/dp3-1.dp");

        dp.build(dpf);

        return dpf.toURI().toURL();
    }

    public static URL createEmptyDP() throws IOException, CheckingException {
        de.akquinet.gomobile.deployment.api.DeploymentPackage dp
            = new de.akquinet.gomobile.deployment.api.DeploymentPackage();

        dp
            .setSymbolicName("empty-dp").setVersion("1.0.0");

        File dpf = new File("target/dp-test/empty.dp");

        dp.build(dpf);

        return dpf.toURI().toURL();
    }

    public static URL createDPWithACustomizer() throws IOException, CheckingException {
        File bundle = createConfigurableBundle();
        File rp = createRPBundle();
        de.akquinet.gomobile.deployment.api.DeploymentPackage dp
            = new de.akquinet.gomobile.deployment.api.DeploymentPackage();

        dp
            .addResource(RESOURCE, RESOURCE_PROCESSOR, "conf/ms.xml")
            .addResource(MY_RESOURCE, MY_RESOURCE_PROCESSOR, "props/prop.properties")
            .addBundle(BUNDLE1)
            .addBundle(new BundleResource().setURL(rp.toURI().toURL()).setCustomizer(true))
            .addBundle(bundle.toURI().toURL())
            .setSymbolicName("my.third.dp").setVersion("1.0.0");

        File dpf = new File("target/dp-test/dp3-1.dp");

        dp.build(dpf);

        return dpf.toURI().toURL();
    }


}
