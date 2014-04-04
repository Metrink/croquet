### Croquet

Croquet is a combination of [Wicket](http://wicket.apache.org/), [Jetty](http://www.eclipse.org/jetty/), [Hibernate](http://hibernate.org/), and [Guice](https://code.google.com/p/google-guice/). Croquet is to Wicket as [DropWizard](https://dropwizard.github.io/dropwizard/) is to [Jersey](https://jersey.java.net/).


### Very Quick Start

You can find the complete [Croquet documentation here](http://croquet.metrink.com/), and [JavaDocs here](http://croquet.metrink.com/apidocs/). However, if you just want to jump into it, this is all you need to get going:

1) Create a settings.yml file with your configuration.

2) Create a ``Croquet`` instance.
```
final Croquet<Settings> croquet =
        CroquetBuilder.create(args)
                      .setHomePageClass(MyHomePage.class)
                      .addPageMount("/blah", BlahPage.class)
                      .addHealthCheck("/statuscheck", HealthCheck.class)
                      .pidFile("croquet.pid")
                      .build();
```

3) Call ``run()`` on the ``Croquet`` instance.



