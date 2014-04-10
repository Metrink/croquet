package com.metrink.croquet.wicket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.IInitializer;
import org.apache.wicket.IPageFactory;
import org.apache.wicket.Page;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.authroles.authentication.AbstractAuthenticatedWebSession;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.core.request.handler.PageProvider;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.devutils.debugbar.DebugBar;
import org.apache.wicket.devutils.debugbar.InspectorDebugPanel;
import org.apache.wicket.devutils.debugbar.PageSizeDebugPanel;
import org.apache.wicket.devutils.debugbar.SessionSizeDebugPanel;
import org.apache.wicket.devutils.debugbar.VersionDebugContributor;
import org.apache.wicket.devutils.stateless.StatelessChecker;
import org.apache.wicket.extensions.Initializer;
import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IPackageResourceGuard;
import org.apache.wicket.markup.html.SecurePackageResourceGuard;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.AbstractRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.UrlResourceReference;
import org.apache.wicket.resource.loader.InitializerStringResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.metrink.croquet.Settings;

/**
 * An AuthenticatedWebApplication class that implements sane defaults.
 *
 * You should extend this class if you wish to actually use authentication as
 * this class simply allows anyone to hit any page. To implement authentication
 * follow the directions found on the <a href="http://wicket.apache.org/learn/projects/authroles.html">Wicket site</a>.
 *
 */
public class CroquetApplication extends AuthenticatedWebApplication {
    private static final Logger LOG = LoggerFactory.getLogger(CroquetApplication.class);

    private final IPageFactory pageFactory;
    private final Settings settings;

    /**
     * Constructs the CroquetApplication.
     *
     * Settings are injected via Guice and this application is run
     * by Jetty & Wicket.
     * @param pageFactory the page factory to use when constructing pages.
     * @param settings settings for the application.
     */
    @Inject
    public CroquetApplication(final IPageFactory pageFactory, final Settings settings) {
        this.pageFactory = pageFactory;
        this.settings = settings;
    }

