package com.metrink.croquet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.authroles.authentication.AbstractAuthenticatedWebSession;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.UrlResourceReference;
import org.apache.wicket.settings.IRequestCycleSettings.RenderStrategy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.metrink.croquet.wicket.CroquetApplication;
import com.metrink.croquet.wicket.CroquetApplication.UnauthenticatedWebSession;

/**
 * Base Settings class. Overriding this is not required, but it is recommended.
 *
 * Properties annotated with {@link JsonProperty} will be loaded from the YAML configuration file.
 */
public class WicketSettings extends AbstractSettings {
    private static final long serialVersionUID = -4071324712275262138L;

    private Class<? extends WebApplication> application = CroquetApplication.class;

    private Class<? extends WebPage> homePage;

    private Class<? extends WebPage> loginPage;

    private Class<? extends WebPage> exceptionPage;

    private Class<? extends AbstractAuthenticatedWebSession> wicketSessionClass = UnauthenticatedWebSession.class;

    private Map<String, Class<? extends WebPage>> pageMounts = new HashMap<>();

    private Map<String, Class<? extends IResource>> resourceMounts = new HashMap<>();

    private Class<?> resourcesRootClass;

    @JsonProperty("css_resources")
    private List<String> cssResources = new ArrayList<String>();

    @JsonProperty("js_resources")
    private List<String> jsResources = new ArrayList<String>();

    /*
     * By default these settings are NULL and "inherit" from development (or not) mode.
     * However, the application writer can specify them to override the default behavior.
     */
    @JsonProperty("minify_resources")
    private Boolean minifyResources;

    @JsonProperty("strip_wicket_tags")
    private Boolean stripWicketTags;

    @JsonProperty("stateless_checker")
    private Boolean statelessChecker;

    @JsonProperty("wicket_debug_toolbar")
    private Boolean wicketDebugToolbar;


    @Override
    protected void init() {
        if (getDevelopment()) {
            minifyResources = false;
            stripWicketTags = false;
            statelessChecker = true;
            wicketDebugToolbar = true;
        }
    }

    /**
     * Get the {@link WebApplication} for this application. Defaults to {@link CroquetApplication}.
     * @return the web application
     */
    public Class<? extends WebApplication> getWebApplicationClass() {
        return application;
    }

    void setWebApplicationClass(final Class<? extends CroquetApplication> application) {
        this.application = application;
    }

    /**
     * Gets the {@link AbstractAuthenticatedWebSession} for this application.
     * Defaults to {@link UnauthenticatedWebSession}.
     * @return the session class.
     */
    public Class<? extends AbstractAuthenticatedWebSession> getWicketSessionClass() {
        return wicketSessionClass;
    }

    void setWicketSessionClass(final Class<? extends AbstractAuthenticatedWebSession> wicketSessionClass) {
        this.wicketSessionClass = wicketSessionClass;
    }

    /**
     * Get the home page which will be loaded when visiting "/". Failure to specify a home page is a misconfiguration.
     * @return the home page
     */
    public Class<? extends WebPage> getHomePageClass() {
        return homePage;
    }

    void setHomePageClass(final Class<? extends WebPage> homePage) {
        this.homePage = homePage;
    }

    /**
     * Get the unauthenticated login page. This page will be the target of a redirect whenever visiting an authenticated
     * page prior to authentication. It defaults to the home page.
     * @return the authentication page
     */
    public Class<? extends WebPage> getLoginPageClass() {
        return loginPage == null ? getHomePageClass() : loginPage;
    }

    void setLoginPageClass(final Class<? extends WebPage> loginPage) {
        this.loginPage = loginPage;
    }

    /**
     * Get the page to display whenever an exception occurs. The default is null, which will resort to Wicket's default
     * behavior.
     * @return the exception page or null
     */
    public Class<? extends WebPage> getExceptionPage() {
        return exceptionPage;
    }

    void setExceptionPageClass(final Class<? extends WebPage> exceptionPage) {
        this.exceptionPage = exceptionPage;
    }

    /**
     * Get a mapping of paths to pages. This allows mounting page classes on bookmarkable URLs.
     * @return the map of paths to pages
     */
    public Map<String, Class<? extends WebPage>> getPageMountClasses() {
        return Collections.unmodifiableMap(pageMounts);
    }

    /**
     * Get a mapping of paths to resource.
     * @return the map of paths to resources
     */
    public Map<String, Class<? extends IResource>> getResourceMountClasses() {
        return Collections.unmodifiableMap(resourceMounts);
    }

