package com.metrink.croquet.logging;

import java.util.Map;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.jul.LevelChangePropagator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.DefaultTimeBasedFileNamingAndTriggeringPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.spi.FilterAttachable;

import com.metrink.croquet.LoggingSettings;

/**
 * Initialize the logging infrastructure for Croquet. There's a significant amount of procedural logic in this class
 * which makes it difficult to test. It's abstracted here to isolate that logic.
 * This logic is based upon DropWizard's {@link com.yammer.dropwizard.config.LoggingFactory}.
 */
public class CroquetLoggingFactory {

    /**
     * Configure logging.
     * @param loggingSettings the logging settings
     */
    public void configureLogging(final LoggingSettings loggingSettings) {

        final Logger root = (Logger)LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        final LoggerContext context = root.getLoggerContext();

        // Reset prior configuration
        context.reset();

        // Propagate level changes to java.util.logging
        final LevelChangePropagator propagator = new LevelChangePropagator();
        propagator.setContext(context);
        propagator.setResetJUL(true);

        // Set base threshold for all other loggers.
        root.setLevel(loggingSettings.getLevel());

        // Loop through explicitly configured loggers and set the levels
        for (final Map.Entry<String, Level> entry : loggingSettings.getLoggers().entrySet()) {
            ((Logger)LoggerFactory.getLogger(entry.getKey())).setLevel(entry.getValue());
        }

        if (loggingSettings.getLogFile().isEnabled()) {
            root.addAppender(getFileAppender(loggingSettings.getLogFile(), context));
        }

        if (loggingSettings.getConsole().isEnabled()) {
            root.addAppender(getConsoleAppender(loggingSettings.getConsole(), context));
        }
    }

    private FileAppender<ILoggingEvent> getFileAppender(final LoggingSettings.LogFile settings,
                                                               final LoggerContext context) {
        final PatternLayout formatter = getPatternLayout(context);
        formatter.setPattern(settings.getLogFormat());
        formatter.start();

        final FileAppender<ILoggingEvent> appender =
            settings.isArchive() ? new RollingFileAppender<ILoggingEvent>() :
                                   new FileAppender<ILoggingEvent>();

        appender.setAppend(true);
        appender.setContext(context);
        appender.setLayout(formatter);
        appender.setFile(settings.getCurrentLogFilename());
        appender.setPrudent(false);

        addThresholdFilter(appender, settings.getThreshold());

        if (settings.isArchive()) {

            final DefaultTimeBasedFileNamingAndTriggeringPolicy<ILoggingEvent> triggeringPolicy =
                    new DefaultTimeBasedFileNamingAndTriggeringPolicy<ILoggingEvent>();
            final TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<ILoggingEvent>();

            triggeringPolicy.setContext(context);
            triggeringPolicy.setTimeBasedRollingPolicy(rollingPolicy);

            rollingPolicy.setContext(context);
            rollingPolicy.setFileNamePattern(settings.getArchivedLogFilenamePattern());
            rollingPolicy.setTimeBasedFileNamingAndTriggeringPolicy(triggeringPolicy);
            rollingPolicy.setMaxHistory(settings.getArchivedFileCount());

            ((RollingFileAppender<ILoggingEvent>)appender).setRollingPolicy(rollingPolicy);
            ((RollingFileAppender<ILoggingEvent>)appender).setTriggeringPolicy(triggeringPolicy);

            rollingPolicy.setParent(appender);
            rollingPolicy.start();
        }

        appender.stop();
        appender.start();

        return appender;
    }

    private ConsoleAppender<ILoggingEvent> getConsoleAppender(final LoggingSettings.Console settings,
                                                                    final LoggerContext context) {
        final PatternLayout formatter = getPatternLayout(context);
        formatter.setPattern(settings.getLogFormat());
        formatter.start();

        final ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<ILoggingEvent>();
        appender.setContext(context);
        appender.setLayout(formatter);

        addThresholdFilter(appender, settings.getThreshold());

        appender.start();

        return appender;
    }

    private PatternLayout getPatternLayout(final Context context) {
        final PatternLayout layout = new PatternLayout();
        layout.setContext(context);
        return layout;
    }

    private void addThresholdFilter(final FilterAttachable<ILoggingEvent> appender, final Level threshold) {
        final ThresholdFilter filter = new ThresholdFilter();
        filter.setLevel(threshold.toString());
        filter.start();
        appender.addFilter(filter);
    }
}
