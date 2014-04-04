package com.metrink.croquet.examples.crm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.metrink.croquet.Settings;

/**
 * Custom settings file for croquet-exmaples.
 */
public class CrmSettings extends Settings {

    private static final long serialVersionUID = -7421676903882932579L;

    @JsonProperty(value = "current-user", required = true)
    private String currentUser;

    @JsonProperty(value = "mail-server", required = true)
    private String mailServer;

    @JsonProperty("mail-user")
    private String mailUser;

    @JsonProperty("mail-pass")
    private String mailPass;

    /**
     * Gets the current user.
     * @return current user.
     */
    public String getCurrentUser() {
        return currentUser;
    }

    /**
     * Sets the current user.
     * @param currentUser the current user.
     */
    protected void setCurrentUser(final String currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * Gets the mail server.
     * @return mail server.
     */
    public String getMailServer() {
        return mailServer;
    }

    /**
     * Sets a new mail server.
     * @param mailServer the mail server.
     */
    public void setMailServer(final String mailServer) {
        this.mailServer = mailServer;
    }

    /**
     * Gets the mail user.
     * @return mail user.
     */
    public String getMailUser() {
        return mailUser;
    }

    /**
     * Sets the mail user.
     * @param mailUser the mail user.
     */
    public void setMailUser(final String mailUser) {
        this.mailUser = mailUser;
    }

    /**
     * Gets the mail password.
     * @return mail password.
     */
    public String getMailPass() {
        return mailPass;
    }

    /**
     * Sets the mail password.
     * @param mailPass the mail password.
     */
    public void setMailPass(final String mailPass) {
        this.mailPass = mailPass;
    }


}
