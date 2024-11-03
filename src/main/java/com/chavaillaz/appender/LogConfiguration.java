package com.chavaillaz.appender;

import java.time.Duration;

/**
 * Configuration for logs transmission.
 */
public interface LogConfiguration {

    /**
     * Gets the name of the application generating the logs.
     *
     * @return The application name
     */
    String getApplication();

    /**
     * Sets the name of the application generating the logs.
     *
     * @param application The application name
     */
    void setApplication(String application);

    /**
     * Gets the name of the host on which the application is running.
     *
     * @return The host name
     */
    String getHost();

    /**
     * Sets the name of the host on which the application is running.
     *
     * @param host The host name
     */
    void setHost(String host);

    /**
     * Sets the environment in which the application is running.
     *
     * @return The environment name
     */
    String getEnvironment();

    /**
     * Sets the environment in which the application is running.
     *
     * @param environment The environment name
     */
    void setEnvironment(String environment);

    /**
     * Gets the threshold number of messages triggering the transmission of the logs.
     *
     * @return The threshold number of messages
     */
    long getFlushThreshold();

    /**
     * Sets the threshold number of messages triggering the transmission of the logs.
     *
     * @param messageNumber The threshold number of messages
     */
    void setFlushThreshold(long messageNumber);

    /**
     * Gets the maximum time interval between two flushes.
     * After each interval, the transmission of logs is triggered, even if not reaching the threshold number of messages.
     *
     * @return The maximum interval between two flushes
     */
    Duration getFlushInterval();

    /**
     * Gets the maximum time interval between two flushes.
     * After each interval, the transmission of logs is triggered, even if not reaching the threshold number of messages.
     *
     * @param flushInterval The maximum interval between two flushes
     */
    void setFlushInterval(Duration flushInterval);

}
