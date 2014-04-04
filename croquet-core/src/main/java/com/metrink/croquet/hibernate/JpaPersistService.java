package com.metrink.croquet.hibernate;

import javax.annotation.Nullable;
import javax.persistence.Persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.metrink.croquet.Settings;

/**
 * A PersistService that uses an EntityManager factory from a persistence.xml file.
 */
@Singleton
class JpaPersistService extends CroquetPersistService {
    private static final Logger LOG = LoggerFactory.getLogger(JpaPersistService.class);

    /**
     * Constructor.
     * @param settings the settings from Croquet.
     * @param persistenceUnitName the name of the persistence unit.
     */
    @Inject
    public JpaPersistService(final Settings settings,
                             @Nullable @Named("jpa-unit-name") final String persistenceUnitName) {
        super(settings, persistenceUnitName, null);
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
