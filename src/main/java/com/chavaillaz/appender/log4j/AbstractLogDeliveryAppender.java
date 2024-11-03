package com.chavaillaz.appender.log4j;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.chavaillaz.appender.LogConfiguration;
import com.chavaillaz.appender.LogDelivery;
import lombok.Getter;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

/**
 * Abstract appender with basic implementation for transmissions of logs from Log4j2.
 *
 * @param <C> The configuration type
 */
public abstract class AbstractLogDeliveryAppender<C extends LogConfiguration> extends AbstractAppender {

    private final ScheduledExecutorService threadPool = newSingleThreadScheduledExecutor();

    @Getter
    private final C logConfiguration;

    @Getter
    private LogDelivery logDeliveryHandler;

    protected AbstractLogDeliveryAppender(String name, Filter filter, Layout<?> layout, C configuration) {
        super(name, filter, layout, true, Property.EMPTY_ARRAY);
        this.logConfiguration = configuration;
    }

    @Override
    public void start() {
        try {
            logDeliveryHandler = createDeliveryHandler();
        } catch (Exception e) {
            error("Client configuration error", e);
        }

        if (logConfiguration.getFlushThreshold() > 1) {
            threadPool.scheduleWithFixedDelay(
                    logDeliveryHandler::flush,
                    logConfiguration.getFlushInterval().toMillis(),
                    logConfiguration.getFlushInterval().toMillis(),
                    MILLISECONDS);
        }

        super.start();
    }

    /**
     * Creates the client for logs transmission.
     */
    public abstract LogDelivery createDeliveryHandler();

    @Override
    public void append(LogEvent loggingEvent) {
        threadPool.submit(createDeliveryTask(loggingEvent));
    }

    /**
     * Creates a runnable in order to transmit the given log.
     *
     * @param loggingEvent The logging event to send
     */
    public abstract Runnable createDeliveryTask(LogEvent loggingEvent);

    @Override
    public boolean stop(long timeout, TimeUnit timeUnit) {
        try {
            threadPool.shutdown();
            threadPool.awaitTermination(timeout, timeUnit);
        } catch (InterruptedException e) {
            error("Thread interrupted during termination", e);
            currentThread().interrupt();
        } finally {
            try {
                logDeliveryHandler.close();
            } catch (Exception e) {
                error("Log delivery closing error", e);
            }
            super.stop(timeout, timeUnit);
        }
        return true;
    }

}
