package com.chavaillaz.appender.log4j.elastic;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import com.chavaillaz.appender.DeliveryOptions;
import com.chavaillaz.appender.LogConfiguration;
import com.chavaillaz.appender.log4j.DefaultLogConverter;
import lombok.Builder;
import lombok.Value;
import org.jspecify.annotations.Nullable;

/**
 * Configuration for logs transmissions using Elasticsearch, immutable once built through {@link #builder()}.
 */
@Value
@Builder(toBuilder = true)
public class ElasticsearchConfiguration implements LogConfiguration {

    String application;
    String host;
    String environment;

    /**
     * The class name of the converter turning a logging event into a document, created through its public
     * constructor taking this configuration.
     */
    @Builder.Default
    String converter = DefaultLogConverter.class.getName();

    /**
     * The name of the index to which the documents are sent.
     */
    @Builder.Default
    String index = "ha";

    /**
     * The suffix of the name of the index, formatted with the current date, or {@code null} for none.
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
     * The file the documents dropped are saved to, rotated every day, or {@code null} to only report them
     * on the status logger.
     */
    @Nullable Path deadLetterFile;

    /**
     * How the logs are batched, retried and shipped.
     */
    @Builder.Default
    DeliveryOptions deliveryOptions = DeliveryOptions.defaults();

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
