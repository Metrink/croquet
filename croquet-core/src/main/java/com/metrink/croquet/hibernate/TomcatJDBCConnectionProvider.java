package com.metrink.croquet.hibernate;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.Configurable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * A ConnectionProvider that uses Tomcat's JDBC Connection Pool under the hood.
 */
@Singleton
class TomcatJDBCConnectionProvider implements ConnectionProvider, Configurable {
    private static final long serialVersionUID = -7328861997611373746L;
    private static final Logger LOG = LoggerFactory.getLogger(TomcatJDBCConnectionProvider.class);

    private final transient DataSource ds;

    /**
     * Constructs the {@link ConnectionProvider} using the injected {@link DataSource}.
     * @param dataSource the {@link DataSource} to use.
     */
    @Inject
    public TomcatJDBCConnectionProvider(final DataSource dataSource) {
        this.ds = dataSource;
    }

    @Override
    public void configure(@SuppressWarnings("rawtypes") final Map configurationValues) {
        //
        // There is nothing to configure because this is simply a wrapper around the DataSource passed in
        //
        return;
    }

    @Override
    public boolean isUnwrappableAs(@SuppressWarnings("rawtypes") final Class unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(final Class<T> unwrapType) {
        return null;
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            return ds.getConnection();
        } finally {
            LOG.debug("Getting Connection: active: {} of {}, idle: {} of {}",
                    ds.getNumActive(),
                    ds.getMaxActive(),
                    ds.getNumIdle(),
                    ds.getMaxIdle());
        }
    }

    @Override
    public void closeConnection(final Connection conn) throws SQLException {
        try {
            conn.close();
        } finally {
            LOG.debug("Closing Connection: active: {} of {}, idle: {} of {}",
                    ds.getNumActive(),
                    ds.getMaxActive(),
                    ds.getNumIdle(),
                    ds.getMaxIdle());
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }
}
