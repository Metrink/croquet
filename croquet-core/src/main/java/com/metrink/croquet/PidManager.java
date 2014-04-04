package com.metrink.croquet;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages pid files.
 */
class PidManager {
    private static final Logger LOG = LoggerFactory.getLogger(PidManager.class);

    private static final String PID_FILENAME = "croquet.pid";
    private String pidFilename;

    /**
     * Default constructor.
     */
    PidManager() {
        this.pidFilename = PID_FILENAME;
    }

    /**
     * Initialize the instance.
     * @param pidFilename the name of the pid file
     */
    PidManager(final String pidFilename) {
        this.pidFilename = pidFilename;
    }

    /**
     * Wrapper around {@link #dropPidFile()}. If the call is unsuccessful, terminate the JVM.
     */
    void dropPidOrDie() {
        if (!dropPidFile()) {
            LOG.error("Pid file {} already exists... exiting.", pidFilename);

            // print to the console as well
            System.err.println("Pid file already exists... exiting.");
            System.err.flush();
            System.exit(-1);
        }
    }

    /**
     * Create a file with the current process id in it. This is a no-op on Windows. Prior copies of the file must be
     * removed prior to launch.
     * @return true if the pid file was successfully created
     */
    private boolean dropPidFile() {
        LOG.trace("Entering dropPidFile()");

        // we cannot create pid files on Windows
        if (System.getProperty("os.name").startsWith("Windows")) {
            LOG.info("Pid file creation is unsupported on Windows... skipping");
            return true;
        }

        try {
            final String[] cmd = {"bash", "-o", "noclobber", "-c", "echo $PPID > " + pidFilename};
            final Process p = Runtime.getRuntime().exec(cmd);

            if (p.waitFor() != 0) {
                LOG.error("Unable to drop PID file");
                return false;
            }
        } catch (final InterruptedException | IOException e) {
            LOG.error("Unable to drop PID file: " + e.getMessage());
            return false;
        }

        // This must be called after we've successfully dropped the PID file. Otherwise, it might clean-up another
        // instances PID file. Keep in mind this doesn't account for kill -9 or a hard lockup. The start-up script
        // should provide some additional logic to clean-up stale pid files.
        new File(pidFilename).deleteOnExit();

        LOG.debug("Dropped PID file");
        LOG.trace("Exiting dropPIDFile()");
        return true;
    }
}
