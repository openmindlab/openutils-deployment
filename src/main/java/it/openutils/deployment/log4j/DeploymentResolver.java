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
package it.openutils.deployment.log4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;

import javax.servlet.ServletContext;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author fgiust
 * @version $Id$
 */
public class DeploymentResolver
{

    /**
     * Logger.
     */
    private static Logger log = LoggerFactory.getLogger(DeploymentResolver.class);

    public static File resolveServerRelativeLocation(ServletContext context, String commaSeparatedListOfPaths)
        throws FileNotFoundException
    {
        String[] propertiesLocation = StringUtils.split(commaSeparatedListOfPaths, ',');

        String servername = resolveServerName();

        String rootPath = StringUtils.stripEnd(StringUtils.replace(context.getRealPath("/"), "\\", "/"), "/"); //$NON-NLS-1$ //$NON-NLS-2$
        String webapp = StringUtils.substringAfterLast(rootPath, "/"); //$NON-NLS-1$

        log.info("Resolving log configuration using servername={} and webapp={}", servername, webapp);

        for (int j = 0; j < propertiesLocation.length; j++)
        {
            String location = StringUtils.trim(propertiesLocation[j]);
            location = StringUtils.replace(location, "${servername}", servername); //$NON-NLS-1$
            location = StringUtils.replace(location, "${webapp}", webapp); //$NON-NLS-1$

            File initFile = new File(rootPath, location);

            if (!initFile.exists() || initFile.isDirectory())
            {
                log.debug("Log4j config not found at {}", initFile.getAbsolutePath());
                continue;
            }

            log.debug("Log4j config FOUND at {}", initFile.getAbsolutePath());

            return initFile;

        }

        throw new FileNotFoundException(
            MessageFormat
                .format(
                    "No configuration found using location list {0}. [servername] is [{1}], [webapp] is [{2}] and base path is [{3}]", //$NON-NLS-1$
                    new Object[]{ArrayUtils.toString(propertiesLocation), servername, webapp, rootPath }));

    }

    /**
     * Resolve the current server name.
     * @return server name, all lowercase, without domain
     */
    public static String resolveServerName()
    {
        String servername = null;

        try
        {
            servername = StringUtils.lowerCase(InetAddress.getLocalHost().getHostName());
            if (StringUtils.contains(servername, "."))
            {
                servername = StringUtils.substringBefore(servername, ".");
            }

        }
        catch (UnknownHostException e)
        {
            System.err.println(e.getMessage());
        }
        return servername;
    }
}
