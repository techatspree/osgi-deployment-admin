package de.akquinet.gomobile.deploymentadmin.tests;

import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

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
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import de.akquinet.gomobile.deployment.api.CheckingException;


@RunWith(JUnit4TestRunner.class)
public class EventAdminTest extends Helper {

    @Inject
    private BundleContext context;

    @Configuration
    public static Option[] configure() throws Exception {

        String dp1 = createDPWithOneBundle().toExternalForm();
        String dp3 = createDPWithAResource().toExternalForm();
        String dp1v2 = createDPWithOneBundleV2().toExternalForm();



           Option[] opt =  options(
                   felix(),
                   systemProperty( "org.osgi.framework.storage.clean" ).value( "onFirstInit" ),
                   systemProperty("dp1").value(dp1),
                   systemProperty("dp31").value(dp3),
                   systemProperty("dp1v2").value(dp1v2),
                   Helper.getDPBundles(),
                   provision(
                           mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.eventadmin").versionAsInProject()
                   ));
           return opt;
       }


    private class EventCollector implements EventHandler {

        private final BundleContext context;
        private final String[] topics;
        private final List<Event> events = new ArrayList<Event>();
        private ServiceRegistration reg;

        public EventCollector(BundleContext bc, String[] topics) {
            context = bc;
            this.topics = topics;
        }

        public void handleEvent(Event event) {
            synchronized (this) {
                events.add(event);
            }
        }

        public void start() {
            if (reg != null) {
                reg.unregister();
            } else {
                Dictionary dict = new Properties();
                dict.put(EventConstants.EVENT_TOPIC, topics);
                reg = context.registerService(EventHandler.class.getName(), this, dict);
            }
        }

        public void stop() {
            if (reg != null) {
                reg.unregister();
                reg = null;
            }
        }

        public void clear() {
            synchronized (this) {
                events.clear();
            }
        }

        public List<Event> getEvents() {
            synchronized (this) {
                return new ArrayList<Event>(events);
            }
        }

        public List<Event> getEventsFromTopic(String topic) {
            List<Event> result = new ArrayList<Event>();
            List<Event> list = null ;
            synchronized (this) {
                list = new ArrayList<Event>(events);
            }

            for (Event ev : list) {
                if (ev.getTopic().equals(topic)) {
                    result.add(ev);
                }
            }

            return result;
        }

    }

    @Test
    public void install() throws DeploymentException, IOException, InterruptedException {
        DeploymentAdmin admin = (DeploymentAdmin) context.getService(context.getServiceReference(DeploymentAdmin.class.getName()));
        Assert.assertNotNull(admin);

        EventCollector collector = new EventCollector(context, new String[] {"org/osgi/service/deployment/INSTALL",
                "org/osgi/service/deployment/UNINSTALL", "org/osgi/service/deployment/COMPLETE"});
        collector.start();

        URL d = new URL((String)context.getProperty("dp1"));
        DeploymentPackage dp = admin.installDeploymentPackage(d.openStream());

        // Check installed bundle
        Bundle b2 = getBundleByName("org.apache.felix.bundlerepository");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());

        // Two events expected
        Thread.sleep(1000);

