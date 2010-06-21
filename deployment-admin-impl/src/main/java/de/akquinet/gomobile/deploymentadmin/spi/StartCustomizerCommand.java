package de.akquinet.gomobile.deploymentadmin.spi;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.deploymentadmin.DeploymentException;

import de.akquinet.gomobile.deploymentadmin.AbstractDeploymentPackage;
import de.akquinet.gomobile.deploymentadmin.BundleInfoImpl;

/**
 * Command that starts all customizer bundles defined in the source deployment packages of a deployment
 * session. In addition all customizer bundles of the target deployment package that are not present in the source
 * deployment package are started as well.
 */
public class StartCustomizerCommand extends Command {

    public void execute(DeploymentSessionImpl session) throws DeploymentException {
        AbstractDeploymentPackage target = session.getTargetAbstractDeploymentPackage();
        AbstractDeploymentPackage source = session.getSourceAbstractDeploymentPackage();
        Set bundles = new HashSet();
        Set sourceBundlePaths = new HashSet();
        BundleInfoImpl[] targetInfos = target.getBundleInfoImpls();
        BundleInfoImpl[] sourceInfos = source.getBundleInfoImpls();
        for(int i = 0; i < sourceInfos.length; i++) {
            if (sourceInfos[i].isCustomizer()) {
                sourceBundlePaths.add(sourceInfos[i].getPath());
                Bundle bundle = source.getBundle(sourceInfos[i].getSymbolicName());
                if (bundle != null) {
                    bundles.add(bundle);
                }
            }
        }
        for(int i = 0; i < targetInfos.length; i++) {
            if (targetInfos[i].isCustomizer() && !sourceBundlePaths.contains(targetInfos[i].getPath())) {
                Bundle bundle = target.getBundle(targetInfos[i].getSymbolicName());
                if (bundle != null) {
                    bundles.add(bundle);
                }
            }
        }
        for(Iterator i = bundles.iterator(); i.hasNext(); ) {
            Bundle bundle = (Bundle) i.next();
            try {
                bundle.start();
            }
            catch (BundleException be) {
                throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Could not start customizer bundle '" + bundle.getSymbolicName() + "'", be);
            }
            addRollback(new StopCustomizerRunnable(bundle));
        }
    }

    private static class StopCustomizerRunnable implements Runnable {

        private final Bundle m_bundle;

        public StopCustomizerRunnable(Bundle bundle) {
            m_bundle = bundle;
        }

        public void run() {
            try {
                m_bundle.stop();
            }
            catch (BundleException e) {
                // TODO log this
                e.printStackTrace();
            }
        }

    }
}
