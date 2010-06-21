package de.akquinet.gomobile.deploymentadmin.spi;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.service.deploymentadmin.DeploymentException;

import de.akquinet.gomobile.deploymentadmin.AbstractDeploymentPackage;
import de.akquinet.gomobile.deploymentadmin.AbstractInfo;
import de.akquinet.gomobile.deploymentadmin.BundleInfoImpl;
import de.akquinet.gomobile.deploymentadmin.Constants;

/**
 * Command that installs all bundles described in the source deployment package of a deployment
 * session. If a bundle was already defined in the target deployment package of the same session
 * it is updated, otherwise the bundle is simply installed.
 */
public class UpdateCommand extends Command {

    public void execute(DeploymentSessionImpl session) throws DeploymentException {
        AbstractDeploymentPackage source = session.getSourceAbstractDeploymentPackage();
        AbstractDeploymentPackage targetPackage = session.getTargetAbstractDeploymentPackage();

        Map expectedBundles = new HashMap();
        AbstractInfo[] bundleInfos = (AbstractInfo[]) source.getBundleInfos();
        for (int i = 0; i < bundleInfos.length; i++) {
            AbstractInfo bundleInfo = bundleInfos[i];
            if(!bundleInfo.isMissing()) {
                expectedBundles.put(bundleInfo.getPath(), bundleInfo);
            }
        }

        try {
            while (!expectedBundles.isEmpty()) {
                AbstractInfo entry = source.getNextEntry();
                if (entry == null) {
                    throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Expected more bundles in the stream: " + expectedBundles.keySet());
                }

                String name = entry.getPath();
                BundleInfoImpl bundleInfo = (BundleInfoImpl) expectedBundles.remove(name);
                if (bundleInfo == null) {
                    throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Resource '" + name + "' is not described in the manifest.");
                }

                Bundle bundle = source.getBundle(bundleInfo.getSymbolicName());
                //Bundle bundle = targetPackage.getBundle(bundleInfo.getSymbolicName());
                try {
                    if (bundle == null) {
                        // new bundle, install it
                        bundle = session.installBundle(bundleInfo.getSymbolicName(), new BundleInputStream(source.getCurrentEntryStream()), session.getSourceAbstractDeploymentPackage());
                        //bundle = context.installBundle(Constants.BUNDLE_LOCATION_PREFIX + bundleInfo.getSymbolicName(), new BundleInputStream(source.getCurrentEntryStream()));
                        addRollback(new UninstallBundleRunnable(bundle, session, source));
                    } else {
                        // existing bundle, update it
                        Version sourceVersion = bundleInfo.getVersion();
                        Version targetVersion = Version.parseVersion((String) bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION));
                        if (!sourceVersion.equals(targetVersion)) {
                            bundle = session.updateBundle(bundle, new BundleInputStream(source.getCurrentEntryStream()), session.getSourceAbstractDeploymentPackage());
                            addRollback(new UpdateBundleRunnable(session, bundle, targetPackage, bundleInfo.getSymbolicName()));
                        } else {
                        	session.addOwnership(bundle, session.getSourceAbstractDeploymentPackage());
                        }
                    }
                }
                catch (BundleException be) {
                    if (isCancelled()) {
                        return;
                    }
                    throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Could not install new bundle '" + name + "'", be);
                }
                if (!bundle.getSymbolicName().equals(bundleInfo.getSymbolicName()) || !Version.parseVersion((String)bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION)).equals(bundleInfo.getVersion())) {
                    throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Installed/updated bundle version and/or symbolicnames do not match what was installed/updated");
                }
            }
        }
        catch (IOException e) {
            throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Problem while reading stream", e);
        }
    }

    private static class UninstallBundleRunnable implements Runnable {

        private final Bundle m_bundle;

        private final DeploymentSessionImpl m_session;

        private final AbstractDeploymentPackage m_package;

        public UninstallBundleRunnable(Bundle bundle, DeploymentSessionImpl session, AbstractDeploymentPackage dp) {
            m_bundle = bundle;
            m_session = session;
            m_package = dp;
        }

        public void run() {

            try {
                //m_bundle.uninstall();
                m_session.uninstallBundle(m_bundle, m_package);
            }
            catch (BundleException e) {
                // TODO: log this
                e.printStackTrace();
            }
        }
    }

    private static class UpdateBundleRunnable implements Runnable {

        private final Bundle m_bundle;
        private final AbstractDeploymentPackage m_targetPackage;
        private final String m_symbolicName;
        private final DeploymentSessionImpl m_session;

        public UpdateBundleRunnable(DeploymentSessionImpl session, Bundle bundle, AbstractDeploymentPackage targetPackage, String symbolicName) {
            m_bundle = bundle;
            m_targetPackage = targetPackage;
            m_symbolicName = symbolicName;
            m_session = session;
        }

        public void run() {
            try {
            	m_session.updateBundle(m_bundle, m_targetPackage.getBundleStream(m_symbolicName), m_targetPackage);
                //m_bundle.update(m_targetPackage.getBundleStream(m_symbolicName));
            }
            catch (Exception e) {
                // TODO: log this
                e.printStackTrace();
            }
        }
    }

    private final class BundleInputStream extends InputStream {
        private final InputStream m_inputStream;

        private BundleInputStream(InputStream jarInputStream) {
            m_inputStream = jarInputStream;
        }

        public int read() throws IOException {
            checkCancel();
            return m_inputStream.read();
        }

        public int read(byte[] buffer) throws IOException {
            checkCancel();
            return m_inputStream.read(buffer);
        }

        public int read(byte[] buffer, int off, int len) throws IOException {
            checkCancel();
            return m_inputStream.read(buffer, off, len);
        }

        private void checkCancel() throws IOException {
            if (isCancelled()) {
                throw new IOException("Stream was cancelled");
            }
        }
    }

}
