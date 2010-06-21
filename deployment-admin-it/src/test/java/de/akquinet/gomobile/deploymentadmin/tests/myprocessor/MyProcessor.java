package de.akquinet.gomobile.deploymentadmin.tests.myprocessor;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.osgi.service.deploymentadmin.spi.DeploymentSession;
import org.osgi.service.deploymentadmin.spi.ResourceProcessor;
import org.osgi.service.deploymentadmin.spi.ResourceProcessorException;


public class MyProcessor implements ResourceProcessor {

    private DeploymentSession m_session;

    private Map<String, String> m_map = new HashMap<String, String>();

    /**
     * Map resource name -> properties
     */
    private Map<String, Map<String, String>> m_resources = new HashMap<String, Map<String, String>>();

    private Map<String, String> m_toAdd;
    private Map<String, String> m_toRemove;
    private String m_resourceToAdd;
    private String m_resourceToRemove;


    public void begin(DeploymentSession session) {
        m_session = session;
        m_toAdd = new HashMap<String, String>();
        m_toRemove = new HashMap<String, String>();
    }

    public void cancel() {
        cleanUp();
    }

    public void commit() {
        if (m_toRemove.size() > 0) {
            for (String k : m_toRemove.keySet()) {
                m_map.remove(k);
            }
        }

        if (m_toAdd.size() > 0) {
            m_map.putAll(m_toAdd);
        }

        if (m_resourceToRemove != null) {
            m_resources.remove(m_resourceToRemove);
        }


        if (m_resourceToAdd != null) {
            m_resources.put(m_resourceToAdd, m_toAdd);
        }

        System.out.println("Session committed : " + m_map);

        cleanUp();
    }

    public void dropAllResources() throws ResourceProcessorException {
        if (m_session == null) {
            throw new ResourceProcessorException(ResourceProcessorException.CODE_OTHER_ERROR, "Can not process resource without a Deployment Session");
        }
        m_toRemove.putAll(m_map);

        System.out.println("Drop All Resources");

    }

    public void dropped(String resource) throws ResourceProcessorException {
        if (m_session == null) {
            throw new ResourceProcessorException(ResourceProcessorException.CODE_OTHER_ERROR, "Can not process resource without a Deployment Session");
        }

        m_resourceToRemove = resource;
        m_toRemove = m_resources.get(m_resourceToRemove);

        System.out.println("Drop " + resource);

    }

    public void prepare() throws ResourceProcessorException {
        if (m_session == null) {
            throw new ResourceProcessorException(ResourceProcessorException.CODE_OTHER_ERROR, "Can not process resource without a Deployment Session");
        }
        // Nothing to do...
    }

    public void process(String name, InputStream stream)
            throws ResourceProcessorException {
        if (m_session == null) {
            throw new ResourceProcessorException(ResourceProcessorException.CODE_OTHER_ERROR, "Can not process resource without a Deployment Session");
        }

        Properties props = new Properties();
        try {
            props.load(stream);
        } catch (IOException e) {
            throw new ResourceProcessorException(ResourceProcessorException.CODE_OTHER_ERROR, "Cannot read properties", e);
        }

        Map<String, String> map = convertToMap(props);

        m_resourceToAdd = name;
        m_toAdd = map;

        System.out.println("Processing " + name);

    }

    private Map<String, String> convertToMap(Properties props) {
        Map<String, String> map = new HashMap<String, String>();
        for (Object k : props.keySet()) {
            map.put((String) k, (String) props.get(k));
        }

        return map;
    }

    public void rollback() {
        cleanUp();
    }

    private void cleanUp() {
        m_toAdd = null;
        m_toRemove = null;
        m_resourceToAdd = null;
        m_resourceToRemove = null;
        m_session = null;
    }

}
