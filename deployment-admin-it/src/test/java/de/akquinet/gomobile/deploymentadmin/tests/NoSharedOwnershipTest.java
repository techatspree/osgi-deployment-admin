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
public class NoSharedOwnershipTest extends Helper {

    @Inject
    private BundleContext context;

    @Configuration
    public static Option[] configure() throws Exception {

        String dp1 = createDPWithABundle().toExternalForm();
        String dp2 = createDPWithTheSameBundle().toExternalForm();


           Option[] opt =  options(
                   felix(),
                   systemProperty( "org.osgi.framework.storage.clean" ).value( "onFirstInit" ),
                   systemProperty("dp1").value(dp1),
                   systemProperty("dp2").value(dp2),
                   Helper.getDPBundles()
                   );
           return opt;
       }


    @Test
    public void checkDefaultBehavior() throws DeploymentException, IOException {
        DeploymentAdmin admin = (DeploymentAdmin) context.getService(context.getServiceReference(DeploymentAdmin.class.getName()));
        Assert.assertNotNull(admin);

        URL d = new URL((String)context.getProperty("dp1"));
        DeploymentPackage dp = admin.installDeploymentPackage(d.openStream());

        Bundle b1 = getBundleByName(context, "org.apache.felix.log");
        Assert.assertNull(b1);

        Bundle b2 = getBundleByName(context, "org.apache.felix.bundlerepository");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());


        URL d1 = new URL((String)context.getProperty("dp2"));
        DeploymentPackage dp2 = admin.installDeploymentPackage(d1.openStream());


        b1 = getBundleByName(context, "org.apache.felix.log");
        Assert.assertNotNull(b1);

        b2 = getBundleByName(context, "org.apache.felix.bundlerepository");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b1.getState());
        Assert.assertEquals(Bundle.ACTIVE, b2.getState());

        dp.uninstall();

        // Bundle Repository uninstalled.
        b1 = getBundleByName(context, "org.apache.felix.log");
        Assert.assertNotNull(b1);

        b2 = getBundleByName(context, "org.apache.felix.bundlerepository");
        Assert.assertNull(b2);

        dp2.uninstall();
        b1 = getBundleByName(context, "org.apache.felix.log");
        Assert.assertNull(b1);


    }

    public static URL createDPWithABundle() throws IOException, CheckingException {
        de.akquinet.gomobile.deployment.api.DeploymentPackage dp
            = new de.akquinet.gomobile.deployment.api.DeploymentPackage();

        dp.addBundle(BUNDLE1).setSymbolicName("my.first.dp").setVersion("1.0.0");

        File dpf = new File("target/dp-ownership/dp1.dp");

        dp.build(dpf);

        return dpf.toURI().toURL();
    }

    public static URL createDPWithTheSameBundle() throws Exception {
        de.akquinet.gomobile.deployment.api.DeploymentPackage dp
        = new de.akquinet.gomobile.deployment.api.DeploymentPackage();

        dp.addBundle(BUNDLE1, "bundle/b1.jar").setSymbolicName("my.first2.dp").setVersion("1.0.0");
        dp.addBundle(BUNDLE2);

        File dpf = new File("target/dp-ownership/dp2.dp");

        dp.build(dpf);

        return dpf.toURI().toURL();
    }



}
