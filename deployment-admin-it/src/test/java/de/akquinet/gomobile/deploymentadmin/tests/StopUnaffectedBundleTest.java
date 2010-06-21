package de.akquinet.gomobile.deploymentadmin.tests;

import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;

import de.akquinet.gomobile.deployment.api.CheckingException;


@RunWith(JUnit4TestRunner.class)
public class StopUnaffectedBundleTest extends Helper {

    @Inject
    private BundleContext context;

    @Configuration
    public static Option[] configure() throws Exception {

        String orig = createDPVersion1().toExternalForm();
        String rembundle = createDPRemoveBundle().toExternalForm();


           Option[] opt =  options(
                   felix(),
                   systemProperty( "org.osgi.framework.storage.clean" ).value( "onFirstInit" ),
                   systemProperty("dpv1").value(orig),
                   systemProperty("rem").value(rembundle),
                   Helper.getDPBundles()
                   //systemProperty("de.akquinet.gomobile.deploymentadmin.stopunaffectedbundle").value("false")
                   );
           return opt;
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

        BundleEventCollector collector = new BundleEventCollector();
        context.addBundleListener(collector);

        URL d2 = new URL((String)context.getProperty("rem"));
        DeploymentPackage dp2 = admin.installDeploymentPackage(d2.openStream());
        Assert.assertEquals("1.1.0", dp2.getVersion().toString());

        b1 = getBundleByName(context, "org.apache.felix.bundlerepository");
        Assert.assertNull(b1);

        b2 = getBundleByName(context, "de.akquinet.gomobile.bundle");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());

        // stopped
        Assert.assertTrue(collector.wasStopped("de.akquinet.gomobile.bundle"));
        context.removeBundleListener(collector);

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

    private class BundleEventCollector implements BundleListener {

    	private List<BundleEvent> list = new ArrayList<BundleEvent>();

		public void bundleChanged(BundleEvent event) {
			synchronized (this) {
				list.add(event);
			}
		}

		public boolean wasStopped(String bundle) {
			List<BundleEvent> l = null;
			synchronized (this) {
				l = new ArrayList<BundleEvent>(list);
			}

			for (BundleEvent ev : l) {
				if (ev.getBundle().getSymbolicName().equals(bundle)  && ev.getType() == BundleEvent.STOPPED) {
					return true;
				}
			}

			return false;
		}

    }

}
