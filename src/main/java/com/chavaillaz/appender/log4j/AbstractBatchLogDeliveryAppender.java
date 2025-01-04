package com.chavaillaz.appender.log4j;

import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.chavaillaz.appender.LogConfiguration;
import com.chavaillaz.appender.LogDelivery;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * Abstract implementation of logs transmission by batches.
 *
 * @param <C> The configuration type
 */
@Log4j2
public abstract class AbstractBatchLogDeliveryAppender<C extends LogConfiguration> implements LogDelivery {

    /**
     * The list of logs documents to be sent with the next transmission
     */
    private final List<Map<String, Object>> batch = new ArrayList<>();

    /**
     * The configuration of the logs transmission
     */
    @Getter
    private final C configuration;

    /**
     * Creates a new logs delivery handler sending them by batches.
     *
     * @param configuration The configuration to use
     */
    protected AbstractBatchLogDeliveryAppender(C configuration) {
        this.configuration = configuration;
    }

    @Override
    public void send(Map<String, Object> document) {
        send(singletonList(document));
    }

    @Override
    public void send(List<Map<String, Object>> documents) {
        if (documents != null && !documents.isEmpty()) {
            stackAndSend(documents);
        }
    }

    @Override
    public synchronized void flush() {
        if (!batch.isEmpty()) {
            log.debug("Sending partial batch of {}/{}", batch.size(), configuration.getFlushThreshold());
            sendBatch();
        }
    }

    private synchronized void stackAndSend(List<Map<String, Object>> data) {
        batch.addAll(data);
        log.trace("Stacking batch {}/{}", batch.size(), configuration.getFlushThreshold());
        if (batch.size() >= configuration.getFlushThreshold()) {
            sendBatch();
        }
    }

    private synchronized void sendBatch() {
        if (!batch.isEmpty() && sendBulk(batch)) {
            batch.clear();
        }
    }

    /**
     * Sends the given list of documents representing logs.
     *
     * @param documents The list of record data
     * @return {@code true} if the transmission was successful, {@code false} otherwise
     */
    protected abstract boolean sendBulk(List<Map<String, Object>> documents);

    @Override
    public void close() {
        sendBatch();
    }

}
