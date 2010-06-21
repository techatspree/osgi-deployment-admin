package de.akquinet.gomobile.deployment.mojo;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;

import de.akquinet.gomobile.deployment.api.DeploymentPackage;

/**
 * <p>
 * From the Deployment Admin Specification:
 * </p>
 * <p>
 * A Deployment Package consists of installable <i>resources</i>. Resources are
 * described in the <i>Name sections</i> of the Manifest. They are stored in the
 * JAR file under a path. This path is called the <i>resource id</i>. Subsets of
 * these resources are the bundles. Bundles are treated differently from the
 * other resources by the Deployment Admin service. Non-bundle resources are
 * called <i>processed resources</i>.
 * </p>
 */
public class ProcessedResource implements Resource {

    private String m_filePath;

    private String m_targetPath;

    private de.akquinet.gomobile.deployment.api.Resource m_resource;

    /**
     * A constructor which initializes an instance of a
     * {@link ProcessedResource}.
     */
    public ProcessedResource() {
        m_filePath = null;
        m_targetPath = null;

        m_resource = new de.akquinet.gomobile.deployment.api.Resource();
    }

    /**
     * @return the file
     */
    public final String getFilePath() {
        return m_filePath;
    }

    /**
     * @param file the file to set
     */
    public final void setFilePath(final String file) {

        m_filePath = file;
    }

    /**
     * @return the targetPath
     */
    public final String getTargetPath() {
        return m_targetPath;
    }

    /**
     * @param target the targetPath to set
     */
    public final void setTargetPath(final String target) {
        m_targetPath = target;
    }

    /**
     * @return the processor
     */
    public final String getProcessor() {
        return m_resource.getProcessor();
    }

    /**
     * @param processor the processor to set
     */
    public final void setProcessor(final String processor) {
        m_resource.setProcessor(processor);
    }

    public void resolve(DeploymentPackage dp) throws MojoExecutionException {
        File f = new File(m_filePath);
        String n = f.getName();

        try {
            m_resource.setURL(f.toURI().toURL());
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage());
        }
        if (m_targetPath != null && m_targetPath.length() > 0) {
            m_resource.setPath(m_targetPath + "/" + n);
        } else {
            m_resource.setPath(n);
        }

        dp.addResource(m_resource);

    }

    public Object getResource() {
       return m_resource;
    }


}
