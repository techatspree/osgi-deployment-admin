package de.akquinet.gomobile.deploymentadmin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarInputStream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.PackageAdmin;

import de.akquinet.gomobile.deploymentadmin.spi.CommitResourceCommand;
import de.akquinet.gomobile.deploymentadmin.spi.DeploymentSessionImpl;
import de.akquinet.gomobile.deploymentadmin.spi.DropAllResourcesCommand;
import de.akquinet.gomobile.deploymentadmin.spi.DropBundleCommand;
import de.akquinet.gomobile.deploymentadmin.spi.DropResourceCommand;
import de.akquinet.gomobile.deploymentadmin.spi.GetStorageAreaCommand;
import de.akquinet.gomobile.deploymentadmin.spi.ProcessResourceCommand;
import de.akquinet.gomobile.deploymentadmin.spi.SnapshotCommand;
import de.akquinet.gomobile.deploymentadmin.spi.StartBundleCommand;
import de.akquinet.gomobile.deploymentadmin.spi.StartCustomizerCommand;
import de.akquinet.gomobile.deploymentadmin.spi.StopBundleCommand;
import de.akquinet.gomobile.deploymentadmin.spi.UninstallBundleCommand;
import de.akquinet.gomobile.deploymentadmin.spi.UpdateCommand;

public class DeploymentAdminImpl implements DeploymentAdmin {

    public static final String PACKAGE_DIR = "packages";
    public static final String TEMP_DIR = "temp";
    public static final String PACKAGECONTENTS_DIR = "contents";
    public static final String PACKAGEINDEX_FILE = "index.txt";
    public static final String TEMP_PREFIX = "pkg";
    public static final String TEMP_POSTFIX = "";

    public static final String SHARED_OWNERSHIP_PROP = "de.akquinet.gomobile.deployment.sharedOwnership";
    public static final String STOP_UNAFFECTED_BUNDLE_PROP = "de.akquinet.gomobile.deploymentadmin.stopunaffectedbundle";

    private static final long TIMEOUT = 10000;

    private BundleContext m_context;

    private PackageAdmin m_packageAdmin;
    private LogService m_logService;
    private EventAdmin m_eventAdmin;

    private DeploymentSessionImpl m_session = null;
    private final Map m_packages = new HashMap();
    private final List m_installCommands = new ArrayList();
    private final List m_uninstallCommands = new ArrayList();

    private final boolean m_sharedOwnership; // Enabled by default.

    private final Semaphore m_semaphore = new Semaphore();

    private final Map/*Bundle -> List<DeploymentPackage>*/ m_bundleToPackage = new HashMap();

    /**
     * Create new instance of this <code>DeploymentAdmin</code>.
     * @param bc
     */
    public DeploymentAdminImpl(BundleContext bc) {

        m_context = bc;

        GetStorageAreaCommand getStorageAreaCommand = new GetStorageAreaCommand();
        m_installCommands.add(getStorageAreaCommand);
        m_installCommands.add(new StopBundleCommand());
        m_installCommands.add(new SnapshotCommand(getStorageAreaCommand));
        m_installCommands.add(new UpdateCommand());
        m_installCommands.add(new StartCustomizerCommand());
        CommitResourceCommand commitCommand = new CommitResourceCommand();
        m_installCommands.add(new ProcessResourceCommand(commitCommand));
        m_installCommands.add(new DropResourceCommand(commitCommand));
        m_installCommands.add(new DropBundleCommand());
        m_installCommands.add(commitCommand);
        m_installCommands.add(new StartBundleCommand());

        // Is shared ownership set
        String k = bc.getProperty(SHARED_OWNERSHIP_PROP);
        // If not set - disable
        if (k == null  || (k != null  && k.equalsIgnoreCase("false"))) {
            m_sharedOwnership = false;
        } else {
            m_sharedOwnership = true;
        }

    }

