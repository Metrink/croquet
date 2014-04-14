package com.metrink.croquet.wicket;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentMap;

import org.apache.wicket.Application;
import org.apache.wicket.IPageFactory;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.lang.Generics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.ConfigurationException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.Assisted;
import com.metrink.croquet.Settings;

/**
 * An implementation of an {@link IPageFactory} that uses Guice to create the page instances.
 *
 * Pages can <b>only</b> have one of two constructors:
 * <ul>
 * <li>A constructor where all dependencies are injected by Guice.</li>
 * <li>A constructor with a PageParameters argument and all dependencies are injected by Guice.</li>
 * </ul>
 */
@Singleton
public class GuicePageFactory implements IPageFactory {
    public static final Logger LOG = LoggerFactory.getLogger(GuicePageFactory.class);

    private final Injector injector;
    private final Settings settings;

    private final ConcurrentMap<String, Boolean> pageToBookmarkableCache = Generics.newConcurrentHashMap();

    /**
     * Constructs the {@link GuicePageFactory} with the injector to create page instances.
     * @param injector the injector to use to create page instances.
     */
    @Inject
    public GuicePageFactory(final Injector injector) {
        this.injector = injector;
        this.settings = injector.getInstance(Settings.class);
    }

    @Override
    public <C extends IRequestablePage> C newPage(final Class<C> pageClass) {
        LOG.debug("Creating new {} page without parameters", pageClass.getName());

        if (!Application.get().getSecuritySettings().getAuthorizationStrategy().isInstantiationAuthorized(pageClass)) {
            throw new RestartResponseAtInterceptPageException(settings.getLoginPageClass());
        }

        try {
            final C pageInstance = injector.createChildInjector(new AbstractModule() {

                @Override
                protected void configure() {
                    bind(PageParameters.class).toInstance(new PageParameters());
                }

            }).getInstance(pageClass);

            return pageInstance;
        } catch(final ConfigurationException e) {
            LOG.debug("Could not create page {} through Guice, trying manually: {}", pageClass, e.getMessage());

            return createOrThrow(pageClass, null);
        }
    }

    @Override
    public <C extends IRequestablePage> C newPage(final Class<C> pageClass, final PageParameters parameters) {
        LOG.debug("Creating new {} page with parameters: {}", pageClass.getName(), parameters);

        try {
            final C pageInstance = injector.createChildInjector(new AbstractModule() {

                @Override
                protected void configure() {
                    bind(PageParameters.class).toInstance(parameters);
                }

            }).getInstance(pageClass);

            return pageInstance;
        } catch(final ConfigurationException e) {
            LOG.debug("Could not create page {} through Guice, trying manually", pageClass);

            return createOrThrow(pageClass, parameters);
        }
}

    private <C extends IRequestablePage> C createOrThrow(final Class<C> pageClass, final PageParameters parameters) {
        try {
            if(parameters == null) {
                return pageClass.getConstructor().newInstance();
            } else {
                return pageClass.getConstructor(PageParameters.class).newInstance(parameters);
            }
        } catch (final InstantiationException |
                 IllegalAccessException |
                 IllegalArgumentException |
                 InvocationTargetException |
                 NoSuchMethodException |
                 SecurityException e) {
            throw new WicketRuntimeException("Error creating page " + pageClass, e);
        }
    }

    @Override
    public <C extends IRequestablePage> boolean isBookmarkable(final Class<C> pageClass) {
        Boolean result = pageToBookmarkableCache.get(pageClass.getName());

        if(result == null) {

            // go through all of the constructors looking for the @Assisted annotation
            for(final Constructor<?> constructor:pageClass.getConstructors()) {
                // if we find a constructor that is NOT marked with @Inject,
                // then we're doing it wrong!

                boolean foundInject = false;

                for(final Annotation annotation:constructor.getDeclaredAnnotations()) {
                    if(annotation.annotationType().equals(Inject.class)) {
                        foundInject = true;
                        break;
                    }
                }

                if(foundInject == false) {
                    result = false;
                    break;
                }

                // go through the annotations on the parameters
                for(final Annotation[] params:constructor.getParameterAnnotations()) {
                    for(final Annotation annotation:params) {
                        if(annotation.annotationType().equals(Assisted.class)) {
                            result = false;
                            break;
                        }
                    }
                }
            }

            // if result is still null by here,
            // then all the constructors are OK
            if(result == null) {
                result = true;
            }

            LOG.debug("Is {} bookmarkable? {}", pageClass.getName(), result);

            pageToBookmarkableCache.put(pageClass.getName(), result);
        }

        return result;
    }
}
