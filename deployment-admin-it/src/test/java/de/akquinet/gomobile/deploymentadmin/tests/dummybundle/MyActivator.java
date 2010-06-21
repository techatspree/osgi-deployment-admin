package de.akquinet.gomobile.deploymentadmin.tests.dummybundle;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

public class MyActivator implements BundleActivator {

	public void start(BundleContext bc) throws Exception {
		System.out.println("Hello ! I'm version " + bc.getBundle().getHeaders().get(Constants.BUNDLE_VERSION));
	}

	public void stop(BundleContext arg0) throws Exception {
		// TODO Auto-generated method stub

	}

}
