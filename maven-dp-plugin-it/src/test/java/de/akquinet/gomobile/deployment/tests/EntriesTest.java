package de.akquinet.gomobile.deployment.tests;

import java.io.File;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import junit.framework.Assert;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.Before;
import org.junit.Test;

import de.akquinet.gomobile.deployment.api.Constants;

public class EntriesTest {

    File testDir = new File("src/test/resources/entries");

    @Before
    public void setUp() throws VerificationException, IOException {
        Verifier verifier;

        /*
         * We must first make sure that any artifact created
         * by this test has been removed from the local
         * repository. Failing to do this could cause
         * unstable test results. Fortunately, the verifier
         * makes it easy to do this.
         */
        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( Helper.TEST_GROUP_ID, "test-entries", Helper.TEST_VERSION, "dp" );

        verifier.executeGoal( "clean" );
    }

    @Test
    public void testPackage() throws IOException, VerificationException {

        Verifier verifier  = new Verifier( testDir.getAbsolutePath() );

        /*
         * The Command Line Options (CLI) are passed to the
         * verifier as a list. This is handy for things like
         * redefining the local repository if needed. In
         * this case, we use the -N flag so that Maven won't
         * recurse. We are only installing the parent pom to
         * the local repo here.
         */
        verifier.executeGoal( "package" );

        /*
         * This is the simplest way to check a build
         * succeeded. It is also the simplest way to create
         * an IT test: make the build pass when the test
         * should pass, and make the build fail when the
         * test should fail. There are other methods
         * supported by the verifier. They can be seen here:
         * http://maven.apache.org/shared/maven-verifier/apidocs/index.html
         */
        verifier.verifyErrorFreeLog();

        /*
         * Reset the streams before executing the verifier
         * again.
         */
        verifier.resetStreams();

        // Check existency
        File result = new File(testDir + "/target/test-entries-0.0.1-SNAPSHOT.dp"); // Expected name
        Assert.assertTrue(result.exists());

        JarFile jar = new JarFile(result);
        JarEntry je = jar.getJarEntry("org.apache.felix.bundlerepository-1.4.2.jar");
        Assert.assertNotNull(je);

        Manifest manifest = jar.getManifest();
        String sn = manifest.getMainAttributes().getValue(Constants.DEPLOYMENTPACKAGE_SYMBOLICMAME);
        String ve = manifest.getMainAttributes().getValue(Constants.DEPLOYMENTPACKAGE_VERSION);
        String n = manifest.getMainAttributes().getValue(Constants.DEPLOYMENTPACKAGE_NAME);
        String ca = manifest.getMainAttributes().getValue(Constants.DEPLOYMENTPACKAGE_CONTACTADDRESS);
        String co = manifest.getMainAttributes().getValue(Constants.DEPLOYMENTPACKAGE_COPYRIGHT);
        String desc = manifest.getMainAttributes().getValue(Constants.DEPLOYMENTPACKAGE_DESCRIPTION);
        String doc = manifest.getMainAttributes().getValue(Constants.DEPLOYMENTPACKAGE_DOCURL);
        String ico = manifest.getMainAttributes().getValue(Constants.DEPLOYMENTPACKAGE_ICON);
        String lic = manifest.getMainAttributes().getValue(Constants.DEPLOYMENTPACKAGE_LICENSE);
        String req = manifest.getMainAttributes().getValue(Constants.DEPLOYMENTPACKAGE_REQUIREDSTORAGE);
        String vendor = manifest.getMainAttributes().getValue(Constants.DEPLOYMENTPACKAGE_VENDOR);

        Assert.assertEquals("sn", sn);
        Assert.assertEquals("0.1", ve);
        Assert.assertEquals("description", desc);
        Assert.assertEquals("name", n);
        Assert.assertEquals("copyright", co);
        Assert.assertEquals("me@me.com", ca);
        Assert.assertEquals("http://thedoc", doc);
        Assert.assertEquals("vendor", vendor);
        Assert.assertEquals("file:icon.png", ico);
        Assert.assertEquals("license", lic);
        Assert.assertEquals("1000", req);

        // Extra parameter
        String cb = manifest.getMainAttributes().getValue("Created-By");
        String t = manifest.getMainAttributes().getValue("Tool");
        String d = manifest.getMainAttributes().getValue("Created-At");

        Assert.assertNotNull(cb);
        Assert.assertNotNull(t);
        Assert.assertNotNull(d);

        Attributes section = manifest.getAttributes("org.apache.felix.bundlerepository-1.4.2.jar");
        String bsn = section.getValue("Bundle-SymbolicName");
        String bve = section.getValue("Bundle-Version");

        Assert.assertEquals("org.apache.felix.bundlerepository", bsn);
        Assert.assertEquals("1.4.2", bve);

    }

}
