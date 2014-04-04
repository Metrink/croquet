package com.metrink.croquet.examples.crm;

import com.google.inject.Inject;
import com.google.inject.name.Named;


/**
 * A bogus class that provides the "current user".
 *
 * The only purpose of this class is to act as a non-Serializable dependency for pages.
 * It is used to show off the Hydrate feature.
 */
public class CurrentUser {

    private final String currentUser;

    /**
     * Constructor used by the proxy factory.
     */
    public CurrentUser() {
        currentUser = null;
    }

    /**
     * Constructor that uses the bound name.
     * @param currentUser the name of the current user.
     */
    @Inject
    public CurrentUser(@Named("current-user") final String currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * Gets the current user.
     * @return the current user.
     */
    public String getCurrentUser() {
        return currentUser;
    }
}
