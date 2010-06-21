package de.akquinet.gomobile.deploymentadmin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import org.osgi.framework.BundleContext;
import org.osgi.service.deploymentadmin.DeploymentException;

/**
 * This class represents a deployment package that is read from a jar stream.
 */
class StreamDeploymentPackage extends AbstractDeploymentPackage {

    private final JarInputStream m_input;
    private final List m_names = new ArrayList();

    /**
     * Creates an instance of this class.
     *
     * @param input The stream from which the deployment package can be read.
     * @param bundleContext The bundle context.
     * @param admin the deployment admin.
     * @throws DeploymentException If it was not possible to read a valid deployment package from the specified stream.
     */
    public StreamDeploymentPackage(JarInputStream input, BundleContext bundleContext, DeploymentAdminImpl admin) throws DeploymentException {
        super(input.getManifest(), bundleContext, admin);
        m_input = input;
    }

    public InputStream getBundleStream(String symbolicName) {
        throw new UnsupportedOperationException("Not applicable for stream-based deployment package");
    }

    // This only works for those resources that have been read from the stream already, no guarantees for remainder of stream
    public BundleInfoImpl[] getOrderedBundleInfos() {
        List result = new ArrayList();

        // add all bundle resources ordered by location in stream
        for(Iterator i = m_names.iterator(); i.hasNext();) {
            String indexEntry = (String) i.next();
            AbstractInfo bundleInfo = getBundleInfoByPath(indexEntry);
            if (bundleInfo != null) {
                result.add(bundleInfo);
            }
        }

        // add bundle resources marked missing to the end of the result
        BundleInfoImpl[] bundleInfoImpls = getBundleInfoImpls();
        for (int i = 0; i < bundleInfoImpls.length; i++) {
            if(bundleInfoImpls[i].isMissing()) {
                result.add(bundleInfoImpls[i]);
            }
        }
        return (BundleInfoImpl[]) result.toArray(new BundleInfoImpl[result.size()]);
    }

    public ResourceInfoImpl[] getOrderedResourceInfos() {
        throw new UnsupportedOperationException("Not applicable for stream-based deployment package");
    }

    public AbstractInfo getNextEntry() throws IOException {
        ZipEntry nextEntry = m_input.getNextJarEntry();
        if (nextEntry == null) {
            return null;
        }
        String name = nextEntry.getName();
        m_names.add(name);
        AbstractInfo abstractInfoByPath = getAbstractInfoByPath(name);
        return abstractInfoByPath;
    }

    public InputStream getCurrentEntryStream() {
        return new NonCloseableStream(m_input);
    }

}
