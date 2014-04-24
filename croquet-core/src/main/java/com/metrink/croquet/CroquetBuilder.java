package com.metrink.croquet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.persistence.Entity;

import org.apache.wicket.authroles.authentication.AbstractAuthenticatedWebSession;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.resource.IResource;
import org.hibernate.dialect.Dialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.metrink.croquet.LoggingSettings.LogFile;
import com.metrink.croquet.logging.CroquetLoggingFactory;
import com.metrink.croquet.wicket.CroquetApplication;

/**
 * Class used to build the immutable Croquet instance.
 * @param <T> the type of the Settings class.
 */
public class CroquetBuilder<T extends Settings> {
    private static final Logger LOG = LoggerFactory.getLogger(CroquetBuilder.class);

    private final T settings;
    private final Class<T> settingsClass;

    private CroquetBuilder(final Class<T> settingsClass, final T settings) {
        this.settingsClass = settingsClass;
        this.settings = settings;
    }

    /**
     * Creates an instance of the {@link CroquetBuilder} that uses the default {@link Settings} class.
     * @param args the command line args.
     * @return an instance of the {@link CroquetBuilder}.
     */
    public static CroquetBuilder<Settings> create(final String[] args) {
        return CroquetBuilder.create(Settings.class, args);
    }

    /**
     * Creates an instance of the {@link CroquetBuilder} given the command line args.
     * @param settingsClass the Class of the Settings class.
     * @param args the command line args.
     * @return an instance of the {@link CroquetBuilder}.
     * @param <S> the type of the settings class.
     */
    public static <S extends Settings> CroquetBuilder<S> create(final Class<S> settingsClass, final String[] args) {
        if (args.length < 1) {
            System.err.println("Configuration YAML file not provided");
            System.exit(-1);
        }

        // parse the arguments and configuration file here
        final String filename = args[0];
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        final S configuration;
        try {
            configuration = mapper.readValue(new File(filename), settingsClass);
            configuration.initialize();

        } catch (final JsonParseException e) {
            LOG.error("JsonParseException: {}", e.getMessage());
            throw errorAndDie(filename, e.getMessage());
        } catch (final JsonMappingException e) {
            LOG.error("JsonMappingException: {}", e.getMessage());
            throw errorAndDie(filename, e.getMessage());
        } catch (final IOException e) {
            LOG.error("IOException: {}", e.getMessage());
            throw errorAndDie(filename, e.getMessage());
        }

        new CroquetLoggingFactory().configureLogging(configuration.getLoggingSettings());

        return new CroquetBuilder<S>(settingsClass, configuration);
    }

    private static IllegalStateException errorAndDie(final String filename, final String reason) {
        System.err.println("Unable to load '" + filename + "' because: " + reason);
        System.exit(-1);
        // The only reason this returns an exception is so that we can throw on the same line as we call. Otherwise,
        // the constructor will complain about the uninitialized settings field.
        return new IllegalStateException("Unreachable");
    }

    /**
     * Builds the {@link Croquet} object.
     * @return the newly built {@link Croquet} object.
     */
    public Croquet<T> build() {
        checkDbSettings();
        checkLoggingSettings();

        return new Croquet<T>(settingsClass, settings);
    }

    /**
     * Builds a {@link CroquetTester} object.
     * @return the newly built {@link CroquetTester} object.
     */
    public CroquetTester<T> buildTester() {
        checkDbSettings();
        checkLoggingSettings();

        return new CroquetTester<T>(settingsClass, settings);
    }

    /**
     * Runs checks over the database settings before building a {@link Croquet} instance.
     */
    protected void checkDbSettings() {
        final DatabaseSettings db = settings.getDatabaseSettings();

        if(db == null) {
            throw new RuntimeException("You must specify db settings." +
                    " If you don't need a database, simply leave the section blank.");
        }

        // CHECKSTYLE:OFF conditional complexity too large
        if(db.getPersistenceUnit() != null &&
          (db.getDriver() != null ||
           db.getDialectClass() != null ||
           db.getJdbcUrl() != null ||
           db.getUser() != null ||
           db.getPass() != null)) {
        // CHECKSTYLE:ON
            throw new IllegalStateException("Cannot set persist-unit with any other database settings");
        }
    }

    /**
     * Runs some checks over the log settings before building a {@link Corquet} instance.
     */
    protected void checkLoggingSettings() {
        final LoggingSettings logSettings = settings.getLoggingSettings();
        final LogFile logFileSettings = logSettings.getLogFile();

        if(logFileSettings.getCurrentLogFilename() != null &&
           logFileSettings.isEnabled() == false) {
            LOG.warn("You specified a log file, but have it disabled");
        }

        if(logFileSettings.isEnabled() &&
           logFileSettings.getCurrentLogFilename() == null) {
            throw new IllegalStateException("You enabled logging to a file, but didn't specify the file name");
        }
    }

