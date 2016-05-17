package com.metrink.croquet;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.component.AbstractLifeCycle.AbstractLifeCycleListener;
import org.eclipse.jetty.util.component.LifeCycle;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.google.inject.persist.PersistFilter;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.ServletModule;
import com.google.inject.util.Providers;
import com.metrink.croquet.hibernate.DataSourceHibernateModule;
import com.metrink.croquet.hibernate.PersistanceUnitHibernateModule;
import com.metrink.croquet.hibernate.QueryRunnerModule;
import com.metrink.croquet.inject.CroquetRestModule;
import com.metrink.croquet.modules.ManagedModule;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This is the main class of Croquet.
 *
 * You must create an instance of this class to setup and run the framework.
 * @param <T> The class used when loading the settings YAML
 */
public class CroquetRest<T extends RestSettings> {
    protected static final EnumSet<DispatcherType> DISPATCHER_TYPES =
            EnumSet.of(DispatcherType.ASYNC,
                       DispatcherType.REQUEST,
                       DispatcherType.FORWARD,
                       DispatcherType.INCLUDE);

    private static final Logger LOG = LoggerFactory.getLogger(CroquetRest.class);

    private volatile CroquetStatus status = CroquetStatus.STOPPED;

    private Injector injector;

    private final List<Class<? extends ManagedModule>> managedModules = new ArrayList<Class<? extends ManagedModule>>();
    private final List<Module> guiceModules = new ArrayList<Module>();

    private final T settings;
    private Server jettyServer;
    private final ServletContextHandler sch;

    /**
     * Constructs a Croquet instance given the arguments pass into the program. The {@link WicketSettings} type T is passed
     * as the first argument to this constructor to avoid ugly reflection.
     *
     * @param clazz the settings class
     * @param args the arguments passed into the program.
     */
    CroquetRest(final Class<T> clazz, final T settings) {
        this.settings = settings;

        sch = new ServletContextHandler(ServletContextHandler.SESSIONS);

        // add the croquet module to our list of guice modules
        guiceModules.add(new CroquetRestModule<T>(clazz, settings));

        // The user can pick between: no db, a persistence.xml file, or using application.yml file.
        // If you're not using a db, then we skip this.
        // If you're using the application.yml file, then we install the DataSource Hibernate module.
        // If you're using the persistence.xml file, then we install the JpaPersistenceModule.
        if(settings.getDatabaseSettings().getNotUsed()) {
            LOG.info("Not configuring a database. " +
                    "If you get an error about no binding for an EntityManager, you need to configure a database.");
        } else if(settings.getDatabaseSettings().getPersistenceUnit() == null) {
            LOG.info("Using YAML file to configure Hibernate");

            final DataSourceFactory dataSourceFactory = new DataSourceFactory(settings.getDatabaseSettings());

            guiceModules.add(new DataSourceHibernateModule(dataSourceFactory));
            guiceModules.add(new QueryRunnerModule(dataSourceFactory));
        } else {
            LOG.info("Using persistence.xml file to configure Hibernate");

            guiceModules.add(new PersistanceUnitHibernateModule());
        }

        // this sets the name of the peristence unit
        // we need to jump through these hoops because it has to be
        // null when we're doing a unit test
        guiceModules.add(new AbstractModule() {
            @Override
            protected void configure() {
                bind(String.class).annotatedWith(Names.named("jpa-unit-name"))
                                  .toProvider(getPUNameProvider());
            }
        });

        // make sure we have at least one package
        if(settings.getProviderPackages().isEmpty()) {
            throw new RuntimeException("No provider packages specified");
        }

        guiceModules.add(new ServletModule() {
            @Override
            protected void configureServlets() {
                final StringBuilder sb = new StringBuilder();

                for(String path:settings.getProviderPackages()) {
                    sb.append(path);
                    sb.append(";");
                }

                final Map<String, String> params = new HashMap<String, String>();

                params.put(ServerProperties.PROVIDER_PACKAGES, sb.toString());

                bind(ServletContainer.class).in(Singleton.class);
                serve("/*").with(ServletContainer.class, params);
            }
        });
    }

    /**
     * Provides the name of the persistence unit.
     * @return the name of the persistence unit.
     */
    protected Provider<String> getPUNameProvider() {
        final String puName = settings.getDatabaseSettings().getPersistenceUnit();

        return Providers.<String>of(puName == null ? "croquet" : puName);
    }

    /**
     * Creates and sets the injector.
     */
    protected void createInjector() {
        // create our injector
        injector = Guice.createInjector(guiceModules);
    }

    /**
     * Gets the injector.
     * @return injector.
     */
    protected Injector getInjector() {
        return injector;
    }

    /**
     * Creates and starts all the managed modules.
     * @return a list of the managed module instances.
     */
    protected List<ManagedModule> createAndStartModules() {
        final List<ManagedModule> managedModuleInstances = new ArrayList<>();

        // create and start each managed module
        for(final Class<? extends ManagedModule> module:managedModules) {
            final ManagedModule mm = injector.getInstance(module);
            managedModuleInstances.add(mm);
            mm.start();
        }

        return managedModuleInstances;
    }


