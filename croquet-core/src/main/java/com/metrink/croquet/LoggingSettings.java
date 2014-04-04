package com.metrink.croquet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Logging settings.
 */
public class LoggingSettings implements Serializable {
    private static final long serialVersionUID = -5674522455586997214L;

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(LoggingSettings.class);

    @JsonProperty("level")
    private Level level = Level.WARN;

    @JsonProperty("loggers")
    private Map<String, Level> loggers = new HashMap<>();

    @JsonProperty("console")
    private Console console = new Console();

    @JsonProperty("file")
    private LogFile logFile = new LogFile();

    /**
     * Get level.
     * @return the level
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Set level.
     * @param level the level to set
     */
    public void setLevel(final Level level) {
        this.level = level;
    }

    /**
     * Get loggers.
     * @return the loggers
     */
    public Map<String, Level> getLoggers() {
        return loggers;
    }

    /**
     * Set loggers.
     * @param loggers the loggers to set
     */
    public void setLoggers(final Map<String, Level> loggers) {
        this.loggers = loggers;
    }

    /**
     * Get console.
     * @return the console
     */
    public Console getConsole() {
        return console;
    }

    /**
     * Set console.
     * @param console the console to set
     */
    public void setConsole(final Console console) {
        this.console = console;
    }

    /**
     * Get logFile.
     * @return the logFile
     */
    public LogFile getLogFile() {
        return logFile;
    }

    /**
     * Set logFile.
     * @param logFile the logFile to set
     */
    public void setLogFile(final LogFile logFile) {
        this.logFile = logFile;
    }

    /**
     * Console logging settings.
     */
    public static class Console implements Serializable {

        private static final long serialVersionUID = -3255753487703719490L;

        @JsonProperty
        private boolean enabled = true;

        @JsonProperty
        private Level threshold = Level.ALL;

        @JsonProperty
        private String logFormat = "%-5level %date{ISO8601} %c:  %m%n";

        /**
         * Get enabled.
         * @return the enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Set enabled.
         * @param enabled the enabled to set
         */
        public void setEnabled(final boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Get threshold.
         * @return the threshold
         */
        public Level getThreshold() {
            return threshold;
        }

        /**
         * Set threshold.
         * @param threshold the threshold to set
         */
        public void setThreshold(final Level threshold) {
            this.threshold = threshold;
        }

        /**
         * Get logFormat.
         * @return the logFormat
         */
        public String getLogFormat() {
            return logFormat;
        }

        /**
         * Set logFormat.
         * @param logFormat the logFormat to set
         */
        public void setLogFormat(final String logFormat) {
            this.logFormat = logFormat;
        }
    }

    /**
     * File logging settings.
     */
    public static class LogFile implements Serializable {

        private static final long serialVersionUID = 3605918064301699493L;

        private static final int ARCHIVE_FILE_COUNT_DEFAULT = 5;

        @JsonProperty
        private boolean enabled;

        @JsonProperty
        private Level threshold = Level.ALL;

        @JsonProperty
        private String currentLogFilename;

        @JsonProperty
        private boolean archive = true;

        @JsonProperty
        private String archivedLogFilenamePattern;

        @JsonProperty
        private int archivedFileCount = ARCHIVE_FILE_COUNT_DEFAULT;

        @JsonProperty
        private String logFormat = "%-5level %date{ISO8601} %c:  %m%n";

        /**
         * Get enabled.
         * @return the enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Set enabled.
         * @param enabled the enabled to set
         */
        public void setEnabled(final boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Get threshold.
         * @return the threshold
         */
        public Level getThreshold() {
            return threshold;
        }

        /**
         * Set threshold.
         * @param threshold the threshold to set
         */
        public void setThreshold(final Level threshold) {
            this.threshold = threshold;
        }

        /**
         * Get currentLogFilename.
         * @return the currentLogFilename
         */
        public String getCurrentLogFilename() {
            return currentLogFilename;
        }

        /**
         * Set currentLogFilename.
         * @param currentLogFilename the currentLogFilename to set
         */
        public void setCurrentLogFilename(final String currentLogFilename) {
            this.currentLogFilename = currentLogFilename;
        }

        /**
         * Get archive.
         * @return the archive
         */
        public boolean isArchive() {
            return archive;
        }

        /**
         * Set archive.
         * @param archive the archive to set
         */
        public void setArchive(final boolean archive) {
            this.archive = archive;
        }

        /**
         * Get archivedLogFilenamePattern.
         * @return the archivedLogFilenamePattern
         */
        public String getArchivedLogFilenamePattern() {
            return archivedLogFilenamePattern;
        }

        /**
         * Set archivedLogFilenamePattern.
         * @param archivedLogFilenamePattern the archivedLogFilenamePattern to set
         */
        public void setArchivedLogFilenamePattern(final String archivedLogFilenamePattern) {
            this.archivedLogFilenamePattern = archivedLogFilenamePattern;
        }

        /**
         * Get archivedFileCount.
         * @return the archivedFileCount
         */
        public int getArchivedFileCount() {
            return archivedFileCount;
        }

        /**
         * Set archivedFileCount.
         * @param archivedFileCount the archivedFileCount to set
         */
        public void setArchivedFileCount(final int archivedFileCount) {
            this.archivedFileCount = archivedFileCount;
        }

        /**
         * Get logFormat.
         * @return the logFormat
         */
        public String getLogFormat() {
            return logFormat;
        }

        /**
         * Set logFormat.
         * @param logFormat the logFormat to set
         */
        public void setLogFormat(final String logFormat) {
            this.logFormat = logFormat;
        }
    }
}