    void addPageMount(final String path, final Class<? extends WebPage> page) {
        pageMounts.put(path, page);
    }

    void addResourceMount(final String path, final Class<? extends IResource> page) {
        resourceMounts.put(path, page);
    }

    /**
     * Get the root class for the relative paths of the CSS/JavaScript resources.
     * @return the class
     */
    public Class<?> getResourcesRootClass() {
        return resourcesRootClass != null ? resourcesRootClass : homePage;
    }

    /**
     * Get a list of CSS resources to include on the default page.
     * @return the list of CSS resources
     */
    public CssResourceReference[] getCssResourceReferences() {
        final List<CssResourceReference> resources = new ArrayList<CssResourceReference>();

        for (final String resource : cssResources) {
            // Don't provide resources with FQL URLs
            if (!resource.startsWith("//") && resource.startsWith("http")) {
                resources.add(new CssResourceReference(getResourcesRootClass(), resource));
            }
        }
        return resources.toArray(new CssResourceReference[resources.size()]);
    }

    /**
     * Get a list of FQL URL external resources to include on the default page. Used for CDNs.
     * @return the list of CSS resources
     */
    public UrlResourceReference[] getExternalCssResourceReferences() {
        final List<UrlResourceReference> resources = new ArrayList<UrlResourceReference>();

        for (final String resource : cssResources) {
            // Provide resources with FQL URLs
            if (resource.startsWith("//") || resource.startsWith("http")) {
                resources.add(new UrlResourceReference(Url.parse(resource)));
            }
        }
        return resources.toArray(new UrlResourceReference[resources.size()]);
    }

    /**
     * Get a list of JavaScript resources to include on the default page.
     * @return the list of JavaScript resources
     */
    public JavaScriptResourceReference[] getJavaScriptResourceReferences() {
        final List<JavaScriptResourceReference> resources = new ArrayList<JavaScriptResourceReference>();

        for (final String resource : jsResources) {
            if (!resource.startsWith("//") && resource.startsWith("http")) {
                resources.add(new JavaScriptResourceReference(getResourcesRootClass(), resource));
            }
        }
        return resources.toArray(new JavaScriptResourceReference[resources.size()]);
    }

    /**
     * Get a list of FQL URL external resources to include on the default page. Used for CDNs.
     * @return the list of JavaScript resources
     */
    public UrlResourceReference[] getExternalJavaScriptResourceReferences() {
        final List<UrlResourceReference> resources = new ArrayList<UrlResourceReference>();

        for (final String resource : jsResources) {
            // Provide resources with FQL URLs
            if (resource.startsWith("//") || resource.startsWith("http")) {
                resources.add(new UrlResourceReference(Url.parse(resource)));
            }
        }
        return resources.toArray(new UrlResourceReference[resources.size()]);
    }

    /**
     * Should Croquet minify CSS and JavaScript resources? Defaults to follow dev vs deploy.
     * @return true if resources should be minified
     */
    public Boolean getMinifyResources() {
        if(getDevelopment()) {
            return minifyResources != null ? minifyResources : false;
        } else {
            return minifyResources != null ? minifyResources : true;
        }
    }

    /**
     * Determine if tags should be removed from the final markup. Defaults to follow dev vs deploy.
     * @return true if the tags sholud be stripped
     */
    public boolean getStripWicketTags() {
        if(getDevelopment()) {
            return stripWicketTags != null ? stripWicketTags : false;
        } else {
            return stripWicketTags != null ? stripWicketTags : true;
        }
    }

    /**
     * Should Croquet enable the stateless checker? Defaults to follow dev vs deploy.
     * @return true if the stateless checker should be enabled
     */
    public Boolean getStatelessChecker() {
        if(getDevelopment()) {
            return statelessChecker != null ? statelessChecker : true;
        } else {
            return statelessChecker != null ? statelessChecker : false;
        }
    }

    /**
     * Should Croquet enable the wicket debug toolbar? Defaults to follow dev vs deploy.
     *
     * All of your pages MUST extend CroquetPage for the toolbar to appear.
     *
     * @return true if the wicket debug toolbar should be enabled
     */
    public Boolean getWicketDebugToolbar() {
        if(getDevelopment()) {
            return wicketDebugToolbar != null ? wicketDebugToolbar : true;
        } else {
            return wicketDebugToolbar != null ? wicketDebugToolbar : false;
        }
    }

    /**
     * Get the Wicket rendering strategy.
     * @return the rendering strategy
     */
    public RenderStrategy getRenderStrategy() {
        return RenderStrategy.ONE_PASS_RENDER;
    }

}
