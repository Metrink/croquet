package com.metrink.croquet.examples.crm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.metrink.croquet.modules.ManagedModule;

/**
 * This is a bogus "email" module that demonstrates {@link ManagedModule}s.
 */
public class EmailModule implements ManagedModule {
    private static final Logger LOG = LoggerFactory.getLogger(EmailModule.class);

    private CrmSettings settings;

    /**
     * Constructor for the "bogus" EmailModule.
     * @param settings the custom settings.
     */
    @Inject
    public EmailModule(final CrmSettings settings) {
        this.settings = settings;
    }

    @Override
    public void start() {
        LOG.info("Starting emailer with {} {} {}",
                settings.getMailServer(), 
                settings.getMailUser(), 
                settings.getMailPass());
    }

    @Override
    public void stop() {
        LOG.info("Stopping emailer");
    }
}