    /**
     * Sets the class of the Wicket application.
     * @param application the WebApplication class for Wicket.
     * @return the {@link CroquetBuilder}.
     */
    public CroquetBuilder<T> setWebApplicationClass(final Class<? extends CroquetApplication> application) {
        settings.setWebApplicationClass(application);
        return this;
    }

    /**
     * Sets the {@link AbstractAuthenticatedWebSession} for the application.
     * @param wicketSessionClass the {@link AbstractAuthenticatedWebSession} class.
     * @return the {@link CroquetBuilder}.
     */
    public CroquetBuilder<T> setWicketSessionClass(final Class<? extends AbstractAuthenticatedWebSession> wicketSessionClass) {
        settings.setWicketSessionClass(wicketSessionClass);
        return this;
    }

    /**
     * Sets the class to use as the home page.
     * @param homePage the {@link WebPage} to use as the home page.
     * @return the {@link CroquetBuilder}.
     */
    public CroquetBuilder<T> setHomePageClass(final Class<? extends WebPage> homePage) {
        settings.setHomePageClass(homePage);
        return this;
    }

    /**
     * Sets the class to use as the login page.
     * @param loginPage the {@link WebPage} to use as the login page.
     * @return the {@link CroquetBuilder}.
     */
    public CroquetBuilder<T> setLoginPageClass(final Class<? extends WebPage> loginPage) {
        settings.setLoginPageClass(loginPage);
        return this;
    }

    /**
     * Sets the class to use as the exception page.
     * @param exceptionPage the {@link WebPage} to use as the exception page.
     * @return the {@link CroquetBuilder}.
     */
    public CroquetBuilder<T> setExceptionPageClass(final Class<? extends WebPage> exceptionPage) {
        settings.setExceptionPageClass(exceptionPage);
        return this;
    }

    /**
     * Adds a {@link WebPage} to the list of mounts.
     * @param path the location to mount the page.
     * @param page the page to mount.
     * @return the {@link CroquetBuilder}.
     */
    public CroquetBuilder<T> addPageMount(final String path, final Class<? extends WebPage> page) {
        settings.addPageMount(path, page);
        return this;
    }

    /**
     * Adds a health check resource to a particular mount.
     * @param path the path
     * @param resource the resource to mount
     * @return the {@link CroquetBuilder}.
     */
    public CroquetBuilder<T> addHealthCheck(final String path, final Class<? extends IResource> resource) {
        settings.addResourceMount(path, resource);
        return this;
    }

    /**
     * Adds a {@link IResource} to the list of resources.
     * @param path the location to mount the page.
     * @param resource the resource to mount.
     * @return the {@link CroquetBuilder}.
     */
    public CroquetBuilder<T> addResource(final String path, final Class<? extends IResource> resource) {
        settings.addResourceMount(path, resource);
        return this;
    }

    /**
     * Adds a JPA entity to Croquet.
     * @param entity the entity to add.
     * @return the {@link CroquetBuilder}.
     */
    public CroquetBuilder<T> addJpaEntity(final Class<? extends Serializable> entity) {
        // check to ensure the class has the @Entity annotation
        if(entity.getAnnotation(Entity.class) == null) {
            throw new IllegalArgumentException("Only classes marked with @Entity can be added");
        }

        settings.getDatabaseSettings().addEntity(entity);
        return this;
    }

    /**
     * Adds a property to the database configuration.
     *
     * <b>Only used when configuring the DB via the YAML file.</b>
     * @param property the DB property to set.
     * @param value the value of the property.
     * @return the {@link CroquetBuilder}.
     */
    public CroquetBuilder<T> addDbProperty(final String property, final Object value) {
        settings.getDatabaseSettings().addProperty(property, value);
        return this;
    }

    /**
     * Sets the hibernate.dialect class.
     * @param dialectClass the dialect class to use.
     * @return the {@link CroquetBuilder}.
     */
    public CroquetBuilder<T> setSqlDialect(final Class<? extends Dialect> dialectClass) {
        settings.getDatabaseSettings().setDialectClass(dialectClass);
        return this;
    }

    /**
     * Sets the name of the PID file to drop on Linux.
     *
     * @param pidFilename the pid filename
     * @return the {@link CroquetBuilder}
     */
    public CroquetBuilder<T> setPidFile(final String pidFilename) {
        settings.setPidFile(pidFilename);
        return this;
    }
}
