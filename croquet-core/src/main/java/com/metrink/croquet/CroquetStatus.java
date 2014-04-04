package com.metrink.croquet;


/**
 * The status of a Croquet application.
 *
 * <ul>
 * <li>STARTING: The Croquet application is starting up but not yet running.</li>
 * <li>RUNNING: The Croquet application has fully started and can handle requests.</li>
 * <li>STOPPING: The Croque application is in the process of stopping.</li>
 * <li>STOPPED: The Croque application has stopped and cannot handle requests.</li>
 * </ul>
 *
 */
public enum CroquetStatus {
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED
}
