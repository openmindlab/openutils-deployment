/*
 * Copyright Openmind http://www.openmindonline.it
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.openutils.deployment.spring;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.ResourceUtils;
import org.springframework.web.context.WebApplicationContext;


/**
 * @author fgiust
 * @version $Id: $
 */
public class EnvironmentPropertyConfigurer extends PropertyPlaceholderConfigurer
    implements
    ApplicationContextAware,
    SmartInstantiationAwareBeanPostProcessor
{

    /**
     * Application name (webapp name) variable.
     */
    private static final String PROPERTY_APPL = "${appl}";

    /**
     * Environment (server name) variable.
     */
    private static final String PROPERTY_ENV = "${env}";

    /**
     * Logger.
     */
    private static Logger log = LoggerFactory.getLogger(EnvironmentPropertyConfigurer.class);

    /**
     * @deprecated use defaultLocation
     */
    @Deprecated
    private String defaultEnvironment;

    private ServletContext servletContext;

    private String fileLocation;

    /**
     * Cached properties (super field is private).
     */
    protected Properties properties;

    /**
     * Are properties inherited from default configuration? default is true,
     */
    private boolean inherit = true;

    /**
     * Set all the properties configured as system properties.
     */
    private boolean exposeSystemProperties;

    /**
     * Setter for <code>fileLocation</code>.
     * @param fileLocation The fileLocation to set.
     */
    public void setFileLocation(String fileLocation)
    {
        this.fileLocation = fileLocation;
    }

    /**
     * Sets the inherit.
     * @param inherit the inherit to set
     */
    public void setInherit(boolean inherit)
    {
        this.inherit = inherit;
    }

    /**
     * Setter for <code>defaultEnvironment</code>.
     * @param defaultEnvironment The defaultEnvironment to set.
     * @deprecated use defaultLocation
     */
    @Deprecated
    public void setDefaultEnvironment(String defaultEnvironment)
    {
        this.defaultEnvironment = defaultEnvironment;
    }

    /**
     * Set all the properties configured as system properties.
     * @param exposeSystemProperties <code>true</code> if you want to set configured properties as system properties.
     */
    public void setExposeSystemProperties(boolean exposeSystemProperties)
    {
        this.exposeSystemProperties = exposeSystemProperties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException
    {
        if (fileLocation != null)
        {

            String hostname = null;
            try
            {
                hostname = StringUtils.substringBefore(
                    StringUtils.lowerCase(InetAddress.getLocalHost().getHostName()),
                    ".");
            }
            catch (UnknownHostException e)
            {
                log.error(e.getMessage()); // should not happen
            }

            if (hostname != null)
            {
                System.setProperty("env", hostname);
            }

            String applName = getApplicationName();
            if (applName != null)
            {
                System.setProperty("appl", applName);
            }

            URL propertyUrl = null;

            String fileLocationFull = fileLocation;

            if (defaultEnvironment != null)
            {
                log.warn("Usage of \"defaultEnvironment\" is deprecated, please specify the fallback location "
                    + "as the last comma separated value in \"fileLocation\"");
                fileLocationFull = fileLocationFull
                    + ","
                    + StringUtils.replace(fileLocationFull, PROPERTY_ENV, this.defaultEnvironment);
            }

            String replacedLocations = StringUtils.replace(fileLocationFull, PROPERTY_ENV, hostname);
            replacedLocations = StringUtils.replace(replacedLocations, PROPERTY_APPL, applName);

            String[] locations = StringUtils.split(replacedLocations, ",");

            if (inherit)
            {
                ArrayUtils.reverse(locations);
            }

            Properties props = new Properties();
            boolean found = false;

            for (String loc : locations)
            {
                propertyUrl = getResource(StringUtils.strip(loc));
                if (propertyUrl != null)
                {
                    found = true;
                    log.debug("Loading property file at {}", loc);

                    Resource resource = new UrlResource(propertyUrl);
                    InputStream is = null;

                    try
                    {
                        is = resource.getInputStream();
                        props.load(is);
                    }
                    catch (IOException e)
                    {
                        log.error("Error loading " + propertyUrl.toString(), e);
                    }
                    finally
                    {
                        if (is != null)
                        {
                            try
                            {
                                is.close();
                            }
                            catch (IOException e)
                            {
                                // ignore
                            }
                        }
                    }

                    if (!inherit)
                    {
                        break;
                    }
                }
                log.debug("Property file not found at {}", loc);

            }

            if (!found)
            {
                log.error("No properties found at {}", replacedLocations);
            }

            this.properties = props;
            super.setProperties(props);

            if (exposeSystemProperties)
            {
                Iterator<Object> i = props.keySet().iterator();
                while (i.hasNext())
                {
                    String key = (String) i.next();
                    String value = (String) props.get(key);
                    System.setProperty(key, value);
                }

            }

        }

        super.postProcessBeanFactory(beanFactory);
    }

    private URL getResource(String resource)
    {
        URL url = null;

        if (servletContext != null && !StringUtils.contains(resource, "classpath:"))
        {
            try
            {
                if (resource != null && !resource.startsWith("/"))
                {
                    url = servletContext.getResource("/" + resource);
                }
                else
                {
                    url = servletContext.getResource(resource);
                }

                if (url != null)
                {
                    // check needed for servletUnit
                    // we need to check for a connection because getResource always returns a URL, also if the resource
                    // doesn't exists
                    url.openConnection().connect();
                }

            }
            catch (MalformedURLException e)
            {
                log.error(e.getMessage(), e);
            }
            catch (IOException e)
            {
                // ignore, URL is not a valid resource
                url = null;
            }
        }
        if (url == null)
        {
            try
            {
                url = ResourceUtils.getURL(resource);
                url.openStream().close(); // test if the resource actually exists
            }
            catch (IOException e)
            {
                // ignore, can be normal
                url = null;
            }
        }
        return url;
    }

    private String getApplicationName()
    {
        if (servletContext != null)
        {
            String url = servletContext.getRealPath("/");
            url = StringUtils.replace(url, "\\", "/");
            if (url.endsWith("/"))
            {
                url = StringUtils.substringBeforeLast(url, "/");
            }

            return StringUtils.substringAfterLast(url, "/");
        }
        return StringUtils.EMPTY;
    }

    /**
     * Returns the Properties loaded by this configurer.
     * @return Properties
     */
    public Properties getProperties()
    {
        return properties;
    }

    /**
     * Returns a single property.
     * @param key Property key
     * @return property value or <code>null</code> if not found.
     */
    public String getProperty(String key)
    {
        // better be safe, it doesn't hurt
        if (properties == null)
        {
            return null;
        }
        return properties.getProperty(key);
    }

    /**
     * {@inheritDoc}
     */
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        // don't implement ServletContextAware or it will fail if javax.servlet dependency is not available
        try
        {
            if (applicationContext instanceof WebApplicationContext)
            {
                this.servletContext = ((WebApplicationContext) applicationContext).getServletContext();
            }
        }
        catch (NoClassDefFoundError e)
        {
            // ignore, we are not in a web project or spring web is not available
        }
    }

    public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException
    {
        PropertyAnnotationsUtils.autowireProperties(bean, properties);
        return true;
    }

    public Class predictBeanType(Class beanClass, String beanName)
    {
        return null;
    }

    public Constructor[] determineCandidateConstructors(Class beanClass, String beanName) throws BeansException
    {
        return null;
    }

    public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException
    {
        return bean;
    }

    public Object postProcessBeforeInstantiation(Class beanClass, String beanName) throws BeansException
    {
        return null;
    }

    public PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean,
        String beanName) throws BeansException
    {

        return pvs;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException
    {
        return bean;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException
    {
        return bean;
    }
}
