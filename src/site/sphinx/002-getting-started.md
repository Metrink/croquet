# Getting Started

This section will walk through the croquet-example, a simple CRM site to show off the various features of Croquet. For more detailed information, see the [User Manual section](003-user-manual.html).

## Croquet Dependency

The easiest way to get started with Croquet is to use [Maven](http://maven.apache.org/) and simply add the ``croquet-core`` dependency. It can be added to your project like this:

```xml
<dependency>
  <groupId>com.metrink.croquet</groupId>
  <artifactId>croquet-core</artifactId>
  <version>0.1.0</version>
</dependency>
```

That's it! Croquet will bring in the proper versions of Wicket, Jetty, Hibernate, Guice, and a few other common libraries.

## Configuration

Croquet is configured through a YAML file and via code. This may seem confusing at first as there are two places to look for configurations; however, we felt that some configurations are just easier (for example when needing to specify full classpaths) through code. Also, Guice is inherently configured via code, and we didn't want to reinvent the wheel by making Guice configurable via a file.

Why have a file at all then? Simply because when you're changing environments (from staging to production for example), it's very easy to change the file Croquet runs with. So we've put those things you might want to change from environment to environment (database, log level, etc) into the YAML file, and those things that aren't likely to change (home page, Guice modules) are configured via code.

### Configuration File

There are 3 main sections to the YAML file: top-level, ``db``, ``logging``. The top-level section configures Wicket and Jetty. The ``db`` section configures Hibernate. Finally, the ``logging`` section configures logback.

You can provide custom settings in the configuration file by specifying a settings class. You can see this in the ``croquet-example``'s ``application.yml`` file:

```
# custom settings, see CrmSettings.java
current-user: Joe User
mail-server: mail.example.com
mail-user: test-user
mail-pass: test-pass

css_resources:
    - style.css
    - style-2.css
    - //netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css

js_resources:
    - script.js
    - script-2.js
    - //netdna.bootstrapcdn.com/bootstrap/3.1.1/js/bootstrap.min.js

port: 8080

pid_file: croquet.pid

development: true

db:
    # persistence-unit: croquet-example # uncomment this, and comment the lines below to use a persistence.xml file
    driver: org.hsqldb.jdbcDriver
    jdbc_url: jdbc:hsqldb:file:db/crm
    user: SA
    pass:
 
logging:
    loggers:
        "com.metrink.croquet": DEBUG
        "org.hibernate": WARN
    file:
        enabled: true
        currentLogFilename: ./croquet.log
        archivedLogFilenamePattern: ./croquet-%d.log.gz

```

### CroquetBuilder

The rest of Croquet is configured via code using the ``CroquetBuilder`` class. This class uses the [Builder design pattern](http://en.wikipedia.org/wiki/Builder_pattern) to construct a ``Croquet`` object. The ``Croquet`` object has additional methods to configure how Croquet runs. Why the two-step building/configuring? Because we want the user to have access to their custom settings from the configuration file. The ``CroquetBuilder`` class parses the configuration file returning a ``Croquet`` object that can provide the custom settings. This is often useful when constructing Guice modules or Managed modules (see below).

The ``croquet-example``'s ``Main`` class shows how the ``CroquetBuilder`` class is used to configure a ``Croquet`` object:

```
        final Croquet<CrmSettings> croquet =
                CroquetBuilder.create(CrmSettings.class, args)
                              .setHomePageClass(PeoplePage.class)
                              .addPageMount("/people", PeoplePage.class)
                              .addPageMount("/company", CompanyPage.class)
                              .addHealthCheck("/statuscheck", HealthCheck.class)
                              .setPidFile("croquet.pid")
                              .setSqlDialect(HSQLDialect.class)
                              .addJpaEntity(PeopleBean.class)
                              .addJpaEntity(CompanyBean.class)
                              .build();
```

## Adding Modules

Croquet supports two types of modules: [Guice modules](http://google-guice.googlecode.com/git/javadoc/com/google/inject/Module.html) and [ManagedModule](http://croquet.metrink.com/apidocs/com/metrink/croquet/modules/ManagedModule.html)s. These modules are added to the ``Croquet`` instance created by the ``CroquetBuilder``. You can access any custom settings via the ``Croquet.getSettings()`` method. This is often useful when creating modules.

### Guice Modules

Guice modules are used to configure additional bindings in Guice. Any modules that extends ``Module`` can be added to Croquet via the ``addGuiceModule()`` method. The example adds 1 binding and creates 2 [AssistedInject](https://code.google.com/p/google-guice/wiki/AssistedInject) factories. For more information on creating Guice modules, see the [Guice documentation](https://code.google.com/p/google-guice/wiki/GettingStarted).

```
    @Override
    protected void configure() {
        bind(String.class).annotatedWith(Names.named("current-user")).toInstance("Metrink User");

        install(new FactoryModuleBuilder().build(PeopleDataProviderFactory.class));
        install(new FactoryModuleBuilder().build(new TypeLiteral<GenericDataProviderFactory<CompanyBean>>(){}));
    }
```

### Managed Modules

Managed modules are classes that extend the ``ManagedModule`` interface provided by Croquet. These classes are started after Jetty has started and are shutdown after Jetty has shutdown. These modules are useful for dependencies (an HTTP Client for example) your application might have that require startup and shutdown.

> If there are managed modules that you find yourself implementing (or copying) over-and-over again, create a pull request with your code and we'll try to get it into the next release of Croquet.

The ``Croquet`` class only adds Managed modules via the ``public void addManagedModule(final Class<? extends ManagedModule> module)`` method. You might be wondering how to pass settings into a Managed when the method only accepts a ``Class<? extends ManagedModule>`` parameter. The answer is via a Guice module. You should bind any settings that need to be passed into a Managed module in your Guice module. You can use the ``@Named`` [annotation](http://code.google.com/p/google-guice/wiki/BindingAnnotations) to aid in disambiguating common types like strings. Or you can simply inject your entire custom settings class:

```
    @Inject
    public EmailModule(final CrmSettings settings) {
        this.settings = settings;
    }
```

## Building Your Croquet Application

The easiest way to build a Croquet application is by bundling everything together into one big JAR file. This is most easily done with the [Maven Shade plugin](http://maven.apache.org/plugins/maven-shade-plugin/). The ``croquet-example``'s ``pom.xml`` file show how to create such a JAR file:

```
    <plugin>
        <artifactId>maven-shade-plugin</artifactId>
        <version>2.2</version>
        <executions>
            <execution>
                <phase>package</phase>
                <goals>
                    <goal>shade</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
```

You might find you need to add transformers to the shade configuration to remove any signatures from packages.

> There can be issues with using the Maven Shade plugin when multiple packages pull in different versions of common dependencies. One way around this is to use the exclude directive in your pom. Another avenue is to use the [Maven Dependency plugin](http://maven.apache.org/plugins/maven-dependency-plugin/) in concert with the [Maven JAR plugin](http://maven.apache.org/plugins/maven-jar-plugin/). This is a bit more tricky, so try and get Maven Shade working when possible.

## Running Your Croquet Application

Running a Croquet application that is bundled into a single JAR is as easy as:

```
java -jar croquet-example.jar application.yml
```

Because everything is packaged into a single JAR, you do not need to fuss with classpaths. The desired configuration file is passed as a command line argument to your Croquet application. This makes it _very_ easy to change between configurations; simply pass a different YAML file.

When your Croquet application is up and running, it will drop a PID file (if specified in code or config file) to let you know everything is working. This is useful for tools like [Monit](http://mmonit.com/monit/) to ensure your process is always running.

