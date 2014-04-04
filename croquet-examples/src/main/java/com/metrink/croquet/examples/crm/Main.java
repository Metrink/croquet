package com.metrink.croquet.examples.crm;

import org.hibernate.dialect.HSQLDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metrink.croquet.Croquet;
import com.metrink.croquet.CroquetBuilder;
import com.metrink.croquet.Settings;
import com.metrink.croquet.examples.crm.data.CompanyBean;
import com.metrink.croquet.examples.crm.data.PeopleBean;
import com.metrink.croquet.examples.crm.pages.CompanyPage;
import com.metrink.croquet.examples.crm.pages.PeoplePage;
import com.metrink.croquet.health.HealthCheck;

/**
 * The main class where Croquet starts from.
 *
 * There is only the static main method here. This class has
 * no state.
 */
public class Main {
    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private Main() { }

    /**
     * The main method of this application.
     * @param args the command line arguments.
     */
    public static void main(final String[] args) {
        // create the croquet object through the builder
        final Croquet<CrmSettings> croquet = configureBuilder(CrmSettings.class, args).build();

        // get the custom settings for the application
        // if custom settings aren't needed for Guice modules, then you
        // don't need this method as settings are bound by Croquet
        final CrmSettings settings = croquet.getSettings();

        // add in our Guice module
        croquet.addGuiceModule(new CrmModule(settings));

        // add in a managed module
        croquet.addManagedModule(EmailModule.class);

        // run the Crouet application
        croquet.run();

        // after this, a thread(s) will be running in the background
        // you can stop the application via SIGTERM
    }

    /**
     * This method is used to configure a {@link CroquetBuilder} as we use in here an in unit tests.
     * @param clazz the settings class.
     * @param args the command line arguments.
     * @return a CroquetBuilder instance.
     * @param <S> the type of the settings.
     */
    public static <S extends Settings> CroquetBuilder<S> configureBuilder(final Class<S> clazz, final String[] args) {
        return CroquetBuilder.create(clazz, args)
            .setHomePageClass(PeoplePage.class)
            .addPageMount("/people", PeoplePage.class)
            .addPageMount("/company", CompanyPage.class)
            .addHealthCheck("/statuscheck", HealthCheck.class)
            .setSqlDialect(HSQLDialect.class)
            .addJpaEntity(PeopleBean.class)
            .addJpaEntity(CompanyBean.class)
            .setPidFile("croquet.pid");
    }

}
