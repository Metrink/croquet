package com.metrink.croquet.hibernate;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.metrink.croquet.DataSourceFactory;
import com.sop4j.dbutils.QueryRunner;

/**
 * Guice module for binding QueryRunner provider.
 */
public class QueryRunnerModule extends AbstractModule {
    //private static final Logger LOG = LoggerFactory.getLogger(QueryRunnerModule.class);

    private final DataSourceFactory dataSourceFactory;

    /**
     * Constructor.
     * @param dataSourceFactory a factory to create {@link DataSource}s.
     */
    public QueryRunnerModule(final DataSourceFactory dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
    }

    @Override
    protected void configure() {
    }

    /**
     * Provider for {@link QueryRunner}.
     * @return a new {@link QueryRunner}.
     */
    @Provides
    public QueryRunner queryRunnerProvider() {
        return QueryRunnerProxyFactory.createProxy(dataSourceFactory);
    }
}
