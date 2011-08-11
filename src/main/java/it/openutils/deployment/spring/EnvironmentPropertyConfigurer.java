/**
 *
 * openutils deployment tools (http://www.openmindlab.com/lab/products/deployment.html)
 * Copyright(C) 2007-2011, Openmind S.r.l. http://www.openmindonline.it
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
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
import org.springframework.util.StringValueResolver;
import org.springframework.web.context.WebApplicationContext;


/**
 * <p>
 * A propertyconfigurer that can be used to dynamically select a list of property files based on the current
 * environment.
 * </p>
 * <p>
 * You can configure the <code>fileLocation</code> parameter with a list of file location, using variables for:
 * </p>
 * <ul>
 * <li>the server name: ${env}</li>
 * <li>the web application root folder name (only for web contexts): ${appl}</li>
 * <li>any web context init parameter (only for web contexts): ${contextParam/paramname}</li>
 * </ul>
 * <p>
 * <p>
 * A sample value for the <code>fileLocation</code> parameter would be: <code>WEB-INF/config/${env}/my.properties, 
 *  WEB-INF/config/${appl}/my.properties,
 *  WEB-INF/config/${contextParam/instance}/my.properties, 
 *  classpath:my-${env}.properties,classpath:default.properties</code>.
 * </p>
 * <p>
 * After replacing all the known variables the resulting list is parsed and any existing file is merged in a single
 * property list, used to configure the remaining spring context
 * </p>
 * @author fgiust
 * @version $Id: EnvironmentPropertyConfigurer.java 592 2010-03-19 13:24:12Z christian.strappazzon $
 */
public class EnvironmentPropertyConfigurer extends PropertyPlaceholderConfigurer
    implements
    ApplicationContextAware,
    SmartInstantiationAwareBeanPostProcessor
{

    private String serverPropertyName = "env";

    private String applicationPropertyName = "appl";

    /**
     * Logger.
     */
    private static Logger log = LoggerFactory.getLogger(EnvironmentPropertyConfigurer.class);

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

    private String nullValue;

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
     * Sets the serverPropertyName.
     * @param serverPropertyName the serverPropertyName to set
     */
    public void setServerPropertyName(String serverPropertyName)
    {
        this.serverPropertyName = serverPropertyName;
    }

    /**
     * Sets the applicationPropertyName.
     * @param applicationPropertyName the applicationPropertyName to set
     */
    public void setApplicationPropertyName(String applicationPropertyName)
    {
        this.applicationPropertyName = applicationPropertyName;
    }

    /**
     * Set all the properties configured as system properties.
     * @param exposeSystemProperties <code>true</code> if you want to set configured properties as system properties.
     */
    public void setExposeSystemProperties(boolean exposeSystemProperties)
    {
        this.exposeSystemProperties = exposeSystemProperties;
    }

    @Override
    public void setNullValue(String nullValue)
    {
        this.nullValue = nullValue;
        super.setNullValue(nullValue);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException
    {

        Map<String, String> initParametersMap = new HashMap<String, String>();

        if (fileLocation != null)
        {

            if (this.servletContext != null)
            {

                Enumeration<String> initParameters = servletContext.getInitParameterNames();
                while (initParameters.hasMoreElements())
                {
                    String paramName = initParameters.nextElement();
                    initParametersMap.put(
                        "${contextParam/" + paramName + "}",
                        servletContext.getInitParameter(paramName));
                }
            }

            String hostname = null;
            try
            {
                hostname = StringUtils.substringBefore(
                    StringUtils.lowerCase(InetAddress.getLocalHost().getHostName()),
                    ".");
                initParametersMap.put("${" + serverPropertyName + "}", hostname);
            }
            catch (UnknownHostException e)
            {
                log.error(e.getMessage()); // should not happen
            }

            if (hostname != null)
            {
                System.setProperty(serverPropertyName, hostname);
            }

            String applName = getApplicationName();
            if (applName != null)
            {
                System.setProperty(applicationPropertyName, applName);
                initParametersMap.put("${" + applicationPropertyName + "}", applName);
            }

            URL propertyUrl = null;

            String fileLocationFull = fileLocation;

            String replacedLocations = replaceAll(initParametersMap, fileLocationFull);

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
                    log.debug("Loading property file at {} from {}", loc, propertyUrl);

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
                else
                {
                    log.debug("Property file not found at {}", loc);
                }

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

    /**
     * @param propertyenv
     * @param hostname
     * @param fileLocationFull
     * @return
     */
    private String replaceAll(Map<String, String> params, String fileLocationFull)
    {
        String replacedLocations = fileLocationFull;

        for (Map.Entry<String, String> param : params.entrySet())
        {
            replacedLocations = StringUtils.replace(replacedLocations, param.getKey(), param.getValue());
        }

        return replacedLocations;
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
        PropertyAnnotationsUtils.autowireProperties(bean, new PlaceholderResolvingStringValueResolver(properties));
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

    /**
     * BeanDefinitionVisitor that resolves placeholders in String values, delegating to the
     * <code>parseStringValue</code> method of the containing class.
     */
    protected class PlaceholderResolvingStringValueResolver implements StringValueResolver
    {

        private final Properties props;

        public PlaceholderResolvingStringValueResolver(Properties props)
        {
            this.props = props;
        }

        public String resolveStringValue(String strVal) throws BeansException
        {
            String value = parseStringValue(strVal, this.props, new HashSet());
            return (value.equals(nullValue) ? null : value);
        }
    }
}
