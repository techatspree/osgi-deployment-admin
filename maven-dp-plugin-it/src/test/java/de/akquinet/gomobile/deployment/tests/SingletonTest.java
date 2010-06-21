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

public class SingletonTest {

    File testDir = new File("src/test/resources/singleton");

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
        verifier.deleteArtifact( Helper.TEST_GROUP_ID, "test-singleton", Helper.TEST_VERSION, "dp" );

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
        File result = new File(testDir + "/target/test-singleton-0.0.1-SNAPSHOT.dp"); // Expected name
        System.out.println("result : " + result.getAbsolutePath());
        Assert.assertTrue(result.exists());

        JarFile jar = new JarFile(result);
        JarEntry je = jar.getJarEntry("org.apache.felix.ipojo-1.4.0.jar");
        Assert.assertNotNull(je);

        Manifest manifest = jar.getManifest();
        String sn = manifest.getMainAttributes().getValue(Constants.DEPLOYMENTPACKAGE_SYMBOLICMAME);
        String ve = manifest.getMainAttributes().getValue(Constants.DEPLOYMENTPACKAGE_VERSION);

        Assert.assertEquals("de.akquinet.gomobile.deployment.test.singleton", sn);
        Assert.assertEquals("0.0.1.SNAPSHOT", ve);

        Attributes section = manifest.getAttributes("org.apache.felix.ipojo-1.4.0.jar");
        String bsn = section.getValue("Bundle-SymbolicName");
        String bve = section.getValue("Bundle-Version");

        Assert.assertEquals("org.apache.felix.ipojo", bsn);
        Assert.assertEquals("1.4.0", bve);

    }

}
