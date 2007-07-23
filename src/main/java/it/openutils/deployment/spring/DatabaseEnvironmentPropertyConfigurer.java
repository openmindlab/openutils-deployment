package it.openutils.deployment.spring;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;


/**
 * @author fgiust
 * @version $Id$
 */
public class DatabaseEnvironmentPropertyConfigurer extends EnvironmentPropertyConfigurer
    implements
    ApplicationContextAware,
    ApplicationListener
{

    private static Logger log = LoggerFactory.getLogger(DatabaseEnvironmentPropertyConfigurer.class);

    private String sqlQuery;

    private String dataSourceName;

    private DataSource dataSource;

    private ApplicationContext applicationContext;

    public void setSqlQuery(String sqlQuery)
    {
        this.sqlQuery = sqlQuery;
    }

    public void setDataSourceName(String dataSourceName)
    {
        this.dataSourceName = dataSourceName;
    }

    /**
     * {@inheritDoc}
     */
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
    }

    /**
     * Get Spring Context proprieties.
     * @return A properties object containing all spring properties.
     */
    @Override
    public Properties getProperties()
    {
        // loadAndRefresh(); @todo is this needed anymore?
        return properties;
    }

    /**
     * @throws IOException
     */
    private void loadAndRefresh() throws IOException
    {
        properties = mergeProperties();
        refresh();
    }

    /**
     * used to reload configuration code from db
     */
    public void refresh()
    {
        manuallyLoadDatasource();

        /**
         * inner utility class to create properties from rows extracted by the query
         * @author diego
         * @version $Id$
         */
        class RowHandler implements RowCallbackHandler
        {

            /**
             * {@inheritDoc}
             */
            public void processRow(final ResultSet rs) throws SQLException
            {
                String parmName = rs.getString(1);
                String parmValue = rs.getString(2);

                log.debug("Configuring property {}={}", parmName, parmValue);
                properties.put(parmName, parmValue);

            }
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        try
        {
            jdbcTemplate.query(sqlQuery, new RowHandler());
        }
        catch (DataAccessException e)
        {
            log.error(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onApplicationEvent(ApplicationEvent event)
    {
        manuallyLoadDatasource();
    }

    /**
     *
     */
    private void manuallyLoadDatasource()
    {
        if (dataSource == null)
        {
            dataSource = (DataSource) applicationContext.getBean(dataSourceName);
            try
            {
                properties = mergeProperties();
            }
            catch (IOException e)
            {
                log.debug("Exception while loading environment properties from file.");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException
    {

        super.postProcessBeanFactory(beanFactory);

        try
        {
            loadAndRefresh();
            Properties mergedProps = properties;

            // Convert the merged properties, if necessary.
            convertProperties(mergedProps);

            // Let the subclass process the properties.
            processProperties(beanFactory, mergedProps);
        }
        catch (IOException ex)
        {
            throw new BeanInitializationException("Could not load properties", ex);
        }

    }
}
