package com.metrink.croquet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Settings class for REST. Overriding this is not required, but it is recommended.
 *
 * Properties annotated with JsonProperty will be loaded from the YAML configuration file.
 */
public class RestSettings extends AbstractSettings {
    private static final long serialVersionUID = -4071324712275262138L;

    private List<String> providerPackages = new ArrayList<>();

    /**
     * Perform post de-serialization modification of the Settings.
     */
    @Override
    protected void init() {
    }

    /**
     * Gets the list of provider packages.
     * @return list of provider packages.
     */
    public List<String> getProviderPackages() {
        return Collections.unmodifiableList(providerPackages);
    }

    /**
     * Adds a provider package to the list.
     * @param providerPackage the provider package to add.
     */
    public void addProviderPackage(final String providerPackage) {
        providerPackages.add(providerPackage);
    }

}
