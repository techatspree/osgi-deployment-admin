package de.akquinet.gomobile.deployment.api;

/**
 * Copy of org.apache.felix.deploymentadmin.Constants of org.apache.felix.deploymentadmin.
 */
public interface Constants extends org.osgi.framework.Constants
{

    // manifest main attribute header constants
    public static final String DEPLOYMENTPACKAGE_SYMBOLICMAME = "DeploymentPackage-SymbolicName";
    public static final String DEPLOYMENTPACKAGE_VERSION = "DeploymentPackage-Version";
    public static final String DEPLOYMENTPACKAGE_FIXPACK = "DeploymentPackage-FixPack";

    // Others headers
    public static final String DEPLOYMENTPACKAGE_NAME = "DeploymentPackage-Name";
    public static final String DEPLOYMENTPACKAGE_ICON = "DeploymentPackage-Icon";
    public static final String DEPLOYMENTPACKAGE_REQUIREDSTORAGE = "DeploymentPackage-RequiredStorage";
    public static final String DEPLOYMENTPACKAGE_COPYRIGHT = "DeploymentPackage-Copyright";
    public static final String DEPLOYMENTPACKAGE_CONTACTADDRESS = "DeploymentPackage-ContactAddress";
    public static final String DEPLOYMENTPACKAGE_DESCRIPTION = "DeploymentPackage-Description";
    public static final String DEPLOYMENTPACKAGE_DOCURL = "DeploymentPackage-DocURL";
    public static final String DEPLOYMENTPACKAGE_VENDOR = "DeploymentPackage-Vendor";
    public static final String DEPLOYMENTPACKAGE_LICENSE = "DeploymentPackage-License";


    // manifest 'name' section header constants
    public static final String RESOURCE_PROCESSOR = "Resource-Processor";
    public static final String DEPLOYMENTPACKAGE_MISSING = "DeploymentPackage-Missing";
    public static final String DEPLOYMENTPACKAGE_CUSTOMIZER = "DeploymentPackage-Customizer";

    // event topics and properties
    public static final String EVENTTOPIC_INSTALL = "org/osgi/service/deployment/INSTALL";
    public static final String EVENTTOPIC_UNINSTALL = "org/osgi/service/deployment/UNINSTALL";
    public static final String EVENTTOPIC_COMPLETE = "org/osgi/service/deployment/COMPLETE";
    public static final String EVENTPROPERTY_DEPLOYMENTPACKAGE_NAME = "deploymentpackage.name";
    public static final String EVENTPROPERTY_SUCCESSFUL = "successful";

    // miscellaneous constants
    public static final String BUNDLE_LOCATION_PREFIX = "osgi-dp:";
    public static final String SHA_ATTRIBUTE = "SHA1-Digest";
}