        // Install event
        List<Event> install = collector.getEventsFromTopic("org/osgi/service/deployment/INSTALL");
        Assert.assertEquals(1, install.size());
        Event ev = install.get(0);
        Assert.assertEquals("my.first.dp", ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NAME));
        Assert.assertNull(ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_READABLENAME));
        Assert.assertNull(ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_CURRENTVERSION));
        Assert.assertEquals(Version.parseVersion("1.0.0"), ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NEXTVERSION));

        List<Event> complete = collector.getEventsFromTopic("org/osgi/service/deployment/COMPLETE");
        Assert.assertEquals(1, complete.size());
        ev = complete.get(0);
        Assert.assertEquals("my.first.dp", ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NAME));
        Assert.assertNull(ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_READABLENAME));
        Assert.assertNull(ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_CURRENTVERSION));
        Assert.assertEquals(Version.parseVersion("1.0.0"), ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NEXTVERSION));
        Assert.assertTrue(((Boolean) ev.getProperty("successful")).booleanValue());

        collector.clear();

        dp.uninstall();

        b2 = getBundleByName("org.apache.felix.bundlerepository");
        Assert.assertNull(b2);

        // Two events expected
        Thread.sleep(1000);

        List<Event> uninstall = collector.getEventsFromTopic("org/osgi/service/deployment/UNINSTALL");
        Assert.assertEquals(1, uninstall.size());
        ev = uninstall.get(0);
        Assert.assertEquals("my.first.dp", ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NAME));
        Assert.assertNull(ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_READABLENAME));
        complete = collector.getEventsFromTopic("org/osgi/service/deployment/COMPLETE");
        Assert.assertEquals(1, complete.size());
        ev = complete.get(0);
        Assert.assertEquals("my.first.dp", ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NAME));
        Assert.assertTrue(((Boolean) ev.getProperty("successful")).booleanValue());
    }

    @Test
    public void update() throws DeploymentException, IOException, InterruptedException {
        DeploymentAdmin admin = (DeploymentAdmin) context.getService(context.getServiceReference(DeploymentAdmin.class.getName()));
        Assert.assertNotNull(admin);

        EventCollector collector = new EventCollector(context, new String[] {"org/osgi/service/deployment/INSTALL",
                "org/osgi/service/deployment/UNINSTALL", "org/osgi/service/deployment/COMPLETE"});
        collector.start();

        URL d = new URL((String)context.getProperty("dp1"));
        DeploymentPackage dp = admin.installDeploymentPackage(d.openStream());

        // Check installed bundle
        Bundle b2 = getBundleByName("org.apache.felix.bundlerepository");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());

        // Two events expected
        Thread.sleep(1000);

        // Install event
        List<Event> install = collector.getEventsFromTopic("org/osgi/service/deployment/INSTALL");
        Assert.assertEquals(1, install.size());
        Event ev = install.get(0);
        Assert.assertEquals("my.first.dp", ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NAME));
        Assert.assertNull(ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_READABLENAME));
        Assert.assertNull(ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_CURRENTVERSION));
        Assert.assertEquals(Version.parseVersion("1.0.0"), ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NEXTVERSION));

        List<Event> complete = collector.getEventsFromTopic("org/osgi/service/deployment/COMPLETE");
        Assert.assertEquals(1, complete.size());
        ev = complete.get(0);
        Assert.assertEquals("my.first.dp", ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NAME));
        Assert.assertNull(ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_READABLENAME));
        Assert.assertNull(ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_CURRENTVERSION));
        Assert.assertEquals(Version.parseVersion("1.0.0"), ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NEXTVERSION));
        Assert.assertTrue(((Boolean) ev.getProperty("successful")).booleanValue());

        collector.clear();

        // Update...
        d = new URL((String)context.getProperty("dp1v2"));
        DeploymentPackage dp2 = admin.installDeploymentPackage(d.openStream());

        // Two events expected
        Thread.sleep(1000);

        // Install event
        install = collector.getEventsFromTopic("org/osgi/service/deployment/INSTALL");
        Assert.assertEquals(1, install.size());
        ev = install.get(0);
        Assert.assertEquals("my.first.dp", ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NAME));
        Assert.assertNull(ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_READABLENAME));
        Assert.assertEquals(Version.parseVersion("1.0.0"), ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_CURRENTVERSION));
        Assert.assertEquals(Version.parseVersion("2.0.0"), ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NEXTVERSION));

        complete = collector.getEventsFromTopic("org/osgi/service/deployment/COMPLETE");
        Assert.assertEquals(1, complete.size());
        ev = complete.get(0);
        Assert.assertEquals("my.first.dp", ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NAME));
        Assert.assertNull(ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_READABLENAME));
        Assert.assertEquals(Version.parseVersion("1.0.0"), ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_CURRENTVERSION));
        Assert.assertEquals(Version.parseVersion("2.0.0"), ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NEXTVERSION));
        Assert.assertTrue(((Boolean) ev.getProperty("successful")).booleanValue());

        collector.clear();

        dp2.uninstall();

        b2 = getBundleByName("org.apache.felix.bundlerepository");
        Assert.assertNull(b2);

        // Two events expected
        Thread.sleep(1000);

        List<Event> uninstall = collector.getEventsFromTopic("org/osgi/service/deployment/UNINSTALL");
        Assert.assertEquals(1, uninstall.size());
        ev = uninstall.get(0);
        Assert.assertEquals("my.first.dp", ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NAME));
        Assert.assertNull(ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_READABLENAME));
        complete = collector.getEventsFromTopic("org/osgi/service/deployment/COMPLETE");
        Assert.assertEquals(1, complete.size());
        ev = complete.get(0);
        Assert.assertEquals("my.first.dp", ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NAME));
        Assert.assertTrue(((Boolean) ev.getProperty("successful")).booleanValue());
    }

    @Test
    public void uninstallForced() throws DeploymentException, IOException, InterruptedException {
        DeploymentAdmin admin = (DeploymentAdmin) context.getService(context.getServiceReference(DeploymentAdmin.class.getName()));
        Assert.assertNotNull(admin);

        EventCollector collector = new EventCollector(context, new String[] {"org/osgi/service/deployment/INSTALL",
                "org/osgi/service/deployment/UNINSTALL", "org/osgi/service/deployment/COMPLETE"});
        collector.start();

        URL d = new URL((String)context.getProperty("dp1"));
        DeploymentPackage dp = admin.installDeploymentPackage(d.openStream());

        // Check installed bundle
        Bundle b2 = getBundleByName("org.apache.felix.bundlerepository");
        Assert.assertNotNull(b2);

        Assert.assertEquals(Bundle.ACTIVE, b2.getState());

        // Two events expected
        Thread.sleep(1000);

        // Install event
        List<Event> install = collector.getEventsFromTopic("org/osgi/service/deployment/INSTALL");
        Assert.assertEquals(1, install.size());
        Event ev = install.get(0);
        Assert.assertEquals("my.first.dp", ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NAME));
        Assert.assertNull(ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_READABLENAME));
        Assert.assertNull(ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_CURRENTVERSION));
        Assert.assertEquals(Version.parseVersion("1.0.0"), ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NEXTVERSION));

        List<Event> complete = collector.getEventsFromTopic("org/osgi/service/deployment/COMPLETE");
        Assert.assertEquals(1, complete.size());
        ev = complete.get(0);
        Assert.assertEquals("my.first.dp", ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NAME));
        Assert.assertNull(ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_READABLENAME));
        Assert.assertNull(ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_CURRENTVERSION));
        Assert.assertEquals(Version.parseVersion("1.0.0"), ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NEXTVERSION));
        Assert.assertTrue(((Boolean) ev.getProperty("successful")).booleanValue());

        collector.clear();

        dp.uninstallForced();

        b2 = getBundleByName("org.apache.felix.bundlerepository");
        Assert.assertNull(b2);

        // Two events expected
        Thread.sleep(1000);

        List<Event> uninstall = collector.getEventsFromTopic("org/osgi/service/deployment/UNINSTALL");
        Assert.assertEquals(1, uninstall.size());
        ev = uninstall.get(0);
        Assert.assertEquals("my.first.dp", ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NAME));
        Assert.assertNull(ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_READABLENAME));
        complete = collector.getEventsFromTopic("org/osgi/service/deployment/COMPLETE");
        Assert.assertEquals(1, complete.size());
        ev = complete.get(0);
        Assert.assertEquals("my.first.dp", ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NAME));
        Assert.assertTrue(((Boolean) ev.getProperty("successful")).booleanValue());
    }

    @Test
    public void installFailure() throws DeploymentException, Exception, InvalidSyntaxException {
        // Stop CA:
        Bundle b = getBundleByName("org.apache.felix.configadmin");
        Assert.assertNotNull(b);
        b.stop();

        DeploymentAdmin admin = (DeploymentAdmin) context.getService(context.getServiceReference(DeploymentAdmin.class.getName()));
        Assert.assertNotNull(admin);

        EventCollector collector = new EventCollector(context, new String[] {"org/osgi/service/deployment/INSTALL",
                "org/osgi/service/deployment/UNINSTALL", "org/osgi/service/deployment/COMPLETE"});
        collector.start();

        ServiceReference ca = context.getServiceReference(ConfigurationAdmin.class.getName());
        Assert.assertNull(ca); // Uninstalled...

        URL d = new URL((String)context.getProperty("dp31"));

        try {
            admin.installDeploymentPackage(d.openStream());
            Assert.fail("Must fail : No CA");
        } catch (DeploymentException exp) {
            // Ok
        }

        // Two events expected
        Thread.sleep(1000);

        // Install event
        List<Event> install = collector.getEventsFromTopic("org/osgi/service/deployment/INSTALL");
        Assert.assertEquals(1, install.size());
        Event ev = install.get(0);
        Assert.assertEquals("my.third.dp", ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NAME));
        Assert.assertNull(ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_READABLENAME));
        Assert.assertNull(ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_CURRENTVERSION));
        Assert.assertEquals(Version.parseVersion("1.0.0"), ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NEXTVERSION));

        List<Event> complete = collector.getEventsFromTopic("org/osgi/service/deployment/COMPLETE");
        Assert.assertEquals(1, complete.size());
        ev = complete.get(0);
        Assert.assertEquals("my.third.dp", ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NAME));
        Assert.assertNull(ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_READABLENAME));
        Assert.assertNull(ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_CURRENTVERSION));
        Assert.assertEquals(Version.parseVersion("1.0.0"), ev.getProperty(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NEXTVERSION));
        Assert.assertFalse(((Boolean) ev.getProperty("successful")).booleanValue());

        // Check installed bundle
        Bundle b2 = getBundleByName("org.apache.felix.bundlerepository");
        Assert.assertNull(b2);
        Bundle b1 = getBundleByName("de.akquinet.gomobile.configurable");
        Assert.assertNull(b1);

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

    public static URL createDPWithOneBundleV2() throws IOException, CheckingException {
        de.akquinet.gomobile.deployment.api.DeploymentPackage dp
            = new de.akquinet.gomobile.deployment.api.DeploymentPackage();

        dp.addBundle(BUNDLE1).setSymbolicName("my.first.dp").setVersion("2.0.0");

        File dpf = new File("target/dp-test/dp1-v2.dp");

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

}
