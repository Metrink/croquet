package com.metrink.croquet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.hibernate.dialect.Dialect;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Hibernate and Database settings.
 *
 * This class is not exposed (public) because users should set it via the config file
 * and then never need to touch these settings.
 */
public class DatabaseSettings implements Serializable {

    private static final long serialVersionUID = -2467814019785143850L;

    private boolean notUsed;

    @JsonProperty("persistence-unit")
    private String persistenceUnit;

    @JsonProperty("driver")
    private String driver;

    @JsonProperty("jdbc_url")
    private String jdbcUrl;

    @JsonProperty("user")
    private String user;

    @JsonProperty("pass")
    private String pass;

    // we make this a property as you might switch DBs when you switch configs
    @JsonProperty("dialect")
    private String dialectClass;

    private final List<Class<? extends Serializable>> entities = new ArrayList<>();
    
    private final Properties properties = new Properties();

    DatabaseSettings() {
        notUsed = false;
    }

    DatabaseSettings(final String s) {
        notUsed = true;
    }

    boolean getNotUsed() {
        return notUsed;
    }

    String getPersistenceUnit() {
        return persistenceUnit;
    }

    void setPersistenceUnit(final String persistenceUnit) {
        this.persistenceUnit = persistenceUnit;
    }

    String getDriver() {
        return driver;
    }

    void setDriver(final String driver) {
        this.driver = driver;
    }

    String getJdbcUrl() {
        return jdbcUrl;
    }

    void setJdbcUrl(final String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    String getUser() {
        return user;
    }

    void setUser(final String user) {
        this.user = user;
    }

    String getPass() {
        return pass;
    }

    void setPass(final String pass) {
        this.pass = pass;
    }

    /**
     * Gets the hibernate.dialect class to use.
     * @return the dialect class to use.
     */
    public String getDialectClass() {
        return dialectClass;
    }

    void setDialectClass(final Class<? extends Dialect> dialectClass) {
        this.dialectClass = dialectClass.getCanonicalName();
    }

    /**
     * Returns a list of classes that are mapped as Entities in Hibernate.
     * @return the list of Hibernate entities.
     */
    public List<Class<? extends Serializable>> getEntities() {
        return entities;
    }

    void addEntity(final Class<? extends Serializable> entity) {
        this.entities.add(entity);
    }
    
    /**
     * Returns the database properties.
     * @return the database properties.
     */
    public Properties getProperties() {
        return this.properties;
    }
    
    void addProperty(final String property, final Object value) {
        this.properties.put(property, value);
    }
}
