package com.metrink.croquet.wicket;

import org.apache.wicket.devutils.debugbar.DebugBar;
import org.apache.wicket.markup.html.WebPage;

import com.google.inject.Inject;
import com.metrink.croquet.WicketSettings;

/**
 * Base Croquet {@link WebPage}.
 */
public class CroquetPage extends WebPage {
    private static final long serialVersionUID = 1L;

    // we break our own rule and do field injection here so simplify the construction of pages
    // however we handle the unit test case below
    @Inject private WicketSettings wicketSettings;

    /**
     * Initialize the instance.
     */
    public CroquetPage() {
        add(new DebugBar("croquetDebugBar") {
            private static final long serialVersionUID = 9172081361782379034L;

            @Override
            public boolean isVisible() {
                // in unit tests, this will be null
                return wicketSettings == null ? false : wicketSettings.getWicketDebugToolbar();
            }
        });
    }
}
