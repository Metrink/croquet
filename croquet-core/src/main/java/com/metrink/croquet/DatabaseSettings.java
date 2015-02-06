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

    /*
     * Below are the Tomcat JDBC Connection Pool attributes
     * See: http://tomcat.apache.org/tomcat-7.0-doc/jdbc-pool.html#Attributes
     */
    //CHECKSTYLE:OFF magic values
    @JsonProperty("maxActive")
    private Integer maxActive = 100;

    @JsonProperty("maxIdle")
    private Integer maxIdle = 10;

    @JsonProperty("minIdle")
    private Integer minIdle = 2;

    @JsonProperty("initialSize")
    private Integer initialSize = 10;
    //CHECKSTYLE:ON

    @JsonProperty("testOnBorrow")
    private Boolean testOnBorrow = Boolean.TRUE;

    @JsonProperty("testOnReturn")
    private Boolean testOnReturn = Boolean.FALSE;

    @JsonProperty("testWhileIdle")
    private Boolean testWhileIdle = Boolean.TRUE;

    @JsonProperty("validationQuery")
    private String validationQuery = "select 1";

    @JsonProperty("logValidationErrors")
    private Boolean logValidationErrors = Boolean.TRUE;

    @JsonProperty("zeroDateTimeBehavior")
    private String zeroDateTimeBehavior = "convertToNull";

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
        // kinda hack, but no issue with jamming it in there every time :-)
        this.properties.put("zeroDateTimeBehavior", getZeroDateTimeBehavior());
        return this.properties;
    }

    void addProperty(final String property, final Object value) {
        this.properties.put(property, value);
    }

    /**
     * Get the max active connections.
     * @return the max active connections, defaults to 100.
     */
    public int getMaxActive() {
        return maxActive;
    }

    void setMaxActive(final Integer maxActive) {
        this.maxActive = maxActive;
    }

    /**
     * Gets the max number of idle connections.
     * @return max number of idle connections, defaults to 10.
     */
    public int getMaxIdle() {
        return maxIdle;
    }

    void setMaxIdle(final Integer maxIdle) {
        this.maxIdle = maxIdle;
    }

    /**
     * Gets the min idle connections.
     * @return min idle connections, defaults to 2.
     */
    public int getMinIdle() {
        return minIdle;
    }

    void setMinIdle(final Integer minIdle) {
        this.minIdle = minIdle;
    }

    /**
     * Gets the initial size of the connection pool.
     * @return initial size of the connection pool, defaults to 10.
     */
    public int getInitialSize() {
        return initialSize;
    }

    void setInitialSize(final Integer initialSize) {
        this.initialSize = initialSize;
    }

    /**
     * If connections should be tested when borrowed.
     * @return true if connections should be tested on borrow, defaults to true.
     */
    public boolean getTestOnBorrow() {
        return testOnBorrow;
    }

    void setTestOnBorrow(final Boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }

    /**
     * If connections should be tested when returned.
     * @return true if connections should be tested when returned, defaults to false.
     */
    public boolean getTestOnReturn() {
        return testOnReturn;
    }

    void setTestOnReturn(final Boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
    }

    /**
     * Gets true if connections should be tested while idle.
     * @return true if connections should be tested while idle, defaults to true.
     */
    public boolean getTestWhileIdle() {
        return testWhileIdle;
    }

    void setTestWhileIdle(final Boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
    }

    /**
     * Gets the query to use for validation.
     * @return the query to use for validation, defaults to "select 1".
     */
    public String getValidationQuery() {
        return validationQuery;
    }

    void setValidationQuery(final String validationQuery) {
        this.validationQuery = validationQuery;
    }

    /**
     * Should validation errors be logged.
     * @return true if validation errors should be logged, defaults to true.
     */
    public Boolean getLogValidationErrors() {
        return logValidationErrors;
    }

    void setLogValidationErrors(final Boolean logValidationErrors) {
        this.logValidationErrors = logValidationErrors;
    }

    /**
     * Gets the behavior of a zero DateTime.
     * @return behavior of a zero DateTime.
     */
    public String getZeroDateTimeBehavior() {
        return zeroDateTimeBehavior;
    }

    void setZeroDateTimeBehavior(final String zeroDateTimeBehavior) {
        this.zeroDateTimeBehavior = zeroDateTimeBehavior;
    }
}
