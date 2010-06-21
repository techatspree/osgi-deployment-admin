package de.akquinet.gomobile.deploymentadmin.spi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.spi.ResourceProcessor;
import org.osgi.service.deploymentadmin.spi.ResourceProcessorException;
import org.osgi.service.log.LogService;

/**
 * Command that commits all the resource processors that were added to the command.
 */
public class CommitResourceCommand extends Command implements Runnable {

    private final List m_processors = new ArrayList();

    public void execute(DeploymentSessionImpl session) throws DeploymentException {
        for (ListIterator i = m_processors.listIterator(m_processors.size()); i.hasPrevious();) {
    		ResourceProcessor processor = (ResourceProcessor) i.previous();
            try {
                processor.prepare();
            }
            catch (ResourceProcessorException e) {
                session.getLog().log(LogService.LOG_ERROR, "Preparing commit for resource processor failed", e);
                throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Preparing commit for resource processor failed", e);
            }
        }
        for (ListIterator i = m_processors.listIterator(m_processors.size()); i.hasPrevious();) {
            ResourceProcessor processor = (ResourceProcessor) i.previous();
            try {
                processor.commit();
            }
            catch (Exception e) {
                session.getLog().log(LogService.LOG_ERROR, "Committing resource processor '" + processor + "' failed", e);
                // TODO Throw exception?
            }
        }
        m_processors.clear();
    }

    public void rollback() {
        for (ListIterator i = m_processors.listIterator(m_processors.size()); i.hasPrevious();) {
            ResourceProcessor processor = (ResourceProcessor) i.previous();
            try {
                processor.rollback();
            }
            catch (Exception e) {
                // TODO Log this?
            }
            i.remove();
        }
    }

    /**
     * Add a resource processor, all resource processors that are added will be committed when the command is executed.
     *
     * @param processor The resource processor to add.
     * @return true if the resource processor was added, false if it was already added.
     */
    public boolean addResourceProcessor(ResourceProcessor processor) {
        for (Iterator i = m_processors.iterator(); i.hasNext();) {
            ResourceProcessor proc = (ResourceProcessor) i.next();
            if (proc == processor) {
                return false;
            }
        }
        m_processors.add(processor);
        return true;
    }

    public void run() {
        rollback();
    }

}
