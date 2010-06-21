package de.akquinet.gomobile.deployment.mojo;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import de.akquinet.gomobile.deployment.api.DeploymentPackage;

/**
 * <p>
 * This class represents a Deployment-Package and stores some information about
 * it.
 * </p>
 * <p>
 * The resources if a Deployment-Package are categorized in three groups:
 * <ul>
 * <li>Bundle-Resources</li>
 * <li>Localization-Resources (not supported)</li>
 * <li>Processed-Resources</li>
 * </ul>
 * </p>
 */
public class DeploymentPackageMetadata {

    private DeploymentPackage m_package;

    /**
     * The resources.
     */
    private List<Resource> m_resources = null;

    /**
     * A constructor which initializes an instance of a
     * {@link DeploymentPackageMetadata}.
     */
    public DeploymentPackageMetadata() {
        m_resources = new ArrayList<Resource>();
        m_package = new DeploymentPackage();
    }

    /**
     * @return the description
     */
    public final String getDescription() {
        return m_package.getDescription();
    }

    /**
     * @param desc the description to set
     */
    public final void setDescription(final String desc) {
        m_package.setDescription(desc);
    }

    /**
     * @return the name
     */
    public final String getName() {
        return m_package.getName();
    }

    /**
     * @param name the name to set
     */
    public final void setName(final String name) {
        m_package.setName(name);
    }

    /**
     * @return the version
     */
    public final String getVersion() {
        return m_package.getVersion();
    }

    /**
     * @param version the version to set
     */
    public final void setVersion(final String version) {
        m_package.setVersion(version);
    }

    /**
     * @return the required space
     */
    public final long getRequiredStorage() {
        return m_package.getRequiredStorage();
    }

    /**
     * @param desc the description to set
     */
    public final void setRequiredStorage(final long space) {
        m_package.setRequiredStorage(space);
    }

    /**
     * @return the license
     */
    public final String getLicense() {
        return m_package.getLicense();
    }

    /**
     * @param license the license to set
     */
    public final void setLicense(final String license) {
        m_package.setLicense(license);
    }

    /**
     * @return the copyright
     */
    public final String getCopyright() {
        return m_package.getCopyright();
    }

    /**
     * @param cr the copyright to set
     */
    public final void setCopyright(final String cr) {
        m_package.setCopyright(cr);
    }

    /**
     * @return the contactAddress
     */
    public final String getContactAddress() {
        return m_package.getAddress();
    }

    /**
     * @param ad the contactAddress to set
     */
    public final void setContactAddress(final String ad) {
        m_package.setContactAddress(ad);
    }

    /**
     * @return the docURL
     */
    public final String getDocURL() {
        if (m_package.getDocURL() != null) {
            return m_package.getDocURL().toExternalForm();
        } else {
            return null;
        }
    }

    /**
     * @param url the docURL to set
     * @throws MalformedURLException
     */
    public final void setDocURL(final String url) throws MalformedURLException {
        m_package.setDocURL(new URL(url));
    }

    /**
     * @param url the icon url to set
     * @throws MalformedURLException
     */
    public final void setIcon(final String url) throws MalformedURLException {
        m_package.setIcon(new URL(url));
    }

    /**
     * @return the icon
     */
    public final String getIcon() {
        if (m_package.getIcon() != null) {
            return m_package.getIcon().toExternalForm();
        } else {
            return null;
        }
    }


    /**
     * @return the vendor
     */
    public final String getVendor() {
        return m_package.getVendor();
    }

    /**
     * @param vendor the vendor to set
     */
    public final void setVendor(final String vendor) {
        m_package.setVendor(vendor);
    }

    /**
     * @return the fixPack
     */
    public final String getFixPack() {
        return m_package.getFixPack();
    }

    /**
     * @param fixPack the fixPack to set
     */
    public final void setFixPack(final String fixPack) {
        m_package.setFixPackage(fixPack);
    }

    /**
     * @return the resources
     */
    public final List<Resource> getResources() {
        return m_resources;
    }

    /**
     * @param resources the resources to set
     */
    public final void setResources(final List<Resource> resources) {
        // Not processed yet...
        m_resources = resources;
    }

    /**
     * @return the symbolicName
     */
    public final String getSymbolicName() {
        return m_package.getSymbolicName();
    }

    /**
     * @param sn the symbolicName to set
     */
    public final void setSymbolicName(final String sn) {
        // Compute singleton:=
        String symb = sn.trim();
        int index = symb.indexOf(";singleton:=");
        if (index != -1) {
            m_package.setSymbolicName(symb.substring(0, index));
        } else {
            m_package.setSymbolicName(symb);
        }
    }

    /**
     * Resources can be Bundle-, or Processed-Resources. This method filters the
     * whole resource list and returns only Bundle-Resources.
     * @return A filtered resource list where only Bundle-Resources are
     *         contained.
     */
    public final List<BundleResource> getBundleResources() {
        final List<BundleResource> list = new ArrayList<BundleResource>();
        for (final Resource resource : m_resources) {
            if (resource instanceof BundleResource) {
                list.add((BundleResource) resource);
            }
        }
        return list;
    }

    /**
     * Resources can be Bundle-, or Processed-Resources. This method filters the
     * whole resource list and returns only Processed-Resources.
     * @return A filtered resource list where only Processed-Resources are
     *         contained.
     */
    public final List<ProcessedResource> getProcessedResources() {
        final List<ProcessedResource> list = new ArrayList<ProcessedResource>();
        for (final Resource resource : m_resources) {
            if (resource instanceof ProcessedResource) {
                list.add((ProcessedResource) resource);
            }
        }

        return list;
    }

    public DeploymentPackage getDeploymentPackage() {
        return m_package;
    }
}
