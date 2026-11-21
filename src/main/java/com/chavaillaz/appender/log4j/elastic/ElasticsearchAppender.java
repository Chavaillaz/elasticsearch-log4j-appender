package com.chavaillaz.appender.log4j.elastic;

import static org.apache.logging.log4j.core.Appender.ELEMENT_TYPE;
import static org.apache.logging.log4j.core.Core.CATEGORY_NAME;

import java.time.format.DateTimeFormatter;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.jspecify.annotations.Nullable;

import com.chavaillaz.appender.DeliveryOptions;
import com.chavaillaz.appender.LogDelivery;
import com.chavaillaz.appender.log4j.AbstractLogDeliveryAppender;
import com.chavaillaz.appender.log4j.LogDeliveryAppenderBuilder;

/**
 * Appender implementation using Elasticsearch for transmissions of logs from Log4j.
 */
@Plugin(name = "ElasticsearchAppender", category = CATEGORY_NAME, elementType = ELEMENT_TYPE)
public class ElasticsearchAppender extends AbstractLogDeliveryAppender<ElasticsearchConfiguration> {

    protected ElasticsearchAppender(Builder builder, ElasticsearchConfiguration configuration) {
        super(builder, configuration);
    }

    @PluginBuilderFactory
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LogDelivery createLogDeliveryHandler() {
        return new ElasticsearchLogDelivery(getLogConfiguration());
    }

    /**
     * Holds the attributes of the element, those every appender built on the common library takes, inherited, and
     * those of Elasticsearch.
     */
    public static class Builder extends LogDeliveryAppenderBuilder<ElasticsearchAppender, Builder> {

        @PluginBuilderAttribute("Index")
        private String index = "ha";

        @PluginBuilderAttribute("IndexSuffix")
        private @Nullable String indexSuffix;

        @PluginBuilderAttribute("Url")
        @Required(message = "No URL of Elasticsearch provided")
        private String url;

        @PluginBuilderAttribute("UrlTrusted")
        private boolean urlTrusted = false;

        @PluginBuilderAttribute("User")
        private @Nullable String user;

        /**
         * The password, which the status logger lists hashed among the attributes of the appender, as the API
         * key, rather than as given.
         */
        @PluginBuilderAttribute(value = "Password", sensitive = true)
        private @Nullable String password;

        @PluginBuilderAttribute(value = "ApiKey", sensitive = true)
        private @Nullable String apiKey;

        /**
         * Creates a builder, every attribute at its default.
         */
        public Builder() {
            // Called by ElasticsearchAppender.builder(), the factory method Log4j2 builds the plugin with
        }

        /**
         * The name of the index to which the documents are sent, {@code ha} unless set.
         *
         * @return The index name
         */
        public String getIndex() {
            return index;
        }

        /**
         * The pattern of the suffix added to the name of the index, which the date of each event is formatted
         * with (see {@link DateTimeFormatter}).
         *
         * @return The pattern, or {@code null} for no suffix
         */
        public @Nullable String getIndexSuffix() {
            return indexSuffix;
        }

        /**
         * The address of Elasticsearch, in the format {@code scheme://host:port}, which the appender requires.
         *
         * @return The address
         */
        public String getUrl() {
            return url;
        }

        /**
         * Whether to trust any certificate Elasticsearch presents over HTTPS, without verifying its host name
         * either, {@code false} unless set.
         *
         * @return {@code true} to trust any certificate
         */
        public boolean isUrlTrusted() {
            return urlTrusted;
        }

        /**
         * The username to use as credentials, along with the password.
         *
         * @return The username, or {@code null} for none
         */
        public @Nullable String getUser() {
            return user;
        }

        /**
         * The password of the user.
         *
         * @return The password, or {@code null} for none
         */
        public @Nullable String getPassword() {
            return password;
        }

        /**
         * The API key, already encoded, used rather than the user and password when set.
         *
         * @return The API key, or {@code null} for none
         */
        public @Nullable String getApiKey() {
            return apiKey;
        }

        /**
         * Sets the name of the index to which the documents are sent.
         *
         * @param index The index name
         * @return This builder
         */
        public Builder setIndex(String index) {
            this.index = index;
            return this;
        }

        /**
         * Sets the pattern of the suffix added to the name of the index, which the date of each event is
         * formatted with.
         *
         * @param indexSuffix The pattern, following {@link DateTimeFormatter}, or {@code null} or an empty one
         *                    for no suffix
         * @return This builder
         */
        public Builder setIndexSuffix(@Nullable String indexSuffix) {
            this.indexSuffix = indexSuffix;
            return this;
        }

        /**
         * Sets the address of Elasticsearch.
         *
         * @param url The address, in the format {@code scheme://host:port}
         * @return This builder
         */
        public Builder setUrl(String url) {
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
        public Builder setUrlTrusted(boolean urlTrusted) {
            this.urlTrusted = urlTrusted;
            return this;
        }

        /**
         * Sets the username to use as credentials, along with the password.
         *
         * @param user The username, or {@code null} for none
         * @return This builder
         */
        public Builder setUser(@Nullable String user) {
            this.user = user;
            return this;
        }

        /**
         * Sets the password of the user.
         *
         * @param password The password, or {@code null} for none
         * @return This builder
         */
        public Builder setPassword(@Nullable String password) {
            this.password = password;
            return this;
        }

        /**
         * Sets the API key, already encoded, used rather than the user and password when set.
         *
         * @param apiKey The API key, or {@code null} for none
         * @return This builder
         */
        public Builder setApiKey(@Nullable String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * The delivery options an attribute left unset keeps the value of.
         *
         * @return The defaults of the appender, batching documents
         */
        @Override
        protected DeliveryOptions defaultDeliveryOptions() {
            return ElasticsearchConfiguration.DEFAULT_DELIVERY_OPTIONS;
        }

        /**
         * Builds the appender.
         *
         * @return The new appender
         * @throws IllegalArgumentException If a delivery option is out of its range, or the flush level unknown
         */
        @Override
        public ElasticsearchAppender build() {
            ElasticsearchConfiguration configuration = ElasticsearchConfiguration.builder()
                    .application(getApplication())
                    .host(getHost())
                    .environment(getEnvironment())
                    .index(index)
                    .indexSuffix(toFormatter(indexSuffix))
                    .url(url)
                    .urlTrusted(urlTrusted)
                    .user(user)
                    .password(password)
                    .apiKey(apiKey)
                    .deliveryOptions(getDeliveryOptions())
                    .build();
            return new ElasticsearchAppender(this, configuration);
        }

        /**
         * Parses the pattern of the index suffix.
         *
         * @param pattern The pattern, following {@link DateTimeFormatter}, or an empty one for no suffix
         * @return The formatter, or {@code null} for no suffix
         */
        private static @Nullable DateTimeFormatter toFormatter(@Nullable String pattern) {
            return pattern == null || pattern.isEmpty() ? null : DateTimeFormatter.ofPattern(pattern);
        }

    }

}
