# User Manual

Croquet is all about configuration. Once it is properly configured, it should "just work."

There are 3 steps to configuring Croquet:

1. Configuring the application's YAML file.
2. Building the ``Croquet`` (or ``CroquetTester``) object using the ``CroquetBuilder``
3. Adding any additional modules to the ``Croquet`` object created in step 2

With these 3 steps you can configure every aspect of Croquet. However, with sane defaults Croquet aims to require as little configuration as possible.

The following sections will explain all the configuration options in detail. If all of this scares you, look at ``croquet-example`` and see the [Getting Started](002-getting-started.html) section of this guide.

## Creating an Application Specific Configuration File

Application specific configuration files can easily be created by extending the ``Settings`` class. Properties are added using the (not very intuative) ``@JsonProperty`` annotation. For example, to add a new setting called ``server`` you would simply add the following code to your settings class:

```
@JsonProperty("server")
private String server;

public String getServer() {
    return server;
}

public void setServer(final String server) {
    this.server = server;
}
```

Croquet takes care of parsing the configuration file and using the ``getServer`` method you can access this value in your code. If you need to ensure the ``server`` value is specified in the configuration file, then add mark it as required: ``@JsonProperty(value = "server", required = true)``.


## Configuring Wicket

Wicket is configured by the configuration file and through code using the ``CroquetBuilder`` class. The following aspects of Wicket can be configured via the configuration file:

- CSS Resources (css_resources)
- JavaScript Resources (js_resources)
- Development vs Deployment

The following aspects of Wicket can be configured through the ``CroquetBuilder`` class:

- Home page
- Login page
- Exception page
- ``WebApplication`` class
- Page mounts
- Resources (both CSS & JavaScript can also be configured through ``CroquetBuilder``)


### Basic Configuration

The only required setting for Wicket is the home page class. Without this being set, ``CroquetBuilder`` cannot build a ``Croquet`` object. The Wicket home page is set via the ``setHomePageClass(Class<? extends WebPage>)`` method of the ``CroquetBuilder``.

An exception page can be set to override Wicket's default page using the ``setExceptionPageClass(Class<? extends WebPage>)`` method. (Note: exception pages are NOT set when in development mode as Wicket's built-in exceptions pages are quite helpful.)

### Creating an Authenticated Web Application