    // called automatically once dependencies are satisfied
    public void start() throws DeploymentException {
        File packageDir = m_context.getDataFile(PACKAGE_DIR);
        if (packageDir == null) {
            throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Could not create directories needed for deployment package persistence");
        } else {
            packageDir.mkdirs();
            File[] packages = packageDir.listFiles();
            for(int i = 0; i < packages.length; i++) {
                if (packages[i].isDirectory()) {
                    try {
                        File index = new File(packages[i], PACKAGEINDEX_FILE);
                        File contents = new File(packages[i], PACKAGECONTENTS_DIR);
                        FileDeploymentPackage dp = new FileDeploymentPackage(index, contents, m_context, this);
                        m_packages.put(dp.getName(), dp);
                    }
                    catch (IOException e) {
                        m_logService.log(LogService.LOG_WARNING, "Could not read deployment package from disk, skipping: '" + packages[i].getAbsolutePath() + "'");
                        continue;
                    }
                }
            }
        }
    }


    public void stop() {
        cancel();
    }

    public boolean cancel() {
        if (m_session != null) {
            m_session.cancel();
            return true;
        }
        return false;
    }

    public DeploymentPackage getDeploymentPackage(String symbName) {
        if (symbName == null) {
            throw new IllegalArgumentException("Symbolic name may not be null");
        }
        return (DeploymentPackage) m_packages.get(symbName);
    }

    public DeploymentPackage getDeploymentPackage(Bundle bundle) {
        if (bundle == null) {
            throw new IllegalArgumentException("Bundle can not be null");
        }
        for (Iterator i = m_packages.values().iterator(); i.hasNext();) {
            DeploymentPackage dp = (DeploymentPackage) i.next();
            if (dp.getBundle(bundle.getSymbolicName()) != null) {
                return dp;
            }
        }
        return null;
    }

