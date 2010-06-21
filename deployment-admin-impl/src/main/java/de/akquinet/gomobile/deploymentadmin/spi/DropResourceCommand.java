package de.akquinet.gomobile.deploymentadmin.spi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.deploymentadmin.spi.ResourceProcessor;
import org.osgi.service.deploymentadmin.spi.ResourceProcessorException;
import org.osgi.service.log.LogService;

import de.akquinet.gomobile.deploymentadmin.AbstractDeploymentPackage;
import de.akquinet.gomobile.deploymentadmin.ResourceInfoImpl;

/**
 * Command that drops resources.
 */
public class DropResourceCommand extends Command {

    private final CommitResourceCommand m_commitCommand;

    private final boolean m_forcedMode;


    /**
     * Creates an instance of this command. The commit command is used to make sure
     * the resource processors used to drop resources will be committed at a later stage in the process.
     *
     * @param commitCommand The commit command that will be executed at a later stage in the process.
     * @param forced enable/disable the force mode
     */
    public DropResourceCommand(CommitResourceCommand commitCommand, boolean forced) {
        m_commitCommand = commitCommand;
        m_forcedMode = forced;
        addRollback(m_commitCommand);
    }

    /**
     * Creates an instance of this command. The commit command is used to make sure
     * the resource processors used to drop resources will be committed at a later stage in the process.
     *
     * @param commitCommand The commit command that will be executed at a later stage in the process.
     */
    public DropResourceCommand(CommitResourceCommand commitCommand) {
        m_commitCommand = commitCommand;
        m_forcedMode = false;
        addRollback(m_commitCommand);
    }

    public void execute(DeploymentSessionImpl session) {
        AbstractDeploymentPackage target = session.getTargetAbstractDeploymentPackage();
        AbstractDeploymentPackage source = session.getSourceAbstractDeploymentPackage();
        BundleContext context = session.getBundleContext();
        LogService log = session.getLog();

        ResourceInfoImpl[] orderedTargetResources = target.getOrderedResourceInfos();
        for (int i = orderedTargetResources.length - 1; i >= 0; i--) {
            ResourceInfoImpl resourceInfo = orderedTargetResources[i];
            String path = resourceInfo.getPath();
            String sn = resourceInfo.getResourceProcessor();
            if (source.getResourceInfoByPath(path) == null) {
                ServiceReference ref = target.getResourceProcessor(path);
                if (ref != null) {
                    ResourceProcessor resourceProcessor = (ResourceProcessor) context.getService(ref);
                    if (resourceProcessor != null) {
                        try {
                            if (m_commitCommand.addResourceProcessor(resourceProcessor)) {
                                resourceProcessor.begin(session);
                            }
                            resourceProcessor.dropped(path);
                        }
                        catch (ResourceProcessorException e) {
                            session.fail();
                            log
                                    .log(
                                            LogService.LOG_ERROR,
                                            "The Resource Processor '"
                                                    + sn + "'  has thrown an exception during"
                                                    + " the resource processing : "
                                                    + e.getMessage());
                            if (!m_forcedMode) {
                                // Rollback
                                m_commitCommand.rollback();
                            }
                        }
                    }
                } else {
                    //TODO DEBUG
                    System.out.println(sn + " not available");
                    // Check if it was a customizer provided by the current DP
                    // In that case, the unavailability was expected

                    session.fail();
                    log
                            .log(
                                    LogService.LOG_ERROR,
                                    "The Resource Processor '"
                                            + sn + "'  is not available");
                    if (!m_forcedMode) {
                        // Rollback
                        m_commitCommand.rollback();
                    }
                }
            }
        }
    }
}
