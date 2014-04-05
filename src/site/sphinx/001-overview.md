# Overview

Croquet is a combination of [Wicket](http://wicket.apache.org/), [Jetty](http://www.eclipse.org/jetty/), [Hibernate](http://hibernate.org/), and [Guice](https://code.google.com/p/google-guice/). The goal of Croquet is to make it super easy to create a web application using Wicket by taking the hassle out of combining all the other pieces needed to get started with Wicket. For those familiar with [DropWizard](https://dropwizard.github.io/dropwizard/) (a project we've emulated), Croquet is to Wicket as DropWizard is to [Jersey](https://jersey.java.net/).

To get started with Croquet see the [Getting Started section](002-getting-started.html). For more in-depth documentation, see the [User Manual section](003-user-manual.html).

## Presentation

Croquet was first introduced during an ApacheCon presentation. The [slides](https://docs.google.com/presentation/d/1m3jdbpYoSBOCPz8Wes9mPvhf8TLp_3dndj_gW08iFL8/edit?usp=sharing) from the "launch" [presentation](http://sched.co/1pav4JP) of Croquet give a good overview of what Croquet is and how it was built. 


## Source

Croquet is open source and can be found on [GitHub](http://www.github.com/metrink/croquet). Please file any issues you find, and submit any patches you have through GitHub.


## Philosophies

To make developing Wicket applications easier, Croquet adopts a few philosophies. We try to strike a balance between jamming our ideas down people's throats and providing no guidance. Below are the philosophies adopted by Croquet:

### Dependency Injection

Croquet **strongly** favors dependency injection via the constructor. Because Wicket requires that all pages be serializable, dependency injection becomes more difficult than in other applications. For dependencies that are injected into pages that are **not** serializable, Croquet relies upon Wicket's IoC extension to construct proxy objects allowing the instances to be serialized. (You should always try and make all dependencies serializable to prevent this overhead.) As a consequence of this, Croquet requires that all dependencies that are **not** serializable be injected via field injection. However, it is recommended that these dependencies also be injected via the constructor. (How do you do that, just mark both the constructor and field with ``@Inject``, then call ``Injector.get().inject(this)`` in the constructor.)

So why not just use field injection for everything? Field injection makes writing unit tests more difficult, and after all unit tests are one of the main reasons to use dependency injection in the first place. To aid in writing unit tests, Croquet is built so that every page inject its dependencies via a single constructors. For example, you would have the following:

```
public class MyPage extends WebPage {
  private final SerializableDep dep1;  
  
  @Inject // Can easily provide dependencies when unit testing
  public MyPage(final SerializableDep dep1, final PageParameters params) {
    this.dep1 = dep1;
  }
}
```

Everything, even the ``PageParameters`` will be injected by Guice. If your page needed a dependency that was not serializable, then you would have to mark the field appropriately, and call ``Injector.get().inject(this)``. An example of this is shown below:

```
public class MyPage extends WebPage {
  private final SerializableDep dep1;
  @Inject private final transient NotSerializableDep dep2;
  
  @Inject // Can easily provide dependencies when unit testing
  public MyPage(final SerializableDep dep1, final NotSerializableDep dep2, final PageParameters params) {
    Injector.get().inject(this); // only needed for NotSerializableDep
    this.dep1 = dep1;
    this.dep2 = dep2;
  }
}
```

With all dependencies injected via the constructor, it is very easy to make mocks (checkout [Mockito](https://code.google.com/p/mockito/)) for your dependencies and pass them into the page via the constructor. Combine this with Croquet's ``CroquetTester`` class, and ensure every page you have can be rendered via a unit test becomes a breeze.

### Passing State

Wicket allows state to be passed from page-to-page via three methods:

1. By constructing objects and passing them into a new instance of a page.
2. Through query parameters in the URL using the ``PageParameters`` class.
3. By manually setting the state in the session or some other backend database.

We feel strongly that option 1 is a bad idea. The reason for this is that when you create objects and pass them via the constructor you are really using option 3 unknowingly. This can unnecessarily bloat the size of the session without the developer considering the implications. By forcing the developer to manually set the state in the session, we find that more care is given to the size of the state being set. Option 2, when appropriate (think security or size) is the best option. Because of this, Croquet supports and promotes the use of state being passed via query parameters in the URL. To accomplish this, Croquet allows for the "default" constructor to take a single parameter of type ``PageParameters``. This parameter will be automatically injected by Croquet when the page is constructed.


### Development Mode vs Deployment Mode

Wicket natively provides two runtime modes: deployment and development. Deployment should **always** be used in production environments. There are numerous optimizations that Wicket employs when running in deployment mode. Croquet furthers these optimizations by doing things like minifying JavaScript and CSS; reducing the verbosity of logs, and ensuring state checks (see [StatelessChecker](http://ci.apache.org/projects/wicket/apidocs/6.0.x/org/apache/wicket/devutils/stateless/StatelessComponent.html)) are disabled.

Development mode on the other hand provides all sorts of useful information about your applications. Croquet enables more verbose logging, SQL statistics, SQL statements, raw JavaScript and CSS, enables state checks, and adds the [debug bar](http://ci.apache.org/projects/wicket/apidocs/6.0.x/org/apache/wicket/devutils/debugbar/DebugBar.html). Croquet's philosophy is that when running in development mode as much debugging information as possible should be provided to the developer.
