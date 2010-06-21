package de.akquinet.gomobile.deployment.mojo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.License;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

import de.akquinet.gomobile.deployment.api.CheckingException;
import de.akquinet.gomobile.deployment.api.DeploymentPackage;

/**
 * Create an OSGi deployment package from Maven project.
 * @goal package
 * @phase package
 * @requiresDependencyResolution runtime
 * @description build an OSGi deployment package jar
 */
public class DeploymentPackageMojo extends AbstractMojo {

    private static final String DP_FILE_EXTENSION = ".dp";

    private static final String PLUGIN_NAME = "de.akquinet.gomobile:maven-dp-plugin - 0.9.0-SNAPSHOT";

    /**
     * The directory for the generated bundles.
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private File m_ouputDirectory;

    /**
     * The directory for the pom.
     * @parameter expression="${basedir}"
     * @required
     */
    private File m_basedir;

    /**
     * The infos for the deployment-package.
     * @parameter property="deploymentPackage"
     * @required
     */
    private DeploymentPackageMetadata m_deploymentPackageInfo = null;

    /**
     * Directory where the manifest will be written.
     * @parameter property="manifestLocation" expression="${manifestLocation}"
     *            default-value="${project.build.outputDirectory}/META-INF"
     */
    private File m_manifestLocation;

    /**
     * The Maven project.
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject m_project;

    /**
     * The directory for the generated JAR.
     * @parameter property="buildDirectory"
     *            expression="${project.build.directory}"
     * @required
     */
    private String m_buildDirectory;

    /**
     * Project types which this plugin supports.
     * @parameter alias="supportedProjectTypes"
     */
    private List<String> m_supportedProjectTypes = Arrays
            .asList(new String[] {"deployment-package"});

    /**
     * The local repository used to resolve artifacts.
     * @parameter property="localRepository" expression="${localRepository}"
     * @required
     */
    private ArtifactRepository m_localRepository;

    /**
     * The remote repositories used to resolve artifacts.
     * @parameter property="remoteRepositories"
     *            expression="${project.remoteArtifactRepositories}"
     * @required
     */
    private List<ArtifactRepository> m_remoteRepositories;

    /**
     * Flag that indicates if the manifest of the resulting deployment-package
     * should contain extra data like "Created-By, Creation-Date, ...".
     * @parameter property="writeExtraData"
     */
    private boolean m_writeExtraData = true;

    /**
     * @component
     */
    private ArtifactFactory m_artifactFactory;

    /**
     * @component
     */
    private ArtifactResolver m_artifactresolver;

    /**
     * @component
     */
    private ArtifactHandlerManager m_artifactHandlerManager;

    /**
     * @component
     */
    private ArchiverManager m_archiverManager;

