package com.metrink.croquet.inject;

import com.google.inject.AbstractModule;
import com.metrink.croquet.AbstractSettings;
import com.metrink.croquet.RestSettings;

/**
 * Croquet's Guice module that configures most of the dependencies.
 * @param <T> the subclass type of the settings instance
 */
public class CroquetRestModule<T extends RestSettings> extends AbstractModule {

    private final T settings;
    private Class<T> clazz;

    /**
     * Constructor for the CroquetModule.
     * @param clazz the settings base class.
     * @param settings the settings.
     */
    public CroquetRestModule(final Class<T> clazz, final T settings) {
        this.clazz = clazz;
        this.settings = settings;
    }

    @Override
    protected void configure() {
        // bind the settings classes
        bind(AbstractSettings.class).toInstance(settings);
        bind(clazz).toInstance(settings);
        bind(RestSettings.class).toInstance(settings);
    }

}
