package com.chavaillaz.appender.log4j.elastic;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import lombok.Builder;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import com.chavaillaz.appender.DeliveryOptions;
import com.chavaillaz.appender.LogConfiguration;

/**
 * Configuration for logs transmissions using Elasticsearch, immutable once built through {@link #builder()}.
 */
@Value
@Builder(toBuilder = true)
public class ElasticsearchConfiguration implements LogConfiguration {

    /**
     * The delivery options of the appender, unless configured otherwise: those of the common library, but for
     * documents sent 100 at a time, and bulk requests split past 10 MiB, well under the 100 MB of
     * {@code http.max_content_length} over which Elasticsearch refuses a request by default.
     */
    public static final DeliveryOptions DEFAULT_DELIVERY_OPTIONS = DeliveryOptions.defaults().with()
            .flushThreshold(100)
            .maxRequestBytes(10L * 1024 * 1024)
            .build();

    String application;
    String host;
    String environment;

    /**
     * The name of the index to which the documents are sent.
     */
    @Builder.Default
    String index = "ha";

    /**
     * The suffix of the name of the index, formatted with the date of each event, or {@code null} for none.
     */
    @Nullable DateTimeFormatter indexSuffix;

    /**
     * The address of Elasticsearch, in the format {@code scheme://host:port}.
     */
    String url;

    /**
     * Whether to trust any certificate Elasticsearch presents over HTTPS, a self-signed one for instance,
     * without verifying its host name either.
     */
    boolean urlTrusted;

    @Nullable String user;
    @Nullable String password;

    /**
     * The API key, already encoded, used rather than the user and password when set.
     */
    @Nullable String apiKey;

    /**
     * How the logs are batched, retried and shipped.
     */
    @Builder.Default
    DeliveryOptions deliveryOptions = DEFAULT_DELIVERY_OPTIONS;

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

}
