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

public class CustomizerTest {


    private static final String TEST_NAME = "customizer";

    File testDir = new File("src/test/resources/" + TEST_NAME);

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
        verifier.deleteArtifact( Helper.TEST_GROUP_ID, "test-" + TEST_NAME, Helper.TEST_VERSION, "dp" );


        verifier.executeGoal( "clean" );
    }

    @Test
    public void testPackage() throws IOException, VerificationException {

        Verifier verifier  = new Verifier( testDir.getAbsolutePath() );


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
        File result = new File(testDir + "/target/test-" + TEST_NAME + "-0.0.1-SNAPSHOT.dp"); // Expected name
        Assert.assertTrue(result.exists());

        JarFile jar = new JarFile(result);
        JarEntry je = jar.getJarEntry("bundles/org.apache.felix.bundlerepository-1.4.2.jar");
        Assert.assertNotNull(je);

        JarEntry je2 = jar.getJarEntry("bundles/org.apache.felix.log-1.0.0.jar");
        Assert.assertNotNull(je2);

        Manifest manifest = jar.getManifest();
        String sn = manifest.getMainAttributes().getValue(Constants.DEPLOYMENTPACKAGE_SYMBOLICMAME);
        String ve = manifest.getMainAttributes().getValue(Constants.DEPLOYMENTPACKAGE_VERSION);

        Assert.assertEquals("de.akquinet.gomobile.deployment.test." + TEST_NAME, sn);
        Assert.assertEquals("0.0.1.SNAPSHOT", ve);

        Attributes section = manifest.getAttributes("bundles/org.apache.felix.bundlerepository-1.4.2.jar");
        String bsn = section.getValue("Bundle-SymbolicName");
        String bve = section.getValue("Bundle-Version");
        String cutomizer = section.getValue("DeploymentPackage-Customizer");

        Assert.assertEquals("org.apache.felix.bundlerepository", bsn);
        Assert.assertEquals("1.4.2", bve);
        Assert.assertEquals("true", cutomizer);


        section = manifest.getAttributes("bundles/org.apache.felix.log-1.0.0.jar");
        bsn = section.getValue("Bundle-SymbolicName");
        bve = section.getValue("Bundle-Version");

        Assert.assertEquals("org.apache.felix.log", bsn);
        Assert.assertEquals("1.0.0", bve);

    }

}
