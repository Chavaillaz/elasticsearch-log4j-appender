package com.chavaillaz.appender.log4j.elastic;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.chavaillaz.appender.DeliveryOptions;
import com.chavaillaz.appender.LogConfiguration;

/**
 * Configuration for logs transmissions using Elasticsearch, immutable once built through {@link #builder()}.
 */
public final class ElasticsearchConfiguration implements LogConfiguration {

    /**
     * The delivery options of the appender, unless configured otherwise: those of the common library, but for
     * documents sent 100 at a time, and bulk requests split past 10 MiB, well under the 100 MB of
     * {@code http.max_content_length} over which Elasticsearch refuses a request by default.
     */
    public static final DeliveryOptions DEFAULT_DELIVERY_OPTIONS = DeliveryOptions.defaults().with()
            .flushThreshold(100)
            .maxRequestBytes(10L * 1024 * 1024)
            .build();

    private final String application;
    private final String host;
    private final String environment;
    private final String index;
    private final @Nullable DateTimeFormatter indexSuffix;
    private final String url;
    private final boolean urlTrusted;
    private final @Nullable String user;
    private final @Nullable String password;
    private final @Nullable String apiKey;
    private final DeliveryOptions deliveryOptions;

    /**
     * Creates the configuration a builder describes.
     *
     * @param builder The builder holding the settings
     */
    private ElasticsearchConfiguration(ElasticsearchConfigurationBuilder builder) {
        this.application = builder.application;
        this.host = builder.host;
        this.environment = builder.environment;
        this.index = builder.index;
        this.indexSuffix = builder.indexSuffix;
        this.url = builder.url;
        this.urlTrusted = builder.urlTrusted;
        this.user = builder.user;
        this.password = builder.password;
        this.apiKey = builder.apiKey;
        this.deliveryOptions = builder.deliveryOptions;
    }

    /**
     * Creates a builder, with the index {@code ha} and the delivery options {@link #DEFAULT_DELIVERY_OPTIONS}
     * unless set, every other setting unset.
     *
     * @return The new builder
     */
    public static ElasticsearchConfigurationBuilder builder() {
        return new ElasticsearchConfigurationBuilder();
    }

    /**
     * Creates a builder starting from the settings of this configuration.
     *
     * @return The new builder
     */
    public ElasticsearchConfigurationBuilder toBuilder() {
        return new ElasticsearchConfigurationBuilder(this);
    }

