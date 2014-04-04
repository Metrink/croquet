package com.metrink.croquet.examples.crm;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.metrink.croquet.examples.crm.data.CompanyBean;
import com.metrink.croquet.examples.crm.data.GenericDataProvider.GenericDataProviderFactory;
import com.metrink.croquet.examples.crm.data.PeopleDataProvider.PeopleDataProviderFactory;

/**
 * A Guice module that binds dependencies.
 */
public class CrmModule extends AbstractModule {

    private final CrmSettings settings;

    /**
     * Constructor that takes a settings instance.
     * @param settings the settings.
     */
    public CrmModule(final CrmSettings settings) {
        this.settings = settings;
    }

    @Override
    protected void configure() {
        bind(String.class).annotatedWith(Names.named("current-user")).toInstance(settings.getCurrentUser());

        install(new FactoryModuleBuilder().build(PeopleDataProviderFactory.class));
        install(new FactoryModuleBuilder().build(new TypeLiteral<GenericDataProviderFactory<CompanyBean>>() { }));
    }

}
