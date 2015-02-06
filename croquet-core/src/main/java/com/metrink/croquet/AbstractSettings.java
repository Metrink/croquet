package com.metrink.croquet;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The base set of settings used by both Wicket & Rest.
 */
public abstract class AbstractSettings implements Serializable {
    private static final long serialVersionUID = -2385834291456376380L;
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSettings.class);

    private static final int DEFAULT_PORT = 8080;

    @JsonProperty("development")
    private Boolean development = false;

    @JsonProperty("port")
    private Integer port = DEFAULT_PORT;

    @JsonProperty("pid_file")
    private String pidFile;

    @JsonProperty("logging")
    private LoggingSettings loggingSettings = new LoggingSettings();

    @JsonProperty("db")
    private DatabaseSettings dbSettings;


    /**
     * Perform post de-serialization modification of the Settings.
     */
    protected final void initialize() {
        // Enabling development mode forces these settings. This is somewhat inelegant, because to configure one of
        // these values differently will require disabling development mode and manually configure the remaining values.
        if (development) {
            loggingSettings.getLoggers().put("org.hibernate.SQL", Level.DEBUG);
            loggingSettings.getLoggers().put("com.metrink.croquet", Level.DEBUG);
        }

        // call the class's init method
        init();
    }

    /**
     * Perform post de-serialization modification of the Settings.
     */
    protected abstract void init();

    /**
     * Helper method used to obtain a class from a fully qualified class name. If the value is null or an exception is
     * thrown, return the default result instead.
     * @param className the fully qualified class name to try
     * @param defaultResult the nullable default result
     * @param <T> the class type
     * @return the class or null
     */
    @SuppressWarnings("unchecked")
    protected <T> Class<T> getClassOrDefault(final String className, final Class<T> defaultResult) {
        try {
            return className == null
                    ? defaultResult
                    : (Class<T>)Class.forName(className);
        } catch (final ClassNotFoundException e) {
            LOG.error("ClassNotFoundException for {} - defaulting to {}", className, defaultResult);
            return defaultResult;
        }
    }

    /**
     * In development mode?
     * Defaults to false to avoid accidental deployment of development mode in production.
     * @return true if in development mode
     */
    public Boolean getDevelopment() {
        return development;
    }

    /**
     * Get the TCP port which will be bound to Jetty.
     * @return the port
     */
    public int getPort() {
        return port;
    }


    /**
     * Get the {@link DatabaseSettings}.
     * @return the {@link DatabaseSettings}.
     */
    public DatabaseSettings getDatabaseSettings() {
        return dbSettings;
    }

    /**
     * Get the {@link LoggingSettings}.
     * @return the {@link LoggingSettings}.
     */
    public LoggingSettings getLoggingSettings() {
        return loggingSettings;
    }

    /**
     * Get pidFile.
     * @return the pidFile
     */
    public String getPidFile() {
        return pidFile;
    }

    /**
     * Set pidFile.
     * @param pidFile the pidFile to set
     */
    public void setPidFile(final String pidFile) {
        this.pidFile = pidFile;
    }

}
