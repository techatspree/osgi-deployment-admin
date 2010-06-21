package de.akquinet.gomobile.deploymentadmin.spi;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.deploymentadmin.BundleInfo;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.log.LogService;

import de.akquinet.gomobile.deploymentadmin.AbstractDeploymentPackage;

/**
 * Command that stops all bundles described in the target deployment package of a deployment session.
 *
 * By spec every single bundle of the target package should be stopped, even if this is not strictly necessary
 * because of bundles being unaffected by an update. To be able to skip the stopping of unaffected bundles the
 * following system property can be defined: <code>de.akquinet.gomobile.deploymentadmin.stopunaffectedbundle</code>.
 * If this property has value <code>false</code> (case insensitive) then unaffected bundles will not be stopped,
 * in all other cases the bundles will be stopped according to the OSGi specification.
 */
public class UninstallBundleCommand extends Command {

    public void execute(DeploymentSessionImpl session) throws DeploymentException {
        AbstractDeploymentPackage target = session.getTargetAbstractDeploymentPackage();
        BundleInfo[] bundleInfos = target.getOrderedBundleInfos();
        for (int i = 0; i < bundleInfos.length; i++) {
            if (isCancelled()) {
                throw new DeploymentException(DeploymentException.CODE_CANCELLED);
            }
            String symbolicName = bundleInfos[i].getSymbolicName();
            Bundle bundle = target.getBundle(symbolicName);
            if (bundle != null) {
                addRollback(new InstallBundleRunnable(session, bundle));
                try {
                    //bundle.uninstall();
                    session.uninstallBundle(bundle, target);
                }
                catch (BundleException e) {
                    session.fail();
                    session.getLog().log(LogService.LOG_WARNING, "Could not uninstall bundle '" + bundle.getSymbolicName() + "'", e);
                    //TODO Rollback here ?
                }
            }
            else {
                session.getLog().log(LogService.LOG_WARNING, "Could not uninstall bundle '" + symbolicName + "' because it was not defined int he framework");
            }
        }
    }



    private class InstallBundleRunnable implements Runnable {

        private final Bundle m_bundle;

        private DeploymentSessionImpl m_session;

        public InstallBundleRunnable(DeploymentSessionImpl session,
                Bundle bundle) {
            m_bundle = bundle;
            m_session = session;
        }

        public void run() {
            try {
                // m_session.getBundleContext().installBundle(m_bundle.getLocation());
                m_session.installBundle(m_bundle.getSymbolicName(), (new URL(
                        m_bundle.getLocation())).openStream(), m_session
                        .getTargetAbstractDeploymentPackage());
            } catch (Exception e) {
                // TODO: log this
                e.printStackTrace();
            }
        }

    }
}

