package com.metrink.croquet.hibernate;

import java.io.Serializable;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.jpa.internal.EntityManagerFactoryImpl;
import org.hibernate.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;
import com.metrink.croquet.Settings;

/**
 * A PersistService, UnitOfWork, and Provider<EntityManager> implementation that configures Hibernate.
 *
 * The persist.xml file is NOT used to configure Hibernate. Everything is driven through the DatabaseSettings class.
 */
@Singleton
class CroquetPersistService implements Provider<EntityManager>, UnitOfWork, PersistService {
    private static final Logger LOG = LoggerFactory.getLogger(CroquetPersistService.class);
    private static final String TRUE_STRING = "true";
    private static final String FALSE_STRING = "false";

    private final Settings settings;
    private final String persistenceUnitName;
    private final ConnectionProvider connectionProvider;
    private final ThreadLocal<EntityManager> entityManager = new ThreadLocal<EntityManager>();
    private volatile EntityManagerFactory entityManagerFactory;

    /**
     * Constructs the {@link CroquetPersistService}.
     * @param settings the settings to configure everything with.
     * @param connectionProvider the {@link ConnectionProvider} to use.
     */
    @Inject
    public CroquetPersistService(final Settings settings,
                                 @Nullable @Named("jpa-unit-name") final String persistenceUnitName,
                                 final ConnectionProvider connectionProvider) {
        this.settings = settings;
        this.persistenceUnitName = persistenceUnitName;
        this.connectionProvider = connectionProvider;
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    /**
     * Returns the current {@link EntityManagerFactory}.
     * @return {@link EntityManagerFactory}.
     */
    protected EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    /**
     * Sets the {@link EntityManagerFactory}.
     * @param entityManagerFactory the {@link EntityManagerFactory} to set.
     */
    protected void setEntityManagerFactory(final EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    /*
     * This is called when init() in the Servlet filter is called.
     * @see com.google.inject.persist.PersistService#start()
     */
    @Override
    public void start() {
        if(null != entityManagerFactory) {
            throw new IllegalStateException("Persistence service was already initialized.");
        }

        final Configuration configuration = new Configuration();

        //
        // We'll want to map these to settings
        //
        configuration.setProperty(AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "managed");
        configuration.setProperty(AvailableSettings.USE_GET_GENERATED_KEYS, TRUE_STRING);
        configuration.setProperty(AvailableSettings.USE_REFLECTION_OPTIMIZER, TRUE_STRING);
        configuration.setProperty(AvailableSettings.ORDER_UPDATES, TRUE_STRING);
        configuration.setProperty(AvailableSettings.ORDER_INSERTS, TRUE_STRING);
        configuration.setProperty(AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, TRUE_STRING);
        configuration.setProperty("jadira.usertype.autoRegisterUserTypes", TRUE_STRING);
        configuration.setProperty(AvailableSettings.DIALECT, settings.getDatabaseSettings().getDialectClass());

        // turn on and off features based upon log level
        if(LOG.isDebugEnabled()) {
            configuration.setProperty(AvailableSettings.GENERATE_STATISTICS, TRUE_STRING);
            configuration.setProperty(AvailableSettings.SHOW_SQL, TRUE_STRING);
        } else {
            configuration.setProperty(AvailableSettings.GENERATE_STATISTICS, FALSE_STRING);
            configuration.setProperty(AvailableSettings.SHOW_SQL, FALSE_STRING);
        }

        // add in all the entities
        for (final Class<? extends Serializable> entity : settings.getDatabaseSettings().getEntities()) {
            LOG.debug("Adding entity: {}", entity.getCanonicalName());
            configuration.addAnnotatedClass(entity);
        }

        final ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
            .applySettings(configuration.getProperties())
            // set our connection provider
            .addService(ConnectionProvider.class, connectionProvider)
            .build();

        // create the actual entity manager
        this.entityManagerFactory = new EntityManagerFactoryImpl(PersistenceUnitTransactionType.RESOURCE_LOCAL,
                                                                 false,
                                                                 null,
                                                                 configuration,
                                                                 serviceRegistry,
                                                                 persistenceUnitName);

    }

    /*
     * This is called when destroy in the Servlet filter is called.
     * @see com.google.inject.persist.PersistService#stop()
     */
    @Override
    public void stop() {
        if(!entityManagerFactory.isOpen()) {
            throw new IllegalStateException("Persistence service was already shut down.");
        }

        entityManagerFactory.close();
    }

    /*
     * This is called as the first thing in doFilter method in the Servlet filter.
     * @see com.google.inject.persist.UnitOfWork#begin()
     */
    @Override
    public void begin() {
        if(null != entityManager.get()) {
            throw new IllegalStateException("Work already begun on this thread. " +
                "Looks like you have called UnitOfWork.begin() twice without a balancing call to end() in between.");
        }

        entityManager.set(EntityManagerProxyFactory.createProxy((HibernateEntityManagerFactory)entityManagerFactory));
    }

    /*
     * This is called as the last thing in the doFilter method in the Servlet filter.
     * @see com.google.inject.persist.UnitOfWork#end()
     */
    @Override
    public void end() {
        final EntityManager em = entityManager.get();

        // Let's not penalize users for calling end() multiple times.
        if (null == em) {
          return;
        }

        final EntityTransaction tx = em.getTransaction();

        if(tx.isActive()) {
            LOG.warn("There was an active transaction at the end of a request");
            tx.commit();
        }

        em.close();
        entityManager.remove();
    }

    @Override
    public EntityManager get() {
        // check to see if our ThreadLocal has already been set
        if(entityManager.get() == null) {
            begin(); // if it hasn't then we call begin() to set it
        }

        final EntityManager em = entityManager.get();

        if(null == em) {
            throw new IllegalStateException("Requested EntityManager outside work unit. "
                + "Try calling UnitOfWork.begin() first, or use a PersistFilter if you "
                + "are inside a servlet environment.");
        }

        return em;
    }

    /**
     * A wrapper class around the CroquetPersistService because we cannot implement Provider twice.
     */
    @Singleton
    public static class EntityManagerFactoryProvider implements Provider<EntityManagerFactory> {
        private final CroquetPersistService croquetPersistService;

        /**
         * Wraps the {@link CroquetPersistService} to act as a provider.
         * @param croquetPersistService the {@link CroquetPersistService} to wrap.
         */
        @Inject
        public EntityManagerFactoryProvider(final CroquetPersistService croquetPersistService) {
            this.croquetPersistService = croquetPersistService;
        }

        @Override
        public EntityManagerFactory get() {
            return croquetPersistService.entityManagerFactory;
        }
    }

}