    @Override
    public String getApplication() {
        return application;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getEnvironment() {
        return environment;
    }

    /**
     * Gets the name of the index to which the documents are sent.
     *
     * @return The index name
     */
    public String getIndex() {
        return index;
    }

    /**
     * Gets the suffix of the name of the index, formatted with the date of each event.
     *
     * @return The suffix, or {@code null} for none
     */
    public @Nullable DateTimeFormatter getIndexSuffix() {
        return indexSuffix;
    }

    /**
     * Gets the address of Elasticsearch, in the format {@code scheme://host:port}.
     *
     * @return The address
     */
    public String getUrl() {
        return url;
    }

    /**
     * Tells whether to trust any certificate Elasticsearch presents over HTTPS, a self-signed one for instance,
     * without verifying its host name either.
     *
     * @return {@code true} to trust any certificate
     */
    public boolean isUrlTrusted() {
        return urlTrusted;
    }

    /**
     * Gets the username to use as credentials, along with the password, unless an API key is set.
     *
     * @return The username, or {@code null} for none
     */
    public @Nullable String getUser() {
        return user;
    }

    /**
     * Gets the password of the user.
     *
     * @return The password, or {@code null} for none
     */
    public @Nullable String getPassword() {
        return password;
    }

    /**
     * Gets the API key, already encoded, used rather than the user and password when set.
     *
     * @return The API key, or {@code null} for none
     */
    public @Nullable String getApiKey() {
        return apiKey;
    }

    @Override
    public DeliveryOptions getDeliveryOptions() {
        return deliveryOptions;
    }

    /**
     * Generates the index name using the suffix if present.
     *
     * @param dateTime The date and time of the event in the format {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME}
     *                 or {@code null} to avoid using the index suffix if defined
     * @return The computed index name
     */
    public String generateIndexName(@Nullable String dateTime) {
        return generateIndexName(Optional.ofNullable(dateTime)
                .map(OffsetDateTime::parse)
                .orElse(null));
    }

    /**
     * Generates the index name using the suffix if present.
     *
     * @param dateTime The date and time of the event or {@code null} to avoid using the index suffix if defined
     * @return The computed index name
     */
    public String generateIndexName(@Nullable OffsetDateTime dateTime) {
        if (dateTime != null && indexSuffix != null) {
            return index + dateTime.format(indexSuffix);
        } else {
            return index;
        }
    }

    /**
     * Compares the settings: two configurations are equal when every setting is.
     *
     * @param other The object to compare with
     * @return {@code true} for a configuration with the same settings
     */
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof ElasticsearchConfiguration configuration
                && urlTrusted == configuration.urlTrusted
                && Objects.equals(application, configuration.application)
                && Objects.equals(host, configuration.host)
                && Objects.equals(environment, configuration.environment)
                && Objects.equals(index, configuration.index)
                && Objects.equals(indexSuffix, configuration.indexSuffix)
                && Objects.equals(url, configuration.url)
                && Objects.equals(user, configuration.user)
                && Objects.equals(password, configuration.password)
                && Objects.equals(apiKey, configuration.apiKey)
                && Objects.equals(deliveryOptions, configuration.deliveryOptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(application, host, environment, index, indexSuffix, url, urlTrusted, user, password,
                apiKey, deliveryOptions);
    }

    /**
     * Lists every setting with its value, but the password and the API key, which a log printing the
     * configuration would otherwise give away.
     *
     * @return The settings, as {@code ElasticsearchConfiguration[application=myApplication, ...]}
     */
    @Override
    public String toString() {
        return "ElasticsearchConfiguration[application=" + application
                + ", host=" + host
                + ", environment=" + environment
                + ", index=" + index
                + ", indexSuffix=" + indexSuffix
                + ", url=" + url
                + ", urlTrusted=" + urlTrusted
                + ", user=" + user
                + ", deliveryOptions=" + deliveryOptions + "]";
    }

    /**
     * Builder for {@link ElasticsearchConfiguration}, created through {@link ElasticsearchConfiguration#builder()},
     * or from the settings of a configuration through {@link ElasticsearchConfiguration#toBuilder()}.
     */
    public static final class ElasticsearchConfigurationBuilder {

        private String application;
        private String host;
        private String environment;
        private String index = "ha";
        private @Nullable DateTimeFormatter indexSuffix;
        private String url;
        private boolean urlTrusted;
        private @Nullable String user;
        private @Nullable String password;
        private @Nullable String apiKey;
        private DeliveryOptions deliveryOptions = DEFAULT_DELIVERY_OPTIONS;

        private ElasticsearchConfigurationBuilder() {
            // Instantiated only through ElasticsearchConfiguration.builder()
        }

        /**
         * Creates a builder starting from the settings of a configuration, for
         * {@link ElasticsearchConfiguration#toBuilder()}.
         *
         * @param configuration The configuration to start from
         */
        private ElasticsearchConfigurationBuilder(ElasticsearchConfiguration configuration) {
            this.application = configuration.application;
            this.host = configuration.host;
            this.environment = configuration.environment;
            this.index = configuration.index;
            this.indexSuffix = configuration.indexSuffix;
            this.url = configuration.url;
            this.urlTrusted = configuration.urlTrusted;
            this.user = configuration.user;
            this.password = configuration.password;
            this.apiKey = configuration.apiKey;
            this.deliveryOptions = configuration.deliveryOptions;
        }

        /**
         * Sets the name of the application generating the logs.
         *
         * @param application The application name
         * @return This builder
         */
        public ElasticsearchConfigurationBuilder application(String application) {
            this.application = application;
            return this;
        }

        /**
         * Sets the name of the host on which the application is running.
         *
         * @param host The host name
         * @return This builder
         */
        public ElasticsearchConfigurationBuilder host(String host) {
            this.host = host;
            return this;
        }

        /**
         * Sets the name of the environment in which the application is running.
         *
         * @param environment The environment name
         * @return This builder
         */
        public ElasticsearchConfigurationBuilder environment(String environment) {
            this.environment = environment;
            return this;
        }

        /**
         * Sets the name of the index to which the documents are sent.
         *
         * @param index The index name
         * @return This builder
         */
        public ElasticsearchConfigurationBuilder index(String index) {
            this.index = index;
            return this;
        }

        /**
         * Sets the suffix of the name of the index, formatted with the date of each event.
         *
         * @param indexSuffix The suffix, or {@code null} for none
         * @return This builder
         */
        public ElasticsearchConfigurationBuilder indexSuffix(@Nullable DateTimeFormatter indexSuffix) {
            this.indexSuffix = indexSuffix;
            return this;
        }

        /**
         * Sets the address of Elasticsearch.
         *
         * @param url The address, in the format {@code scheme://host:port}
         * @return This builder
         */
        public ElasticsearchConfigurationBuilder url(String url) {
            this.url = url;
            return this;
        }

        /**
         * Sets whether to trust any certificate Elasticsearch presents over HTTPS, without verifying its host
         * name either.
         *
         * @param urlTrusted {@code true} to trust any certificate
         * @return This builder
         */
        public ElasticsearchConfigurationBuilder urlTrusted(boolean urlTrusted) {
            this.urlTrusted = urlTrusted;
            return this;
        }

        /**
         * Sets the username to use as credentials, along with the password.
         *
         * @param user The username, or {@code null} for none
         * @return This builder
         */
        public ElasticsearchConfigurationBuilder user(@Nullable String user) {
            this.user = user;
            return this;
        }

        /**
         * Sets the password of the user.
         *
         * @param password The password, or {@code null} for none
         * @return This builder
         */
        public ElasticsearchConfigurationBuilder password(@Nullable String password) {
            this.password = password;
            return this;
        }

        /**
         * Sets the API key, already encoded, used rather than the user and password when set.
         *
         * @param apiKey The API key, or {@code null} for none
         * @return This builder
         */
        public ElasticsearchConfigurationBuilder apiKey(@Nullable String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * Sets how the logs are batched, retried and shipped.
         *
         * @param deliveryOptions The delivery options
         * @return This builder
         */
        public ElasticsearchConfigurationBuilder deliveryOptions(DeliveryOptions deliveryOptions) {
            this.deliveryOptions = deliveryOptions;
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return The new configuration
         */
        public ElasticsearchConfiguration build() {
            return new ElasticsearchConfiguration(this);
        }

    }

}
