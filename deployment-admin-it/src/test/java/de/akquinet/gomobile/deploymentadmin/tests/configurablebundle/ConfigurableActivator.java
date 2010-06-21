package de.akquinet.gomobile.deploymentadmin.tests.configurablebundle;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;


public class ConfigurableActivator implements BundleActivator, ManagedService {

    private ServiceRegistration m_reg;

    public void start(BundleContext arg0) throws Exception {
        Dictionary dict = new Properties();
        dict.put(Constants.SERVICE_PID, "de.akquinet.gomobile.x");
        m_reg = arg0.registerService(ManagedService.class.getName(), this, dict);
    }

    public void stop(BundleContext arg0) throws Exception {
        m_reg.unregister();
    }

    public void updated(Dictionary properties) throws ConfigurationException {
        System.out.println("Under configuration");

        if (properties == null) {
        	System.out.println("No configuration available");
        	return;
        }

        String[] s = (String[]) properties.get("props");
        if (s == null) {
            System.out.println("Receive null....");
            throw new ConfigurationException("props", "not set");
        } else {
            List list = Arrays.asList(s);
            System.out.println("Props = " + list);
        }
    }

}
