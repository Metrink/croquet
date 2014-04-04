package com.metrink.croquet.hibernate;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;

/**
 * A Guice module that takes care of all the Hibernate bindings when using the persistance.xml.
 */
public class PersistanceUnitHibernateModule extends AbstractModule {
    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(PersistanceUnitHibernateModule.class);

    @Override
    protected void configure() {
        bind(PersistService.class).to(JpaPersistService.class);
        bind(UnitOfWork.class).to(JpaPersistService.class);
        bind(EntityManager.class).toProvider(JpaPersistService.class);

        bind(CroquetPersistService.class).to(JpaPersistService.class);
        bind(EntityManagerFactory.class).toProvider(CroquetPersistService.EntityManagerFactoryProvider.class);
    }
}
