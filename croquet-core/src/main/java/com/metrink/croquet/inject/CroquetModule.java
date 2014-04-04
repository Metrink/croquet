package com.metrink.croquet.inject;

import org.apache.wicket.IPageFactory;
import org.apache.wicket.protocol.http.WebApplication;

import com.google.inject.AbstractModule;
import com.metrink.croquet.Settings;
import com.metrink.croquet.wicket.GuicePageFactory;

/**
 * Croquet's Guice module that configures most of the dependencies.
 * @param <T> the subclass type of the settings instance
 */
public class CroquetModule<T extends Settings> extends AbstractModule {

    private final T settings;
    private Class<T> clazz;

    /**
     * Constructor for the CroquetModule.
     * @param clazz the settings base class.
     * @param settings the settings.
     */
    public CroquetModule(final Class<T> clazz, final T settings) {
        this.clazz = clazz;
        this.settings = settings;
    }

    @Override
    protected void configure() {
        // bind the settings classes
        bind(clazz).toInstance(settings);
        bind(Settings.class).toInstance(settings);

        // bind the Wicket application
        bind(WebApplication.class).to(settings.getWebApplicationClass());

        // bind the page factory
        bind(IPageFactory.class).to(GuicePageFactory.class);
    }

}