By default Croquet uses the ``CroquetApplication`` class as the Wicket ``WebApplication`` class. ``CroquetApplication`` extends ``AuthenticatedWebApplication`` making it easy to create an authenticated application with Croquet. To do so you need to override the ``getWebSessionClass()`` method of ``CroquetApplication`` to return a properly implemented ``AbstractAuthenticatedWebSession`` class. ([More information on authenticated sessions can be found here.](http://ci.apache.org/projects/wicket/apidocs/6.0.x/org/apache/wicket/authroles/authentication/AbstractAuthenticatedWebSession.html)) Then set this new ``WebApplication`` class via the ``CroquetBuilder``'s ``setWebApplicationClass(final Class<? extends CroquetApplication>)`` method. You'll also want to set the login page via the ``setLoginPageClass(Class<? extends WebPage>)`` method, otherwise it will default to the same page as the home page.

### Configuring Resources

Resources can be configured via **either** the configuration file or via the ``CroquetBuilder`` class. CSS resources are set via the ``css_resource`` property in the YAML file. JavaScript resources are set via the ``js_resources`` property in the YAML file. Both local and hosted files can be specified as resources.

> When specifying hosted resources, prefix them with ``//`` to ensure compatibility with ``http`` and ``https``.

Resources can also be added via the ``CroquetBuilder`` class's ``addResource(final String path, final Class<? extends IResource> resource)`` method. Because the second parameter of this method is a ``Class`` that extends ``IResource``, any [type of resource](http://ci.apache.org/projects/wicket/apidocs/6.0.x/org/apache/wicket/request/resource/IResource.html) can be added.

### Fine Grain Control

Normally you will want to set Croquet into either development mode or deployment mode and forget about the details. However, in some cases it can be useful to have more fine grain control over the different aspects of development vs deployment mode. Croquet enables this through a series of settings controled by the YAML file:

- ``minify_resources``: false in development mode
- ``strip_wicket_tags``: false in development mode
- ``stateless_checker``: true in development mode
- ``wicket_debug_toolbar``: true in development mode

To override the default, simply specify the setting in the YAML file. Be careful as various configurations are a bit hacky.

## Configuring Jetty

Jetty is configured via the application's YAML file. Jetty only has one configuration option: the port it listens for connections on.

By default Jetty is configured with Wicket's ``Jetty9WebSocketFilter``, so you can use Web Sockets in Corquet. Jetty is also configured with a Guice and a Hibernate filter. Hibernate sessions are created with every request (when a database is configured). Jetty is also configured so that you will never see that annoying ``JSESSIONID`` parameter in any of your URLs.

If any of the Jetty configuration options do not meet your needs, they can be changed by overriding the ``configureJetty(final int port)`` method in the ``Croquet`` class.

> We plan on adding additional configurations for Jetty. Please raise an issue for those you'd like to see added first.

## Configuring Hibernate

Hibernate is used in Croquet as the JPA provider, and is setup to be as transparent as possible to the developer. It can be configured via either a ``persistence.xml`` file or via the Croquet YAML file, but **not** both. This makes it easy to transfer existing applications that might already have a ``persistence.xml`` file. If you're starting from scratch though, it's recommended that the Croquet YAML file be used so configurations are in as few places as possible.

> Even though ``EntityManager`` does **NOT** implement ``Serializable``, Croquet wraps the ``EntityManager`` in a proxy object that **does** implement ``Serializable``. What this means is that you should **NOT** mark ``EntityManager`` fields with ``@Inject`` or transient! If you're using FindBugs (which you should be), you'll need to add the ``@edu.umd.cs.findbugs.annotations.SuppressWarnings("SE_BAD_FIELD")`` annotation to the field.

Croquet uses the [Tomcat JDBC Connection Pool](https://tomcat.apache.org/tomcat-8.0-doc/jdbc-pool.html) to provide connections to Hibernate. This connection pool implementation is more performant than the one that comes with Hibernate.

## Configuring Logback

Logback is used for logging in Croquet. If no ``logging`` section is specified in the configuration file, then console logging is enabled using the format string: ``%-5level %date{ISO8601} %c:  %m%n``.

You can override this default by specifying a logging section with 3 subsections:

- ``loggers``: specifies a list of loggers and their levels
- ``file``: specifies the configuration for logging to a file
- ``console``: specifies the configuration for logging to the console (enabled by default)

An example log configuration is shown below:

```
logging:
    loggers:
        "com.metrink.croquet": DEBUG
        "org.hibernate": WARN
        
    file:
        enabled: true
        currentLogFilename: ./croquet.log
        archivedLogFilenamePattern: ./croquet-%d.log.gz
        archiveFileCount: 5
    
    console:
        enabled: false
```

## Adding Guice Modules

Guice is weaved throughout Croquet. There are very few things that are instanciated inside of Croquet without using Guice, and this should be carried through your application as well. With Croquet, Guice constructs every page of your application. This means you can **and should** inject all dependencies into the constructor of each page.

> Note: Avoid field injection at all costs! It makes things **MUCH** harder to unit test. With Croquet, Guice is constructing every page, so there is no reason to use field injection. (See exceptions in the Overrview section.)

Croquet will **only** accpect two types of page constructors:

1. Constructors with only dependencies that are ``@Injected``.
2. Constructors with dependencies that are ``@Injected`` and a ``PageParameters`` parameter.

Because of this, parameters that must be passed from page-to-page should be done via parameters (with the exception of sensitive information). This not only makes your application easier to bookmark (all the needed parameters for a page are in the URL), but it also severly reduces the size of the session, reducing the overall memory of your application. This does come at the cost of having to strictly verify all page parameters as they might have been changed/set by a malicious users. We think the tradeoff is worth it which is why passing objects to pages in Croquet is prohibited.

> Note: You can always store objects in the session; however, this comes at a larger memory cost.

## Adding Managed Modules

Managed modules are modules that are started and stopped when Jetty starts and stops. These modules are very useful for dependencies that need to be started and stopped. Managed modules are added to Croquet via the ``addManagedModule(final Class<? extends ManagedModule> module)`` method. Settings (or other dependencies) should be bound with Guice and injected into the class.

## Testing Croquet Applications

Unit tests are an integral part of good application development. However, web applications are often difficult to unit tests. Croquet attempts to make this easier by providing a ``CroquetTester`` class. This class configures a [WicketTester](http://ci.apache.org/projects/wicket/apidocs/6.x/org/apache/wicket/util/tester/WicketTester.html) instance using the same configuration as your application. To create a ``CroquetTester`` simply call the ``buildTester()`` method of ``CroquetBuilder`` instead of the ``build()`` method. To then obtain the ``WicketTester`` instance, call the ``getTester()`` method of the ``CroquetTester`` class. An example of these steps, taken from ``croquet-examples`` is shown below:

```
        // create the croquet tester object through the builder
        final CroquetTester<CrmSettings> croquetTester =
                CroquetBuilder.create(CrmSettings.class, new String[] { "application.yml" })
                              .setHomePageClass(PeoplePage.class)
                              .addPageMount("/people", PeoplePage.class)
                              .addPageMount("/company", CompanyPage.class)
                              .addHealthCheck("/statuscheck", HealthCheck.class)
                              .setPidFile("croquet.pid")
                              .setSqlDialect(HSQLDialect.class)
                              .addJpaEntity(PeopleBean.class)
                              .addJpaEntity(CompanyBean.class)
                              .buildTester();

        // get the custom settings for the application
        final CrmSettings settings = croquetTester.getSettings();

        // add in our Guice module
        croquetTester.addGuiceModule(new CrmModule(settings));

        // add in a managed module
        croquetTester.addManagedModule(EmailModule.class);

        // get the tester
        tester = croquetTester.getTester();

```

Using the ``WicketTester`` you can easily ensure that your page will load:

```
    @Test
    public void test() {
        tester.startPage(CompanyPage.class);

        tester.assertRenderedPage(CompanyPage.class);
    }
```

For more information on using ``WicketTester`` see the [Wicket documentation](http://ci.apache.org/projects/wicket/apidocs/6.x/org/apache/wicket/util/tester/WicketTester.html).

> There is a known bug with the ``CroquetTester`` when using a ``persistence.xml`` file. The problem arises because a single instance of an [EntityManagerFactoryRegistry](https://docs.jboss.org/hibernate/orm/4.0/javadocs/org/hibernate/ejb/internal/EntityManagerFactoryRegistry.html) is kept across unit tests. When a second unit test is run, the ``EntityManagerFactory`` is registered twice causing an issue. Using the YAML file does not have this problem. If you have a solution to this problem, we welcome all pull requests.