    @Override
    public void init() {
        super.init();

        // this is a bit of a hack to get the DebugToolbar working in deployment mode
        if(settings.getWicketDebugToolbar() && !settings.getDevelopment()) {
            getDebugSettings().setDevelopmentUtilitiesEnabled(settings.getWicketDebugToolbar());
            DebugBar.registerContributor(VersionDebugContributor.DEBUG_BAR_CONTRIB, this);
            DebugBar.registerContributor(InspectorDebugPanel.DEBUG_BAR_CONTRIB, this);
            DebugBar.registerContributor(SessionSizeDebugPanel.DEBUG_BAR_CONTRIB, this);
            DebugBar.registerContributor(PageSizeDebugPanel.DEBUG_BAR_CONTRIB, this);
        }


        // check the mode we're running in and configure a few things
        if(settings.getStatelessChecker()) {
            LOG.debug("Statless checker added to application...");
            // add stateless checks if we're in dev mode
            this.getComponentPostOnBeforeRenderListeners().add(new StatelessChecker());
        }

        // set the render strategy for the application
        this.getRequestCycleSettings().setRenderStrategy(settings.getRenderStrategy());

        // Total hack to get wicket extensions properties to load
        final List<IInitializer> initList = new ArrayList<IInitializer>();

        initList.add(new Initializer());
        this.getResourceSettings().getStringResourceLoaders().add(new InitializerStringResourceLoader(initList));

        // mount all of the pages
        for(final Map.Entry<String, Class<? extends WebPage>> page:settings.getPageMountClasses().entrySet()) {
            this.mountPage(page.getKey(), page.getValue());
        }

        // mount all of the resources
        for(final Map.Entry<String, Class<? extends IResource>> page:settings.getResourceMountClasses().entrySet()) {
            this.mountResource(page.getKey(), new ResourceReference(page.getKey()) {
                private static final long serialVersionUID = 1L;

                @Override
                public IResource getResource() {
                    try {
                        return page.getValue().newInstance();
                    } catch (final InstantiationException | IllegalAccessException e) {
                        LOG.error("Exception: {}", e.getMessage());
                        return null;
                    }
                }
            });
        }

        // set the exception page if we're in development and it's set
        if(settings.getDevelopment() && settings.getExceptionPage() != null) {
            this.getRequestCycleListeners().add(new AbstractRequestCycleListener() {
                @Override
                public IRequestHandler onException(final RequestCycle cycle, final Exception e) {
                    LOG.error("Returning 500: {}", e.getMessage());
                    return new RenderPageRequestHandler(new PageProvider(settings.getExceptionPage()));
                }
            });
        }

        // should we strip wicket tags?
        getMarkupSettings().setStripWicketTags(settings.getStripWicketTags());

        // Wicket bootstrap is serving a JQuery map file, which is used to debug
        // minified JavaScript. This results in a 500 when trying to access the
        // resource as that file type is not permitted. Until we can figure out
        // how to tell Wicket to not load that resource, this hack is needed.
        final IPackageResourceGuard packageResourceGuard = getResourceSettings().getPackageResourceGuard();
        if (packageResourceGuard instanceof SecurePackageResourceGuard) {
            final SecurePackageResourceGuard guard = (SecurePackageResourceGuard)packageResourceGuard;
            guard.addPattern("+*.map");
        }

        if(settings.getMinifyResources()) {
            getResourceBundles().addCssBundle(
                    settings.getHomePageClass(),
                    "css-bundle.css",
                    settings.getCssResourceReferences());

            getResourceBundles().addJavaScriptBundle(
                    settings.getHomePageClass(),
                    "js-bundle.js",
                    settings.getJavaScriptResourceReferences());
        }

        getHeaderContributorListenerCollection().add(new IHeaderContributor() {
            private static final long serialVersionUID = 1L;
            @Override
            public void renderHead(final IHeaderResponse response) {
                for (final JavaScriptResourceReference resource : settings.getJavaScriptResourceReferences()) {
                    response.render(JavaScriptReferenceHeaderItem.forReference(resource));
                }
                for (final UrlResourceReference resource : settings.getExternalJavaScriptResourceReferences()) {
                    response.render(JavaScriptReferenceHeaderItem.forReference(resource));
                }
                for (final CssResourceReference resource : settings.getCssResourceReferences()) {
                    response.render(CssReferenceHeaderItem.forReference(resource));
                }
                for (final UrlResourceReference resource : settings.getExternalCssResourceReferences()) {
                    response.render(CssReferenceHeaderItem.forReference(resource));
                }
            }
        });

        LOG.debug("Done calling CroquetApplication.init()");
    }

    @Override
    public RuntimeConfigurationType getConfigurationType() {
         return settings.getDevelopment() ? RuntimeConfigurationType.DEVELOPMENT : RuntimeConfigurationType.DEPLOYMENT;
    }

    @Override
    protected IPageFactory newPageFactory() {
        return pageFactory;
    }

    @Override
    protected Class<? extends AbstractAuthenticatedWebSession> getWebSessionClass() {
        // Wicket needs to be fixed: https://issues.apache.org/jira/browse/WICKET-5557
        //return settings.getWicketSessionClass();
        return UnauthenticatedWebSession.class;
    }

    @Override
    protected Class<? extends WebPage> getSignInPageClass() {
        // return the homepage here, should never be called
        return settings.getLoginPageClass();
    }

    @Override
    public Class<? extends Page> getHomePage() {
        return settings.getHomePageClass();
    }


    // CHECKSTYLE:OFF because the link line is too long
    /**
     * An AbstractAuthenticatedWebSession that states everyone is logged in as Roles.USER.
     *
     * @see http://ci.apache.org/projects/wicket/apidocs/6.0.x/org/apache/wicket/authroles/authentication/AbstractAuthenticatedWebSession.html
     */
    // CHECKSTYLE:ON
    public static class UnauthenticatedWebSession extends AbstractAuthenticatedWebSession {

        private static final long serialVersionUID = -1099204714854141425L;

        /**
         * Constructs an UnauthenticatedWebSession given the request.
         * @param request the request for the session.
         */
        public UnauthenticatedWebSession(final Request request) {
            super(request);
        }

        @Override
        public Roles getRoles() {
            // everyone is just a user
            return new Roles(Roles.USER);
        }

        @Override
        public boolean isSignedIn() {
            // everyone is signed in
            return true;
        }
    }
}