    public DeploymentPackage installDeploymentPackage(InputStream input) throws DeploymentException {
        if (input == null) {
            throw new IllegalArgumentException("Inputstream may not be null");
        }
        try {
            if (!m_semaphore.tryAcquire(TIMEOUT)) {
                throw new DeploymentException(DeploymentException.CODE_TIMEOUT, "Timeout exceeded while waiting to install deployment package (" + TIMEOUT + "msec)");
            }
        }
        catch (InterruptedException ie) {
            throw new DeploymentException(DeploymentException.CODE_TIMEOUT, "Thread interrupted");
        }

        JarInputStream jarInput = null;
        File tempPackage = null;
        File tempIndex = null;
        File tempContents = null;

        try {
            try {
                File tempDir = m_context.getDataFile(TEMP_DIR);
                tempDir.mkdirs();
                tempPackage = File.createTempFile(TEMP_PREFIX, TEMP_POSTFIX, tempDir);
                tempPackage.delete();
                tempPackage.mkdirs();
                tempIndex = new File(tempPackage, PACKAGEINDEX_FILE);
                tempContents = new File(tempPackage, PACKAGECONTENTS_DIR);
                tempContents.mkdirs();
                input = new ExplodingOutputtingInputStream(input, tempIndex, tempContents);
            }
            catch (IOException e) {
                m_logService.log(LogService.LOG_ERROR, "Error writing package to disk", e);
                throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Error writing package to disk", e);
            }
            try {
                jarInput = new JarInputStream(input);
            }
            catch (IOException e) {
                m_logService.log(LogService.LOG_ERROR, "Stream does not contain a valid Jar", e);
                throw new DeploymentException(DeploymentException.CODE_NOT_A_JAR, "Stream does not contain a valid Jar", e);
            }
        }
        finally {
            m_semaphore.release();
        }

        StreamDeploymentPackage source = new StreamDeploymentPackage(jarInput, m_context, this);

        boolean succeeded = false;
        AbstractDeploymentPackage target = null;
        try {
            target = (AbstractDeploymentPackage) getDeploymentPackage(source.getName());
            sendInstallEvent(source.getName(), target, source);
            boolean newPackage = (target == null);
            if (newPackage) {
                target = AbstractDeploymentPackage.emptyPackage;
            } else {
            	// Check if the version are the same.
            	if (target.getVersion() != null  && target.getVersion().equals(source.getVersion())) {
                    m_logService.log(LogService.LOG_INFO, "Same package version '" + target.getVersion() + " - Do nothing");
                    return target;
            	}
            }

            if (source.isFixPackage() && ((newPackage) || (!source.getVersionRange().isInRange(target.getVersion())))) {
                succeeded = false;
                m_logService.log(LogService.LOG_ERROR, "Target package version '" + target.getVersion() + "' is not in source range '" + source.getVersionRange() + "'");
                throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Target package version '" + target.getVersion() + "' is not in source range '" + source.getVersionRange() + "'");
            }
            try {
                m_session = new DeploymentSessionImpl(source, target, m_installCommands, this);
                m_session.call();
            }
            catch (DeploymentException de) {
                succeeded = false;
                throw de;
            }
            try {
                jarInput.close();
            }
            catch (IOException e) {
                // nothing we can do
                m_logService.log(LogService.LOG_WARNING, "Could not close stream properly", e);
            }

            File targetContents = m_context.getDataFile(PACKAGE_DIR + File.separator + source.getName() + File.separator + PACKAGECONTENTS_DIR);
            File targetIndex = m_context.getDataFile(PACKAGE_DIR + File.separator + source.getName() + File.separator + PACKAGEINDEX_FILE);
            if (source.isFixPackage()) {
                try {
                    ExplodingOutputtingInputStream.merge(targetIndex, targetContents, tempIndex, tempContents);
                }
                catch (IOException e) {
                    succeeded = false;
                    m_logService.log(LogService.LOG_ERROR, "Could not merge source fix package with target deployment package", e);
                    throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Could not merge source fix package with target deployment package", e);
                }
            } else {
                m_packages.remove(source.getName());  // Remove the source...
                File targetPackage = m_context.getDataFile(PACKAGE_DIR + File.separator + source.getName());
                targetPackage.mkdirs();
                ExplodingOutputtingInputStream.replace(targetPackage, tempPackage);
            }
            FileDeploymentPackage fileDeploymentPackage = null;
            try {
                fileDeploymentPackage = new FileDeploymentPackage(targetIndex, targetContents, m_context, this);
                m_packages.put(source.getName(), fileDeploymentPackage);
            }
            catch (IOException e) {
                succeeded = false;
                m_logService.log(LogService.LOG_ERROR, "Could not create installed deployment package from disk", e);
                throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Could not create installed deployment package from disk", e);
            }
            succeeded = true;
            return fileDeploymentPackage;
        } finally {
            delete(tempPackage);
            sendCompleteEvent(source.getName(), target, source, succeeded);
            m_semaphore.release();
        }
    }

    private void delete(File target) {
        if (target.isDirectory()) {
            File[] childs = target.listFiles();
            for (int i = 0; i < childs.length; i++) {
                delete(childs[i]);
            }
        }
        target.delete();
    }

    public DeploymentPackage[] listDeploymentPackages() {
        Collection packages = m_packages.values();
        return (DeploymentPackage[]) packages.toArray(new DeploymentPackage[packages.size()]);
    }

    /**
     * Returns reference to this bundle's <code>BundleContext</code>
     *
     * @return This bundle's <code>BundleContext</code>
     */
    public BundleContext getBundleContext() {
        return m_context;
    }

    /**
     * Returns reference to the current logging service defined in the framework.
     *
     * @return Currently active <code>LogService</code>.
     */
    public LogService getLog() {
        return m_logService;
    }

    /**
     * Returns reference to the current package admin defined in the framework.
     *
     * @return Currently active <code>PackageAdmin</code>.
     */
    public PackageAdmin getPackageAdmin() {
        return m_packageAdmin;
    }

