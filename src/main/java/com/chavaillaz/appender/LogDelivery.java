package com.chavaillaz.appender;

import java.util.List;
import java.util.Map;

/**
 * Transmission of logs to an external monitoring system.
 */
public interface LogDelivery extends AutoCloseable {

    /**
     * Sends the given record data.
     *
     * @param document The record data
     */
    void send(Map<String, Object> document);

    /**
     * Sends the given list of record data.
     *
     * @param documents The list of record data
     */
    void send(List<Map<String, Object>> documents);

    /**
     * Sends the pending records (generating a partially filled batch).
     * Intended to be called periodically to clean out pending messages.
     */
    void flush();

}
