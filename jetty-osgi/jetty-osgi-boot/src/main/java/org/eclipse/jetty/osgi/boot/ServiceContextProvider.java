package org.eclipse.jetty.osgi.boot;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.jetty.deploy.App;
import org.eclipse.jetty.osgi.boot.internal.serverfactory.ServerInstanceWrapper;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * ServiceContextProvider
 *
 *
 */
public class ServiceContextProvider extends AbstractContextProvider implements ServiceProvider
{ 
    private static final Logger LOG = Log.getLogger(AbstractContextProvider.class);
    
    private Map<ServiceReference, App> _serviceMap = new HashMap<ServiceReference, App>();
    
    private ServiceRegistration _serviceRegForServices;
    
    
    
    /* ------------------------------------------------------------ */
    public ServiceContextProvider(ServerInstanceWrapper wrapper)
    {
        super(wrapper);
    }
    
    
    /* ------------------------------------------------------------ */
    public boolean serviceAdded (ServiceReference serviceRef, ContextHandler context)
    {
        //TODO deploy a contexthandler that some other package has created as a service
        if (context == null || serviceRef == null)
            return false;
        
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getServerInstanceWrapper().getParentClassLoaderForWebapps());
        try
        {
            //See if there is a context file to apply to this pre-made context
            String contextFile = (String)serviceRef.getProperty(OSGiWebappConstants.JETTY_CONTEXT_FILE_PATH);
            if (contextFile == null)
                contextFile = (String)serviceRef.getProperty(OSGiWebappConstants.SERVICE_PROP_CONTEXT_FILE_PATH); 
                  
            String[] keys = serviceRef.getPropertyKeys();
            Dictionary properties = new Hashtable<String, Object>();
            if (keys != null)
            {
                for (String key:keys)
                    properties.put(key, serviceRef.getProperty(key));
            }
            Bundle bundle = serviceRef.getBundle();                
            String originId = bundle.getSymbolicName() + "-" + bundle.getVersion().toString() + "-"+contextFile;
            BundleApp app = new BundleApp(getDeploymentManager(), this, bundle, properties, contextFile, originId);         
            app.setContextHandler(context); //set the pre=made ContextHandler instance
            _serviceMap.put(serviceRef, app);
            getDeploymentManager().addApp(app);
            return true;
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(cl); 
        }
    }
    
    
    /* ------------------------------------------------------------ */
    public boolean serviceRemoved (ServiceReference serviceRef, ContextHandler context)
    {

        if (context == null || serviceRef == null)
            return false;

        App app = _serviceMap.remove(serviceRef);
        if (app != null)
        {
            getDeploymentManager().removeApp(app);
            return true;
        }

        return false;
    }
    
    
    
    /* ------------------------------------------------------------ */
    @Override
    protected void doStart() throws Exception
    {
        //register as an osgi service for deploying contexts defined in a bundle, advertising the name of the jetty Server instance we are related to
        Dictionary<String,String> properties = new Hashtable<String,String>();
        properties.put(OSGiServerConstants.MANAGED_JETTY_SERVER_NAME, getServerInstanceWrapper().getManagedServerName());
        
        //register as an osgi service for deploying contexts, advertising the name of the jetty Server instance we are related to
        _serviceRegForServices = FrameworkUtil.getBundle(this.getClass()).getBundleContext().registerService(ServiceProvider.class.getName(), this, properties);
        super.doStart();
    }
    
    /* ------------------------------------------------------------ */
    @Override
    protected void doStop() throws Exception
    {
        //unregister ourselves 
        if (_serviceRegForServices != null)
        {
            try
            {
                _serviceRegForServices.unregister();
            }
            catch (Exception e)
            {
                LOG.warn(e);
            }
        }
        super.doStop();
    }
}
