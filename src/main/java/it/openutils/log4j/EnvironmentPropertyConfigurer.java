package it.openutils.log4j;

import java.io.File;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.context.ServletContextAware;


/**
 * @author fgiust
 * @version $Revision$ ($Author$)
 */
public class EnvironmentPropertyConfigurer extends PropertyPlaceholderConfigurer implements ServletContextAware
{

    private String fileLocation;

    private String defaultEnvironment;

    private ServletContext servletContext;

    /**
     * Logger.
     */
    private static Logger log = Logger.getLogger(EnvironmentPropertyConfigurer.class);

    /**
     * @see org.springframework.web.context.ServletContextAware#setServletContext(javax.servlet.ServletContext)
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
     */
    public void setDefaultEnvironment(String defaultEnvironment)
    {
        this.defaultEnvironment = defaultEnvironment;
    }

    private String getRootPath()
    {
        if (servletContext != null)
        {
            return servletContext.getRealPath("/");
        }
        return "src/main/webapp/";
    }

    private URL getResource(String resource)
    {
        URL url = null;

        if (servletContext != null)
        {
            try
            {
                url = servletContext.getResource(resource);
            }
            catch (MalformedURLException e)
            {
                log.error(e.getMessage(), e);
            }
        }
        else
        {
            try
            {
                return new File(getRootPath(), resource).toURL();
            }
            catch (MalformedURLException e)
            {
                log.error(e.getMessage(), e);
            }
            // test
        }
        return url;
    }

    /**
     * @see org.springframework.beans.factory.config.PropertyResourceConfigurer#postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)
     */
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException
    {
        if (fileLocation != null)
        {

            String hostname = null;

            try
            {
                hostname = StringUtils.lowerCase(InetAddress.getLocalHost().getHostName());
            }
            catch (UnknownHostException e)
            {
                log.error(e.getMessage());
            }

            System.setProperty("env", hostname);

            String resolvedLocation = StringUtils.replace(fileLocation, "${env}", hostname);
            URL propertyUrl = null;

            propertyUrl = getResource(resolvedLocation);

            if (propertyUrl == null)
            {
                log.info("No environment specific properties found at " + resolvedLocation + ", using default");
                resolvedLocation = StringUtils.replace(fileLocation, "${env}", this.defaultEnvironment);

                propertyUrl = getResource(resolvedLocation);

            }

            if (propertyUrl == null)
            {
                log.error("No default properties found at " + resolvedLocation);
            }
            else
            {
                Resource resource = new UrlResource(propertyUrl);
                super.setLocation(resource);
            }
        }

        super.postProcessBeanFactory(beanFactory);
    }

}
