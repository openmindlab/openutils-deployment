/**
 *
 * openutils deployment tools (http://www.openmindlab.com/lab/products/deployment.html)
 * Copyright(C) 2007-2010, Openmind S.r.l. http://www.openmindonline.it
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

package it.openutils.deployment.log4j;

import java.io.FileNotFoundException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.springframework.util.Log4jConfigurer;
import org.springframework.web.util.Log4jConfigListener;
import org.springframework.web.util.WebUtils;


/**
 * @author Fabrizio Giustina
 * @version $Id$
 */
public class EnvironmentLog4jConfigListener extends Log4jConfigListener
{

    /**
     * Default value for the DEFAULT_INITIALIZATION_PARAMETER parameter.
     */
    public static final String DEFAULT_INITIALIZATION_PARAMETER = //
    "WEB-INF/config/${servername}/${webapp}/log4j.xml," //$NON-NLS-1$
        + "WEB-INF/config/${servername}/log4j.xml," //$NON-NLS-1$
        + "WEB-INF/config/${webapp}/log4j.xml," //$NON-NLS-1$
        + "WEB-INF/config/default/log4j.xml," //$NON-NLS-1$
        + "WEB-INF/config/log4j.xml"; //$NON-NLS-1$

    /**
     * {@inheritDoc}
     */
    @Override
    public void contextInitialized(ServletContextEvent event)
    {
        initLogging(event.getServletContext());
    }

    public static void initLogging(ServletContext servletContext)
    {
        if (exposeWebAppRoot(servletContext))
        {
            WebUtils.setWebAppRootSystemProperty(servletContext);
        }

        String servername = DeploymentResolver.resolveServerName();
        if (servername != null)
        {
            System.setProperty("server.name", servername);
            System.setProperty("server", servername);
        }

        String locationList = servletContext.getInitParameter("log4jConfigLocation");

        if (locationList == null)
        {
            locationList = DEFAULT_INITIALIZATION_PARAMETER;
        }

        String location;

        try
        {
            location = DeploymentResolver.resolveServerRelativeLocation(servletContext, locationList).getAbsolutePath();
        }
        catch (FileNotFoundException ex)
        {
            throw new IllegalArgumentException("Invalid 'log4jConfigLocation' parameter: " + ex.getMessage());
        }
        if (location != null)
        {
            servletContext.log("Initializing Log4J from [" + location + "]");
            try
            {

                String intervalString = servletContext.getInitParameter("log4jRefreshInterval");
                if (intervalString != null)
                {
                    try
                    {
                        long refreshInterval = Long.parseLong(intervalString);
                        Log4jConfigurer.initLogging(location, refreshInterval);
                    }
                    catch (NumberFormatException ex)
                    {
                        throw new IllegalArgumentException("Invalid 'log4jRefreshInterval' parameter: "
                            + ex.getMessage());
                    }
                }
                else
                {
                    Log4jConfigurer.initLogging(location);
                }
            }
            catch (FileNotFoundException ex)
            {
                throw new IllegalArgumentException("Invalid 'log4jConfigLocation' parameter: " + ex.getMessage());
            }
        }
    }

    private static boolean exposeWebAppRoot(ServletContext servletContext)
    {
        String exposeWebAppRootParam = servletContext.getInitParameter("log4jExposeWebAppRoot");
        return exposeWebAppRootParam == null || Boolean.valueOf(exposeWebAppRootParam).booleanValue();
    }

}
