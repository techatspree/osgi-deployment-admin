package de.akquinet.gomobile.deploymentadmin.spi;

import java.io.IOException;
import java.io.InputStream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.log.LogService;

import de.akquinet.gomobile.deploymentadmin.AbstractDeploymentPackage;
import de.akquinet.gomobile.deploymentadmin.BundleInfoImpl;

/**
 * Command that uninstalls bundles, if rolled back the bundles are restored.
 */
public class DropBundleCommand extends Command {

    public void execute(DeploymentSessionImpl session) throws DeploymentException {
        AbstractDeploymentPackage target = session.getTargetAbstractDeploymentPackage();
        AbstractDeploymentPackage source = session.getSourceAbstractDeploymentPackage();
        LogService log = session.getLog();

        BundleInfoImpl[] orderedTargetBundles = target.getOrderedBundleInfos();
        for (int i = orderedTargetBundles.length - 1; i >= 0; i--) {
            BundleInfoImpl bundleInfo = orderedTargetBundles[i];
            if (!bundleInfo.isCustomizer() && source.getBundleInfoByName(bundleInfo.getSymbolicName()) == null) {
                // stale bundle, save a copy for rolling back and uninstall it
                String symbolicName = bundleInfo.getSymbolicName();
                try {
                    Bundle bundle = target.getBundle(symbolicName);
                    //bundle.update();
                    session.uninstallBundle(bundle, target);
                    addRollback(new InstallBundleRunnable(session, bundle, target.getBundleStream(symbolicName), log, target));
                }
                catch (BundleException be) {
                    log.log(LogService.LOG_WARNING, "Bundle '" + symbolicName + "' could not be uninstalled", be);
                }
                catch (IOException e) {
                    log.log(LogService.LOG_WARNING, "Could not get bundle data stream for bundle '" + symbolicName + "'", e);
                    throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Could not prepare rollback for uninstalling bundle '" + symbolicName + "'");
                }
            }
        }
    }

    private static class InstallBundleRunnable implements Runnable {

        private final InputStream m_bundleStream;
        private final Bundle m_bundle;
        private final LogService m_log;
        private final DeploymentSessionImpl m_session;
        private final AbstractDeploymentPackage m_package;

        public InstallBundleRunnable(DeploymentSessionImpl session, Bundle bundle, InputStream bundleStream, LogService log, AbstractDeploymentPackage pack) {
            m_bundle = bundle;
            m_bundleStream = bundleStream;
            m_log = log;
            m_session = session;
            m_package = pack;
        }

        public void run() {
            try {
            	m_session.updateBundle(m_bundle, m_bundleStream, m_package);
                //m_bundle.update(m_bundleStream);
            }
            catch (BundleException e) {
                m_log.log(LogService.LOG_WARNING, "Could not rollback uninstallation of bundle '" + m_bundle.getSymbolicName() + "'", e);
            }
        }
    }
}
