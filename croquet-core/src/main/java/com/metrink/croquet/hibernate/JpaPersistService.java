package com.metrink.croquet.hibernate;

import javax.annotation.Nullable;
import javax.persistence.Persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.metrink.croquet.WicketSettings;

/**
 * A PersistService that uses an EntityManager factory from a persistence.xml file.
 */
@Singleton
class JpaPersistService extends CroquetPersistService {
    private static final Logger LOG = LoggerFactory.getLogger(JpaPersistService.class);

    /**
     * Constructor.
     * @param wicketSettings the settings from Croquet.
     * @param persistenceUnitName the name of the persistence unit.
     */
    @Inject
    public JpaPersistService(final WicketSettings wicketSettings,
                             @Nullable @Named("jpa-unit-name") final String persistenceUnitName) {
        super(wicketSettings, persistenceUnitName, null);
    }

    @Override
    public synchronized void start() {
        if(null != getEntityManagerFactory()) {
            throw new IllegalStateException("Persistence service was already initialized.");
        }

        LOG.debug("Starting JpaPersistenceService using {}", getPersistenceUnitName());

        this.setEntityManagerFactory(Persistence.createEntityManagerFactory(getPersistenceUnitName()));
    }

}