    private void sendInstallEvent(String name, AbstractDeploymentPackage orig, AbstractDeploymentPackage dest) {
        Dictionary props = new Properties();
        props.put(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NAME, name);
        if (orig != null) {
        	if (orig.getVersion() != Version.emptyVersion) {
        		props.put(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_CURRENTVERSION, orig.getVersion());
        	}
        	if (orig.getDisplayName() != null) {
        		props.put(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_READABLENAME, orig.getDisplayName());
        	}
        }
        if (dest != null) {
        	props.put(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NEXTVERSION, dest.getVersion());
        	if (dest.getDisplayName() != null) {
        		props.put(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_READABLENAME, dest.getDisplayName());
        	}
    	}
        Event event = new Event(Constants.EVENTTOPIC_INSTALL, props);
        m_eventAdmin.postEvent(event);
    }

    private void sendUninstallEvent(String name, AbstractDeploymentPackage orig) {
        Dictionary props = new Properties();
        props.put(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NAME, name);
        if (orig != null) {
        	if (orig.getVersion() != Version.emptyVersion) {
        		props.put(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_CURRENTVERSION, orig.getVersion());
        	}

        	if (orig.getDisplayName() != null) {
        		props.put(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_READABLENAME, orig.getDisplayName());
        	}
        }
        Event event = new Event(Constants.EVENTTOPIC_UNINSTALL, props);
        m_eventAdmin.postEvent(event);
    }

    private void sendCompleteEvent(String name, AbstractDeploymentPackage orig, AbstractDeploymentPackage dest, boolean success) {
        Dictionary props = new Hashtable();
        props.put(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NAME, name);
        if (orig != null) {
        	if (orig.getVersion() != Version.emptyVersion) {
        		props.put(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_CURRENTVERSION, orig.getVersion());
        	}
        	if (orig.getDisplayName() != null) {
        		props.put(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_READABLENAME, orig.getDisplayName());
        	}
        }
        if (dest != null) {
        	props.put(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NEXTVERSION, dest.getVersion());
        	if (dest.getDisplayName() != null) {
        		props.put(DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_READABLENAME, dest.getDisplayName());
        	}
        }
        props.put(Constants.EVENTPROPERTY_SUCCESSFUL, Boolean.valueOf(success));
        Event event = new Event(Constants.EVENTTOPIC_COMPLETE, props);
        m_eventAdmin.postEvent(event);
    }

    public boolean uninstall(AbstractDeploymentPackage dp, boolean forced) throws DeploymentException {

        m_uninstallCommands.clear();
        m_uninstallCommands.add(new StopBundleCommand());
        CommitResourceCommand commitCommand2 = new CommitResourceCommand();
        m_uninstallCommands.add(new DropAllResourcesCommand(commitCommand2, forced));
        m_uninstallCommands.add(new DropResourceCommand(commitCommand2, forced));
        m_uninstallCommands.add(commitCommand2);
        m_uninstallCommands.add(new UninstallBundleCommand());

        try {
            if (!m_semaphore.tryAcquire(TIMEOUT)) {
                throw new DeploymentException(DeploymentException.CODE_TIMEOUT, "Timeout exceeded while waiting to install deployment package (" + TIMEOUT + "msec)");
            }
        }
        catch (InterruptedException ie) {
            throw new DeploymentException(DeploymentException.CODE_TIMEOUT, "Thread interrupted");
        }

        sendUninstallEvent(dp.getName(), dp);

        boolean failed = false;
        try {
            m_session = new DeploymentSessionImpl(AbstractDeploymentPackage.emptyPackage, dp, m_uninstallCommands, this);
            m_session.call();

            m_packages.remove(dp.getName());

            failed = m_session.hasFailed();
        }
        catch (DeploymentException de) {
            getLog().log(LogService.LOG_ERROR, "An exception occured during the " +
                    "uninstallation of a deployement package", de);
            failed = true;
        } finally {
            m_semaphore.release();
            sendCompleteEvent(dp.getName(), dp, null, ! failed);
        }

        return ! failed; // Returns true if successful.


    }

    private String getBundleKey(Bundle bundle) {
    	return bundle.getSymbolicName();
    }

