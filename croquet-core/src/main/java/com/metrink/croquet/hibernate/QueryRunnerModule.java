package com.metrink.croquet.hibernate;

import javax.sql.DataSource;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.util.Providers;
import com.sop4j.dbutils.QueryRunner;

/**
 * Guice module for binding QueryRunner provider.
 */
public class QueryRunnerModule extends AbstractModule {
    //private static final Logger LOG = LoggerFactory.getLogger(QueryRunnerModule.class);

    private final DataSource dataSource;

    /**
     * Constructor which takes a {@link DataSource}.
     * @param dataSource the DataSource to use.
     */
    public QueryRunnerModule(final DataSource dataSource) {
        this.dataSource = dataSource;
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
        return QueryRunnerProxyFactory.createProxy(Providers.of(dataSource));
    }
}
