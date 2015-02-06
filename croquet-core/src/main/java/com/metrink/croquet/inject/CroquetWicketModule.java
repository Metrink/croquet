package com.metrink.croquet.inject;

import org.apache.wicket.IPageFactory;
import org.apache.wicket.protocol.http.WebApplication;

import com.google.inject.AbstractModule;
import com.metrink.croquet.AbstractSettings;
import com.metrink.croquet.WicketSettings;
import com.metrink.croquet.wicket.GuicePageFactory;

/**
 * Croquet's Guice module that configures most of the dependencies.
 * @param <T> the subclass type of the settings instance
 */
public class CroquetWicketModule<T extends WicketSettings> extends AbstractModule {

    private final T settings;
    private Class<T> clazz;

    /**
     * Constructor for the CroquetModule.
     * @param clazz the settings base class.
     * @param settings the settings.
     */
    public CroquetWicketModule(final Class<T> clazz, final T settings) {
        this.clazz = clazz;
        this.settings = settings;
    }

    @Override
    protected void configure() {
        // bind the settings classes
        bind(AbstractSettings.class).toInstance(settings);
        bind(clazz).toInstance(settings);

        // bind the Wicket application
        bind(WebApplication.class).to(settings.getWebApplicationClass());

        // bind the page factory
        bind(IPageFactory.class).to(GuicePageFactory.class);
    }

}
