package de.akquinet.gomobile.deploymentadmin;

import java.util.jar.Attributes;

import org.osgi.service.deploymentadmin.DeploymentException;

/**
 * This class represents the meta data of a processed resource as used by the Deployment Admin.
 */
public class ResourceInfoImpl extends AbstractInfo {

    private String m_resourceProcessor;

    /**
     * Create an instance of this class.
     *
     * @param path String containing the path / resource-id of the processed resource.
     * @param attributes Attributes containing the meta data of the resource.
     * @throws DeploymentException If the specified attributes do not describe a processed resource.
     */
    public ResourceInfoImpl(String path, Attributes attributes) throws DeploymentException {
        super(path, attributes);
        m_resourceProcessor = attributes.getValue(Constants.RESOURCE_PROCESSOR);
    }

    /**
     * Determines the resource processor for this processed resource.
     *
     * @return String containing the PID of the resource processor that should handle this processed resource.
     */
    public String getResourceProcessor() {
        return m_resourceProcessor;
    }
}
