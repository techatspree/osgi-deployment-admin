package de.akquinet.gomobile.deploymentadmin.spi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.deploymentadmin.spi.ResourceProcessor;
import org.osgi.service.deploymentadmin.spi.ResourceProcessorException;
import org.osgi.service.log.LogService;

import de.akquinet.gomobile.deploymentadmin.AbstractDeploymentPackage;
import de.akquinet.gomobile.deploymentadmin.ResourceInfoImpl;

/**
 * Command that drops all resources.
 */
public class DropAllResourcesCommand extends Command {

    private final CommitResourceCommand m_commitCommand;
    private final boolean m_forcedMode;

    /**
     * Creates an instance of this command. The commit command is used to make sure
     * the resource processors used to drop resources will be committed at a later stage in the process.
     *
     * @param commitCommand The commit command that will be executed at a later stage in the process.
     */
    public DropAllResourcesCommand(CommitResourceCommand commitCommand, boolean forced) {
        m_commitCommand = commitCommand;
        m_forcedMode = forced;
        addRollback(m_commitCommand);
    }

    public void execute(DeploymentSessionImpl session) {
        AbstractDeploymentPackage target = session
                .getTargetAbstractDeploymentPackage();
        BundleContext context = session.getBundleContext();
        LogService log = session.getLog();

        // TODO for the count support, checks that the bundle is not used by
        // anybody else.

        ResourceInfoImpl[] orderedTargetResources = target
                .getOrderedResourceInfos();
        for (int i = orderedTargetResources.length - 1; i >= 0; i--) {

            String path = orderedTargetResources[i].getPath();
            String sn = orderedTargetResources[i].getResourceProcessor();
            ServiceReference ref = target.getResourceProcessor(path);

            if (ref != null
                    && target
                            .isResourceProcessorProvidedByTheDeploymentPackage(ref)) {
                // Must call droppedAllResources
                ResourceProcessor rp = (ResourceProcessor) context
                        .getService(ref);
                if (rp == null) {
                    session.fail();
                    log.log(LogService.LOG_ERROR, "Resource Processor '" + sn
                            + "' not available during uninstallation ");
                    if (!m_forcedMode) {
                        // Rollback
                        m_commitCommand.rollback();
                    }
                } else {
                    try {
                        if (m_commitCommand.addResourceProcessor(rp)) {
                            rp.begin(session);
                        }

                        rp.dropAllResources();
                    } catch (ResourceProcessorException e) {
                        session.fail();
                        log.log(LogService.LOG_ERROR,
                                "The Resource Processor '" + sn
                                        + "'  has thrown an exception during"
                                        + " the resource processing : "
                                        + e.getMessage());
                        if (!m_forcedMode) {
                            // Rollback
                            m_commitCommand.rollback();
                        }
                    }
                }

            }

        }
    }
}
