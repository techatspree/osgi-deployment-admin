package de.akquinet.gomobile.deploymentadmin.tests.myprocessor;

import java.util.Properties;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.deploymentadmin.spi.ResourceProcessor;


public class ProcessorActivator implements BundleActivator {

    public static final String PID = "de.akquinet.gomobile.rp.dummy";

    private ServiceRegistration m_sr;

    public void start(BundleContext arg0) throws Exception {
        Properties props = new Properties();
        props.put(Constants.SERVICE_PID, PID);
        m_sr = arg0.registerService(ResourceProcessor.class.getName(),
                new MyProcessor(), props);
    }

    public void stop(BundleContext arg0) throws Exception {
        m_sr.unregister();
    }

}
