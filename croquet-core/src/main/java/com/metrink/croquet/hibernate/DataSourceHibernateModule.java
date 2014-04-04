package com.metrink.croquet.hibernate;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;

/**
 * A Guice module that takes care of all the Hibernate bindings when using a data source.
 */
public class DataSourceHibernateModule extends AbstractModule {
    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(DataSourceHibernateModule.class);

    private final DataSource dataSource;

    /**
     * Constructor that takes the {@link DataSource} to use for all connections.
     * @param dataSource the {@link DataSource} to use for all connections.
     */
    public DataSourceHibernateModule(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    protected void configure() {
        // bind the DataSource
        bind(DataSource.class).toInstance(dataSource);

        // bind the ConnectionProvider
        bind(ConnectionProvider.class).to(TomcatJDBCConnectionProvider.class);

        bind(PersistService.class).to(CroquetPersistService.class);
        bind(UnitOfWork.class).to(CroquetPersistService.class);
        bind(EntityManager.class).toProvider(CroquetPersistService.class);
        bind(EntityManagerFactory.class).toProvider(CroquetPersistService.EntityManagerFactoryProvider.class);
    }
}
