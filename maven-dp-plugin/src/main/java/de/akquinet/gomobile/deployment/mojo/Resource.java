package de.akquinet.gomobile.deployment.mojo;

import org.apache.maven.plugin.MojoExecutionException;

import de.akquinet.gomobile.deployment.api.DeploymentPackage;

/**
 * <p>
 * From the Deployment Admin Specification:
 * </p>
 * <p>
 * A Deployment Package consists of installable <i>resources</i>. Resources are described in the <i>Name sections</i> of the Manifest. They are stored in the
 * JAR file under a path. This path is called the <i>resource id</i>. Subsets of these resources are the bundles. Bundles are treated differently from the
 * other resources by the Deployment Admin service. Non-bundle resources are called <i>processed resources</i>.
 * </p>
 *
 */
public interface Resource {

  Object getResource();

  void resolve(DeploymentPackage dp) throws MojoExecutionException;
}