    /**
     * This method will be called by the Maven framework in order to execute
     * this plugin.
     * @throws MojoExecutionException id any error occures
     * @throws MojoFailureException id any error occures
     */
    public final void execute() throws MojoExecutionException,
            MojoFailureException {

        // First, check if we support such type of packaging.
        final Artifact artifact = getProject().getArtifact();
        if (!getSupportedProjectTypes().contains(artifact.getType())) {
            getLogger()
                    .debug(
                            "Ignoring project "
                                    + artifact
                                    + " : type "
                                    + artifact.getType()
                                    + " is not supported by bundle plugin, supported types are "
                                    + getSupportedProjectTypes());
            return;
        }

        // Create a description from the pom metadata.
        DeploymentPackageMetadata deploymentPackageInfo = getDeploymentPackageInfo();
        if (deploymentPackageInfo == null) {
            throw new MojoExecutionException("No deployment package described");
        }

        DeploymentPackage currentPackage = deploymentPackageInfo
                .getDeploymentPackage();

        // Populate...
        try {
            populate(currentPackage);
        } catch (IOException e1) {
            throw new MojoExecutionException("Cannot analyze the artifact : "
                    + e1.getMessage());
        }

        // Resolve all resources.
        for (BundleResource br : deploymentPackageInfo.getBundleResources()) {
            br.setMojo(this);
            br.resolve(currentPackage);
        }

        for (ProcessedResource pr : deploymentPackageInfo
                .getProcessedResources()) {
            pr.resolve(currentPackage);
        }

        // Check...
        try {
            currentPackage.check();
        } catch (CheckingException e) {
            throw new MojoExecutionException(
                    "The deployment package is inconsistent : "
                            + e.getMessage());
        }

        // Now, handle file creation...
        String finalName = getProject().getBuild().getFinalName()
                + DP_FILE_EXTENSION;
        final File file = new File(getBuildDirectory(), finalName);

        file.getParentFile().mkdirs();

//        // workaround for MNG-1682: force maven to install artifact using the
//        // "jar" handler
        final Artifact mainArtifact = getProject().getArtifact();
//        mainArtifact.setArtifactHandler(getArtifactHandlerManager()
//                .getArtifactHandler("jar"));
        mainArtifact.setFile(file);

        // Build...
        try {
            getLogger().debug("Build the deployment package");
            currentPackage.build(file);
            getLogger().debug("Deployment package built");
        } catch (Exception e) {
            throw new MojoExecutionException(
                    "The deployment package cannot be built : "
                            + e.getMessage());
        }

    }

