package com.metrink.croquet;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.DispatcherType;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.wicket.guice.GuiceWebApplicationFactory;
import org.apache.wicket.protocol.http.WicketFilter;
import org.apache.wicket.protocol.ws.jetty9.Jetty9WebSocketFilter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.component.AbstractLifeCycle.AbstractLifeCycleListener;
import org.eclipse.jetty.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.name.Names;
import com.google.inject.persist.PersistFilter;
import com.google.inject.util.Providers;
import com.metrink.croquet.hibernate.DataSourceHibernateModule;
import com.metrink.croquet.hibernate.PersistanceUnitHibernateModule;
import com.metrink.croquet.inject.CroquetModule;
import com.metrink.croquet.modules.ManagedModule;

/**
 * This is the main class of Croquet.
 *
 * You must create an instance of this class to setup and run the framework.
 * @param <T> The class used when loading the settings YAML
 */
public class Croquet<T extends Settings> {
    protected static final EnumSet<DispatcherType> DISPATCHER_TYPES =
            EnumSet.of(DispatcherType.ASYNC,
                      DispatcherType.REQUEST,
                      DispatcherType.FORWARD,
                      DispatcherType.INCLUDE);

    private static final Logger LOG = LoggerFactory.getLogger(Croquet.class);

    private volatile CroquetStatus status = CroquetStatus.STOPPED;

    private Injector injector;

    private final List<Class<? extends ManagedModule>> managedModules = new ArrayList<Class<? extends ManagedModule>>();
    private final List<Module> guiceModules = new ArrayList<Module>();

    private final T settings;
    private Server jettyServer;

    /**
     * Constructs a Croquet instance given the arguments pass into the program. The {@link Settings} type T is passed
     * as the first argument to this constructor to avoid ugly reflection.
     *
     * @param clazz the settings class
     * @param args the arguments passed into the program.
     */
    Croquet(final Class<T> clazz, final T settings) {
        this.settings = settings;

        // add the croquet module to our list of guice modules
        guiceModules.add(new CroquetModule<T>(clazz, settings));

        // The user can pick between: no db, a persistence.xml file, or using application.yml file.
        // If you're not using a db, then we skip this.
        // If you're using the application.yml file, then we install the DataSource Hibernate module.
        // If you're using the persistence.xml file, then we install the JpaPersistenceModule.
        if(settings.getDatabaseSettings().getNotUsed()) {
            LOG.info("Not configuring a database. " +
                    "If you get an error about no binding for an EntityManager, you need to configure a database.");
        } else if(settings.getDatabaseSettings().getPersistenceUnit() == null) {
            LOG.info("Using YAML file to configure Hibernate");
            final DataSource dataSource = new DataSource();
            final DatabaseSettings dbSettings = settings.getDatabaseSettings();

            dataSource.setDriverClassName(dbSettings.getDriver());
            dataSource.setUrl(dbSettings.getJdbcUrl());
            dataSource.setUsername(dbSettings.getUser());
            dataSource.setPassword(dbSettings.getPass());
            dataSource.setDbProperties(dbSettings.getProperties());

            guiceModules.add(new DataSourceHibernateModule(dataSource));
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
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("DM_EXIT")
    public void run() {
        status = CroquetStatus.STARTING;

        // create the injector and start the modules
        createInjector();
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

        // configure the Jetty server
        jettyServer = configureJetty(settings.getPort());

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
     * Function used to configure Jetty and return a Server instance.
     * @param port the port to run Jetty on.
     * @return the {@link Server} instance.
     */
    protected Server configureJetty(final int port) {
        final Server server = new Server();
        final ServerConnector connector = new ServerConnector(server);

        // TODO: make all of this configurable
        connector.setIdleTimeout((int)TimeUnit.HOURS.toMillis(1));
        connector.setSoLingerTime(-1);
        connector.setPort(port);

        server.addConnector(connector);

        final ServletContextHandler sch = new ServletContextHandler(ServletContextHandler.SESSIONS);

        // set the injector as an attribute in the context
        sch.setAttribute("guice-injector", injector);

        // prevent the JSESSIONID from getting set via a URL argument
        sch.setInitParameter("org.eclipse.jetty.servlet.SessionIdPathParameterName", "none");

        // add the font mime type by default
        sch.getMimeTypes().addMimeMapping("woff", "application/x-font-woff");

        // if we're using a database, then install the filter
        if(!settings.getDatabaseSettings().getNotUsed()) {
            // setup a FilterHolder for the Guice Persistence
            final FilterHolder persistFilter = new FilterHolder(injector.getInstance(PersistFilter.class));

            // add the filter to the context
            sch.addFilter(persistFilter, "/*", DISPATCHER_TYPES);
        }

        // setup a FilterHolder for WebSockets
        final FilterHolder webSocketFilter = new FilterHolder(Jetty9WebSocketFilter.class);

        // set the app factor as the Guice Web App Factory
        webSocketFilter.setInitParameter("applicationFactoryClassName", GuiceWebApplicationFactory.class.getName());

        // tell the filter to use the injector in the context instead of making a new one
        webSocketFilter.setInitParameter("injectorContextAttribute", "guice-injector");

        // setup the filter mapping
        webSocketFilter.setInitParameter(WicketFilter.FILTER_MAPPING_PARAM, "/*");

        webSocketFilter.setInitParameter("configuration", "deployment");

        // add the filter to the context
        sch.addFilter(webSocketFilter, "/*", DISPATCHER_TYPES);

        // add the default servlet as Guice & Wicket will take care of everything for us
        sch.addServlet(DefaultServlet.class,  "/*");

//        sch.setErrorHandler(new MetrinkErrorHandler());

//        server.setHandler(new ServletContextHandler(ServletContextHandler.SESSIONS));
        server.setHandler(sch);

        return server;
    }
}