    /**
     * Returns the parsed settings.
     * @return the parsed settings.
     */
    public T getSettings() {
        return settings;
    }

    /**
     * Gets the status of the Croquet application.
     * @return the status of the Croquet application.
     */
    public CroquetStatus getStatus() {
        return status;
    }

    /**
     * Sets the status of Corquet.
     * @param status the status.
     */
    protected void setStatus(final CroquetStatus status) {
        this.status = status;
    }

    /**
     * Starts the Croquet framework.
     */
    @SuppressFBWarnings("DM_EXIT")
    public void run() {
        status = CroquetStatus.STARTING;

        // create the injector
        createInjector();

        // configure the Jetty server
        jettyServer = configureJetty(settings.getPort());

        // add a life-cycle listener to remove drop the PID
        jettyServer.addLifeCycleListener(new AbstractLifeCycleListener() {
            @Override
            public void lifeCycleStarted(final LifeCycle event) {
                final String pidFile = settings.getPidFile();

                // when a pid-file is configured, drop it upon successfully binding to the port.
                if (pidFile != null) {
                    LOG.info("Dropping PID file: {}", pidFile);

                    // the PidManager will automatically remove the pid file on shutdown
                    new PidManager(pidFile).dropPidOrDie();
                } else {
                    LOG.warn("No PID file specified, so not file will be dropped. " +
                            "Set a file via code or in the config file to drop a pid file.");
                }
            }
        });

        // start the server
        try {
            jettyServer.start();
        //CHECKSTYLE:OFF the only exception that is thrown
        } catch (final Exception e) {
        //CHECKSTYLE:ON
            LOG.error("Error starting Jetty: {}", e.getMessage(), e);

            System.err.println("Error starting Jetty: " + e.getMessage());
            System.exit(-1);

            throw new RuntimeException(e); // throw this so the whole app stops
        }

        // create and start the modules
        final List<ManagedModule> managedModuleInstances = createAndStartModules();

        // install the shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                status = CroquetStatus.STOPPING;

                try {
                    jettyServer.stop();
                //CHECKSTYLE:OFF the only exception that is thrown
                } catch (final Exception e) {
                //CHECKSTYLE:ON
                    LOG.error("Error stopping Jetty: {}", e.getMessage(), e);
                }

                // go through the modules stopping them
                for(final ManagedModule module:managedModuleInstances) {
                    module.stop();
                }

                status = CroquetStatus.STOPPED;

                LOG.info("Croquet has stopped");
            }

        });

        status = CroquetStatus.RUNNING;

        LOG.info("Croquet is running on port {}", settings.getPort());
    }

    /**
     * Adds a module to the Guice injector.
     * @param module the module to add to the Guice injector.
     */
    public void addGuiceModule(final Module module) {
        guiceModules.add(module);
    }

    /**
     * Adds a {@link ManagedModule} to the list of modules to be start.
     *
     * Modules are created by Guice so you can inject any dependencies.
     * Modules are started in the order that they are added.
     *
     * @param module the module to add.
     */
    public void addManagedModule(final Class<? extends ManagedModule> module) {
        managedModules.add(module);
    }

    /**
     * Gets the {@link ServletContextHandler} for Jetty.
     * @return the {@link ServletContextHandler} Jetty uses.
     */
    public ServletContextHandler getServletContextHandler() {
        return sch;
    }

    /**
     * Function used to configure Jetty and return a Server instance.
     * @param port the port to run Jetty on.
     * @return the {@link Server} instance.
     */
    protected Server configureJetty(final int port) {
        final Server server = new Server();
        final ServerConnector connector = new ServerConnector(server);
        final ServletContextHandler sch = getServletContextHandler();

        // TODO: make all of this configurable
        connector.setIdleTimeout((int)TimeUnit.HOURS.toMillis(1));
        connector.setSoLingerTime(-1);
        connector.setPort(port);

        server.addConnector(connector);

        // set the injector as an attribute in the context
        sch.setAttribute("guice-injector", getInjector());

        // prevent the JSESSIONID from getting set via a URL argument
        sch.setInitParameter("org.eclipse.jetty.servlet.SessionIdPathParameterName", "none");

        // if we're using a database, then install the filter
        if(!settings.getDatabaseSettings().getNotUsed()) {
            // setup a FilterHolder for the Guice Persistence
            final FilterHolder persistFilter = new FilterHolder(getInjector().getInstance(PersistFilter.class));

            // add the filter to the context
            sch.addFilter(persistFilter, "/*", DISPATCHER_TYPES);
        }

        // configure a FilterHolder for Guice
        final FilterHolder filterHolder = new FilterHolder(GuiceFilter.class);

        sch.addFilter(filterHolder, "/*", DISPATCHER_TYPES);
        sch.addServlet(DefaultServlet.class, "/*");

        //server.setDumpAfterStart(true);
        server.setHandler(sch);


        return server;
    }
}