    public Bundle installBundle(String sn, InputStream is, AbstractDeploymentPackage dp) throws BundleException {
        Bundle bundle = m_context.installBundle(Constants.BUNDLE_LOCATION_PREFIX + sn, is);

        if (m_sharedOwnership ) {
            System.out.println("Installing " + bundle.getSymbolicName() + " from " + dp.getName() + " - " + m_bundleToPackage.containsKey(getBundleKey(bundle)));

            if (m_bundleToPackage.containsKey(getBundleKey(bundle))) {
                List l = (List) m_bundleToPackage.get(getBundleKey(bundle));
                // Already installed ?
                if (! l.contains(dp.getName())) {
                    l.add(dp.getName());
                    System.out.println("Bundle " + bundle.getSymbolicName() + " -> " + l);
                }
            } else {
                List l = new ArrayList();
                l.add(dp.getName());
                m_bundleToPackage.put(getBundleKey(bundle), l);
                //TODO DEBUG
                System.out.println("Bundle " + bundle.getSymbolicName() + " -> " + dp.getName());
            }
        }
        return bundle;
    }

    public void uninstallBundle(Bundle bundle, AbstractDeploymentPackage dp) throws BundleException {
        if (!m_sharedOwnership) {
            bundle.uninstall();
            return;
        }

        if (m_bundleToPackage.containsKey(getBundleKey(bundle))) {
            List l = (List) m_bundleToPackage.get(getBundleKey(bundle));
            if (l.contains(dp.getName())) {
                if (l.size() == 1) {
                    // Drop bundle, Clear list
                    l.clear();
                    m_bundleToPackage.remove(getBundleKey(bundle));
                    bundle.uninstall();
                } else {
                	l.remove(dp.getName());
                    System.out.println("Bundle " + bundle.getSymbolicName() + " -> " + l);
                }
            } else {
                // Oups, bad dp ?
                System.err.println("Cannot uninstall the bundle " + bundle.getSymbolicName() + " from " + dp.getName() + ": " +
                        " bundle not owned by this package");
            }
        } else {
            // Weird case, we're trying to uninstall an not installed bundles
            //TODO LOG HERE
            System.err.println("Cannot uninstall a not installed bundle : " + bundle.getSymbolicName());
        }
    }

	public Bundle updateBundle(Bundle bundle, InputStream is,
			AbstractDeploymentPackage dp) throws BundleException {
		bundle.update(is);
		if (m_sharedOwnership) {
			System.out.println("Updating " + bundle.getSymbolicName()
					+ " from " + dp.getName() + " - "
					+ m_bundleToPackage.containsKey(getBundleKey(bundle)));

			if (m_bundleToPackage.containsKey(getBundleKey(bundle))) {
				List l = (List) m_bundleToPackage.get(getBundleKey(bundle));
				// Already installed ?
				if (!l.contains(dp.getName())) {
					l.add(dp.getName());
					System.out.println("Bundle " + bundle.getSymbolicName()
							+ " -> " + l);
				}
			} else {
				List l = new ArrayList();
				l.add(dp.getName());
				m_bundleToPackage.put(getBundleKey(bundle), l);
				// TODO DEBUG
				System.out.println("Bundle " + bundle.getSymbolicName()
						+ " -> " + dp.getName());
			}
		}
		return bundle;
	}

	public void addOwnership(Bundle bundle, AbstractDeploymentPackage dp) {
		if (m_sharedOwnership) {
			System.out.println("Adding " + bundle.getSymbolicName()
					+ " from " + dp.getName() + " - "
					+ m_bundleToPackage.containsKey(getBundleKey(bundle)));

			if (m_bundleToPackage.containsKey(getBundleKey(bundle))) {
				List l = (List) m_bundleToPackage.get(getBundleKey(bundle));
				// Already installed ?
				if (!l.contains(dp.getName())) {
					l.add(dp.getName());
					System.out.println("Bundle " + bundle.getSymbolicName()
							+ " -> " + l);
				}
			} else {
				List l = new ArrayList();
				l.add(dp.getName());
				m_bundleToPackage.put(getBundleKey(bundle), l);
				// TODO DEBUG
				System.out.println("Bundle " + bundle.getSymbolicName()
						+ " -> " + dp.getName());
			}
		}
	}




}
