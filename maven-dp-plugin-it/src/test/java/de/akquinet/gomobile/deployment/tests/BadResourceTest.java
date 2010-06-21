package de.akquinet.gomobile.deployment.tests;

import java.io.File;
import java.io.IOException;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.Before;
import org.junit.Test;

public class BadResourceTest {

    File testDir = new File("src/test/resources/bad-resource");

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
        verifier.deleteArtifact( Helper.TEST_GROUP_ID, "test-bad-resource", Helper.TEST_VERSION, "dp" );

        verifier.executeGoal( "clean" );
    }

    @Test(expected=VerificationException.class)
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

        verifier.verifyErrorFreeLog();

    }
}
