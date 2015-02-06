package com.metrink.croquet;

import java.io.Serializable;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Serializable} {@link DataSource} factory class.
 *
 * This class should be used with caution as it could result in multiple pools of connections opened to the DB.
 *
 */
public class DataSourceFactory implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(DataSourceFactory.class);

    private final DatabaseSettings dbSettings;
    private transient DataSource dataSource;

    /**
     * Constructor which takes an initialized {@link DataSource}.
     * @param dbSettings the {@link DatabaseSettings} to use to construct {@link DataSource}s.
     */
    public DataSourceFactory(final DatabaseSettings dbSettings) {
        this.dbSettings = dbSettings;
        this.dataSource = getDataSource();
    }

    /**
     * Gets a {@link DataSource} constructing a new one if needed.
     * @return a {@link DataSource}.
     */
    public DataSource getDataSource() {
        if(dataSource == null) {
            LOG.info("Having to construct a new DataSource");

            dataSource = new DataSource();

            dataSource.setDriverClassName(dbSettings.getDriver());
            dataSource.setUrl(dbSettings.getJdbcUrl());
            dataSource.setUsername(dbSettings.getUser());
            dataSource.setPassword(dbSettings.getPass());

            dataSource.setMaxActive(dbSettings.getMaxActive());
            dataSource.setMaxIdle(dbSettings.getMaxIdle());
            dataSource.setMinIdle(dbSettings.getMinIdle());
            dataSource.setInitialSize(dbSettings.getInitialSize());
            dataSource.setTestOnBorrow(dbSettings.getTestOnBorrow());
            dataSource.setTestOnReturn(dbSettings.getTestOnReturn());
            dataSource.setTestWhileIdle(dbSettings.getTestWhileIdle());
            dataSource.setValidationQuery(dbSettings.getValidationQuery());
            dataSource.setLogValidationErrors(dbSettings.getLogValidationErrors());

            // a catch-all for any other properties that are needed
            dataSource.setDbProperties(dbSettings.getProperties());
        }

        return dataSource;
    }
}
