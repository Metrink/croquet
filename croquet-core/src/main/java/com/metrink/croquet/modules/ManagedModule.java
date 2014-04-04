package com.metrink.croquet.modules;

/**
 * Module that is started before the Jetty server starts and stops after Jetty stops.
 */
public interface ManagedModule {

    /**
     * Called before Jetty starts.
     */
    public void start();
    
    /**
     * Called after Jetty stops.
     */
    public void stop();
}
