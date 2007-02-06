package it.openutils.deployment.spring;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.ResourceUtils;
import org.springframework.web.context.ServletContextAware;


/**
 * @author fgiust
 * @version $Id: $
 */
public class EnvironmentPropertyConfigurer extends PropertyPlaceholderConfigurer implements ServletContextAware
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
     * {@inheritDoc}
     */
    public void setServletContext(ServletContext servletContext)
    {
        this.servletContext = servletContext;
    }

    /**
     * Setter for <code>fileLocation</code>.
     * @param fileLocation The fileLocation to set.
     */
    public void setFileLocation(String fileLocation)
    {
        this.fileLocation = fileLocation;
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

            String replacedLocations = StringUtils.replace(fileLocation, PROPERTY_ENV, hostname);
            replacedLocations = StringUtils.replace(replacedLocations, PROPERTY_APPL, applName);

            String[] locations = StringUtils.split(replacedLocations, ",");

            for (String loc : locations)
            {
                propertyUrl = getResource(StringUtils.strip(loc));
                if (propertyUrl != null)
                {
                    break;
                }
                log.debug("Property file not found at {}", loc);
            }

            if (propertyUrl == null && defaultEnvironment != null)
            {
                log.warn("Usage of \"defaultEnvironment\" is deprecated, please specify the fallback location "
                    + "as the last comma separated value in \"fileLocation\"");
                propertyUrl = getResource(StringUtils.replace(fileLocation, PROPERTY_ENV, this.defaultEnvironment));

            }

            if (propertyUrl == null)
            {
                log.error("No properties found at {}", replacedLocations);
            }
            else
            {
                Resource resource = new UrlResource(propertyUrl);
                super.setLocation(resource);
            }
        }

        super.postProcessBeanFactory(beanFactory);
    }

    private URL getResource(String resource)
    {
        URL url = null;

        if (servletContext != null)
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
        else
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

}
