package de.akquinet.gomobile.deploymentadmin.spi;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.spi.ResourceProcessor;
import org.osgi.service.deploymentadmin.spi.ResourceProcessorException;

import de.akquinet.gomobile.deploymentadmin.AbstractDeploymentPackage;
import de.akquinet.gomobile.deploymentadmin.AbstractInfo;
import de.akquinet.gomobile.deploymentadmin.ResourceInfoImpl;

/**
 * Command that processes all the processed resources in the source deployment package
 * of a deployment session by finding their Resource Processors and having those process
 * the resources.
 */
public class ProcessResourceCommand extends Command {

    private final CommitResourceCommand m_commitCommand;

    /**
     * Creates an instance of this command, the <code>CommitCommand</code> is used
     * to ensure that all used <code>ResourceProcessor</code>s will be committed at a later
     * stage in the deployment session.
     *
     * @param commitCommand The <code>CommitCommand</code> that will commit all resource processors used in this command.
     */
    public ProcessResourceCommand(CommitResourceCommand commitCommand) {
        m_commitCommand = commitCommand;
        addRollback(m_commitCommand);
    }

    public void execute(DeploymentSessionImpl session) throws DeploymentException {
        AbstractDeploymentPackage source = session.getSourceAbstractDeploymentPackage();
        BundleContext context = session.getBundleContext();

        Map expectedResources = new HashMap();
        AbstractInfo[] resourceInfos = (AbstractInfo[]) source.getResourceInfos();
        for (int i = 0; i < resourceInfos.length; i++) {
            AbstractInfo resourceInfo = resourceInfos[i];
            if(!resourceInfo.isMissing()) {
                expectedResources.put(resourceInfo.getPath(), resourceInfo);
            }
        }

        try {
            while (!expectedResources.isEmpty()) {
                AbstractInfo jarEntry = source.getNextEntry();
                if (jarEntry == null) {
                    throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Expected more resources in the stream: " + expectedResources.keySet());
                }

                String name = jarEntry.getPath();

                ResourceInfoImpl resourceInfo = (ResourceInfoImpl) expectedResources.remove(name);
                if (resourceInfo == null) {
                    throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Resource '" + name + "' is not described in the manifest.");
                }

                ServiceReference ref = source.getResourceProcessor(name);
                if (ref != null) {
                    String serviceOwnerSymName = ref.getBundle().getSymbolicName();
                    //TODO Check if the processor is a customizer from another DP.
                    //if (source.getBundleInfoByName(serviceOwnerSymName) != null) {
                        ResourceProcessor resourceProcessor = (ResourceProcessor) context.getService(ref);
                        if (resourceProcessor != null) {
                            try {
                                if (m_commitCommand.addResourceProcessor(resourceProcessor)) {
                                    resourceProcessor.begin(session);
                                }
                                resourceProcessor.process(name, source.getCurrentEntryStream());
                            }
                            catch (ResourceProcessorException rpe) {
                                if (rpe.getCode() == ResourceProcessorException.CODE_RESOURCE_SHARING_VIOLATION) {
                                    throw new DeploymentException(DeploymentException.CODE_RESOURCE_SHARING_VIOLATION, "Violation while processing resource '" + name + "'", rpe);
                                }
                                else {
                                    throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Error while processing resource '" + name + "'", rpe);
                                }
                            }
                        }
                        else {
                            throw new DeploymentException(DeploymentException.CODE_PROCESSOR_NOT_FOUND, "No resource processor for resource: '" + name + "'");
                        }
                    //}
                    //else {
                    //    throw new DeploymentException(DeploymentException.CODE_FOREIGN_CUSTOMIZER, "Resource processor for resource '" + name + "' belongs to foreign deployment package");
                   // }
                }
                else {
                    throw new DeploymentException(DeploymentException.CODE_PROCESSOR_NOT_FOUND, "No resource processor for resource: '" + name + "'");
                }
            }
        }
        catch (IOException e) {
            throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Problem while reading stream", e);
        }
    }

}