    private void populate(DeploymentPackage currentPackage) throws IOException {
        Maven2OsgiConverter converter = new Maven2OsgiConverter();
        if (currentPackage.getSymbolicName() == null) {
            currentPackage.setSymbolicName(converter
                    .getBundleSymbolicName(getProject().getArtifact()));
        }

        if (currentPackage.getVersion() == null) {
            currentPackage.setVersion(converter.getVersion(getProject()
                    .getArtifact()));
        }

        if (currentPackage.getAddress() == null) {
            if (getProject().getOrganization() != null) {
                currentPackage.setContactAddress(getProject().getOrganization()
                        .getUrl());
            }
        }

        if (currentPackage.getCopyright() == null) {
            if (getProject().getOrganization() != null) {
                currentPackage.setCopyright(getProject().getOrganization()
                        .getName());
            }
        }

        if (currentPackage.getDescription() == null) {
            if (getProject().getDescription() != null) {
                currentPackage.setDescription(getProject().getDescription());
            }
        }

        if (currentPackage.getDocURL() == null) {
            if (getProject().getUrl() != null) {
                currentPackage.setDocURL(new URL(getProject().getUrl()));
            }
        }

        if (currentPackage.getLicense() == null) {
            if (getProject().getLicenses() != null
                    && !getProject().getLicenses().isEmpty()) {
                String lic = ((License) getProject().getLicenses().get(0))
                        .getName();
                currentPackage.setLicense(lic);
            }
        }

        if (currentPackage.getName() == null) {
            if (getProject().getName() != null  && ! getProject().getName().startsWith("Unnamed")) {
                currentPackage.setName(getProject().getName());
            }
        }

        if (currentPackage.getVendor() == null) {
            if (getProject().getOrganization() != null) {
                currentPackage.setVendor(getProject().getOrganization()
                        .getName());
            }
        }

        // Write Extra Data ?
        if (m_writeExtraData) {
            currentPackage.addManifestEntry("Created-By", System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
            currentPackage.addManifestEntry("Tool", PLUGIN_NAME);
            currentPackage.addManifestEntry("Created-At", "" + System.currentTimeMillis());
       }

    }

    /**
     * @return the logger
     * @see net.sourceforge.osgi.deployment.maven.IDeploymentPluginContext#getLogger()
     */
    public final Log getLogger() {
        return super.getLog();
    }

    /**
     * @return the outputDirectory
     * @see net.sourceforge.osgi.deployment.maven.IDeploymentPluginContext#getOutputDirectory()
     */
    public final File getOutputDirectory() {
        return m_ouputDirectory;
    }

    /**
     * @param p_outputDirectory the outputDirectory to set
     */
    public final void setOutputDirectory(final File p_outputDirectory) {
        m_ouputDirectory = p_outputDirectory;
    }

    /**
     * @return the base directory
     * @see net.sourceforge.osgi.deployment.maven.IDeploymentPluginContext#getBaseDir()
     */
    public final File getBaseDir() {
        return m_basedir;
    }

    /**
     * @return the menifest location
     * @see net.sourceforge.osgi.deployment.maven.IDeploymentPluginContext#getManifestLocation()
     */
    public final File getManifestLocation() {
        return m_manifestLocation;
    }

    /**
     * @return the maven project
     * @see net.sourceforge.osgi.deployment.maven.IDeploymentPluginContext#getProject()
     */
    public final MavenProject getProject() {
        return m_project;
    }

    /**
     * @return the build directory
     * @see net.sourceforge.osgi.deployment.maven.IDeploymentPluginContext#getBuildDirectory()
     */
    public final String getBuildDirectory() {
        return m_buildDirectory;
    }

    /**
     * @return the supported packaging types
     * @see net.sourceforge.osgi.deployment.maven.IDeploymentPluginContext#getSupportedProjectTypes()
     */
    public final List<String> getSupportedProjectTypes() {
        return m_supportedProjectTypes;
    }

    /**
     * @return the local repository
     * @see net.sourceforge.osgi.deployment.maven.IDeploymentPluginContext#getLocalRepository()
     */
    public final ArtifactRepository getLocalRepository() {
        return m_localRepository;
    }

    /**
     * @return the remote repositories
     * @see net.sourceforge.osgi.deployment.maven.IDeploymentPluginContext#getRemoteRepositories()
     */
    public final List<ArtifactRepository> getRemoteRepositories() {
        return m_remoteRepositories;
    }

    /**
     * @return the artifact factory
     * @see net.sourceforge.osgi.deployment.maven.IDeploymentPluginContext#getArtifactFactory()
     */
    public final ArtifactFactory getArtifactFactory() {
        return m_artifactFactory;
    }

    /**
     * @return the artifact resolver
     * @see net.sourceforge.osgi.deployment.maven.IDeploymentPluginContext#getArtifactResolver()
     */
    public final ArtifactResolver getArtifactResolver() {
        return m_artifactresolver;
    }

    /**
     * @return the artifact handler manager
     * @see net.sourceforge.osgi.deployment.maven.IDeploymentPluginContext#getArtifactHandlerManager()
     */
    public final ArtifactHandlerManager getArtifactHandlerManager() {
        return m_artifactHandlerManager;
    }

    /**
     * @return the archiver manager
     * @see net.sourceforge.osgi.deployment.maven.IDeploymentPluginContext#getArchiverManager()
     */
    public final ArchiverManager getArchiverManager() {
        return m_archiverManager;
    }

    /**
     * @return the deployment package info
     * @see net.sourceforge.osgi.deployment.maven.IDeploymentPluginContext#getDeploymentPackageInfo()
     */
    public final DeploymentPackageMetadata getDeploymentPackageInfo() {
        return m_deploymentPackageInfo;
    }

    /**
     * @return <CODE>TRUE</CODE> if extra data should be generated into the
     *         manifest file, else <CODE>FALSE></CODE>. Default is
     *         <CODE>TRUE</CODE>.
     * @see net.sourceforge.osgi.deployment.maven.IDeploymentPluginContext#isWriteExtraData()
     */
    public final boolean isWriteExtraData() {
        return m_writeExtraData;
    }

    /**
     * @param p_baseDir the baseDir to set
     */
    public final void setBaseDir(final File p_baseDir) {
        m_basedir = p_baseDir;
    }

    /**
     * @param p_manifestLocation the manifestLocation to set
     */
    public final void setManifestLocation(final File p_manifestLocation) {
        m_manifestLocation = p_manifestLocation;
    }

    /**
     * @param p_project the project to set
     */
    public final void setProject(final MavenProject p_project) {
        m_project = p_project;
    }

    /**
     * @param p_buildDirectory the buildDirectory to set
     */
    public final void setBuildDirectory(final String p_buildDirectory) {
        m_buildDirectory = p_buildDirectory;
    }

    /**
     * @param p_supportedProjectTypes the supportedProjectTypes to set
     */
    public final void setSupportedProjectTypes(
            final List<String> p_supportedProjectTypes) {
        m_supportedProjectTypes = p_supportedProjectTypes;
    }

    /**
     * @param p_localRepository the localRepository to set
     */
    public final void setLocalRepository(
            final ArtifactRepository p_localRepository) {
        m_localRepository = p_localRepository;
    }

    /**
     * @param p_remoteRepositories the remoteRepositories to set
     */
    public final void setRemoteRepositories(
            final List<ArtifactRepository> p_remoteRepositories) {
        m_remoteRepositories = p_remoteRepositories;
    }

    /**
     * @param p_artifactFactory the artifactFactory to set
     */
    public final void setArtifactFactory(final ArtifactFactory p_artifactFactory) {
        m_artifactFactory = p_artifactFactory;
    }

    /**
     * @param p_artifactResolver the artifactResolver to set
     */
    public final void setArtifactResolver(
            final ArtifactResolver p_artifactResolver) {
        m_artifactresolver = p_artifactResolver;
    }

    /**
     * @param p_artifactHandlerManager the artifactHandlerManager to set
     */
    public final void setArtifactHandlerManager(
            final ArtifactHandlerManager p_artifactHandlerManager) {
        m_artifactHandlerManager = p_artifactHandlerManager;
    }

    /**
     * @param p_archiverManager the archiverManager to set
     */
    public final void setArchiverManager(final ArchiverManager p_archiverManager) {
        m_archiverManager = p_archiverManager;
    }

    /**
     * @param p_deploymentPackageInfo the deploymentPackage to set
     */
    public final void setDeploymentPackageInfo(
            final DeploymentPackageMetadata p_deploymentPackageInfo) {
        m_deploymentPackageInfo = p_deploymentPackageInfo;
    }

    /**
     * @param p_deploymentPackageInfo the deploymentPackage to set
     */
    public final void setDeploymentPackage(
            final DeploymentPackageMetadata p_deploymentPackageInfo) {
        setDeploymentPackageInfo(p_deploymentPackageInfo);
    }

    /**
     * @param p_writeExtraData the writeExtraData to set
     */
    public final void setWriteExtraData(final boolean p_writeExtraData) {
        m_writeExtraData = p_writeExtraData;
    }

    /**
     * This method resolves an artifact on all available repositories and
     * returns the file handle to that artifact.
     * @param groupId the groupId of the artifact to resolve
     * @param artifactId the artifactId of the artifact to resolve
     * @param version the version of the artifact to resolve
     * @param classifier the classifier of the artifact to resolve
     * @return the resolved file handle of the artifact
     * @throws MojoExecutionException
     */
    public final File resolveResource(final String groupId,
            final String artifactId, final String version, final String classifier)
            throws MojoExecutionException {
        try {
            
            Artifact artifact = null;
            if (classifier == null) {            
                artifact = getArtifactFactory()
                    .createArtifact(groupId, artifactId, version,
                            Artifact.SCOPE_RUNTIME, "jar");
            } else {
                artifact = getArtifactFactory()
                    .createArtifactWithClassifier(groupId, artifactId, version,
                        "jar", classifier);
            }
            
            getArtifactResolver().resolve(artifact, getRemoteRepositories(),
                    getLocalRepository());
            final File artifactFile = artifact.getFile();
            return artifactFile;
        } catch (final Exception e) {
            // Wrap checked exception
            throw new MojoExecutionException("Error while resolving resource "
                    + groupId + ":" + artifactId + ":" + version, e);
        }
    }

}
