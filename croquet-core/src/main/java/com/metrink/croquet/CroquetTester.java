package com.metrink.croquet;

import java.util.List;

import org.apache.wicket.guice.GuiceComponentInjector;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.util.tester.WicketTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Provider;
import com.google.inject.persist.PersistService;
import com.google.inject.util.Providers;
import com.metrink.croquet.modules.ManagedModule;

/**
 * A tester for Croquet that creates a {@link WicketTester} instance.
 *
 * @param <T> The class used when loading the settings YAML
 */
public class CroquetTester<T extends Settings> extends Croquet<T> {
    private static final Logger LOG = LoggerFactory.getLogger(CroquetTester.class);

    private WicketTester tester;

    CroquetTester(final Class<T> clazz, final T settings) {
        super(clazz, settings);
    }

    @Override
    public void run() {
        LOG.debug("Calling run on CroquetTester does nothing");
    }

    @Override
    protected Provider<String> getPUNameProvider() {
        // always set the name to null so it's not recorded in the EntityManagerFactoryRegistry
        return Providers.<String>of(null);
    }

    /**
     * Creates a {@link WicketTester} setting the application.
     * @return a configured {@link WicketTester}.
     */
    public WicketTester getTester() {
        if(tester != null) {
            return tester;
        }

        // create the injector and start the modules
        createInjector();
        final List<ManagedModule> managedModuleInstances = createAndStartModules();

        // get the PersistService (Singleton) so we can start and stop it
        final PersistService persistService = getInjector().getInstance(PersistService.class);

        // This isn't flawless as we're starting the service only ONCE for the whole life of the
        // tester. In "real life" the service would be started with each request. However, because
        // we don't have a real ServletContext (see MockServletContext) we cannot attach a listener.
        persistService.start();

        // install the shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                setStatus(CroquetStatus.STOPPING);


                // go through the modules stopping them
                for(final ManagedModule module:managedModuleInstances) {
                    module.stop();
                }

                // stop the persist service when everything shuts down
                persistService.stop();

                setStatus(CroquetStatus.STOPPED);

                LOG.info("Croquet has stopped");
            }

        });

        // create the WebApplication
        final WebApplication webApplication = getInjector().getInstance(WebApplication.class);

        // calling this constructor allows Injector.get() to work
        new GuiceComponentInjector(webApplication, getInjector());

        // create the tester
        tester = new WicketTester(webApplication);

        setStatus(CroquetStatus.RUNNING);

        return tester;
    }
}
