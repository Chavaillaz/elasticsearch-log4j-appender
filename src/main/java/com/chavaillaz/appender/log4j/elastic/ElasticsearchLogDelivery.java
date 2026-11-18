package com.chavaillaz.appender.log4j.elastic;

import static com.chavaillaz.appender.log4j.elastic.ElasticsearchUtils.createClient;
import static java.time.OffsetDateTime.now;
import static java.util.concurrent.CompletableFuture.completedFuture;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorResponse;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import org.jspecify.annotations.Nullable;

import com.chavaillaz.appender.FileDeadLetterListener;
import com.chavaillaz.appender.LogDeliveryListener;
import com.chavaillaz.appender.TransmissionResult;
import com.chavaillaz.appender.log4j.AbstractBatchLogDelivery;

/**
 * Implementation of logs transmission for Elasticsearch, through its asynchronous client: a bulk request
 * holds no thread while waiting for its answer, which completes the future the library handles.
 */
public class ElasticsearchLogDelivery extends AbstractBatchLogDelivery<ElasticsearchConfiguration> {

    /**
     * Bytes the action preceding each document adds to a bulk request, the name of the index aside:
     * {@code {"index":{"_index":""}}} and the line feeds ending both lines.
     */
    private static final int ACTION_OVERHEAD = 25;

    private final ElasticsearchAsyncClient client;

    /**
     * The length of the name of the index the documents are sent to, taken once, a date suffix keeping it
     * the same from one day to the next.
     */
    private final int indexNameLength;

    /**
     * Creates a new logs delivery handler for Elasticsearch.
     *
     * @param configuration The configuration to use
     */
    public ElasticsearchLogDelivery(ElasticsearchConfiguration configuration) {
        // The listener first, so that a directory it cannot create does not leave a client running
        this(configuration, deadLetterListener(configuration), createClient(configuration));
    }

    /**
     * Creates a new logs delivery handler for Elasticsearch.
     *
     * @param configuration The configuration to use
     * @param client        The asynchronous Elasticsearch client to use, closed along with this delivery
     */
    public ElasticsearchLogDelivery(ElasticsearchConfiguration configuration, ElasticsearchAsyncClient client) {
        this(configuration, deadLetterListener(configuration), client);
    }

    private ElasticsearchLogDelivery(ElasticsearchConfiguration configuration, @Nullable LogDeliveryListener listener, ElasticsearchAsyncClient client) {
        super(configuration, listener);
        this.client = client;
        this.indexNameLength = configuration.generateIndexName(now()).length();
    }

    /**
     * Creates the listener saving the documents dropped to the dead-letter file of the configuration.
     *
     * @param configuration The configuration to use
     * @return The listener, or {@code null} without a dead-letter file
     * @throws java.io.UncheckedIOException If the directory of the file does not exist and cannot be created
     */
    private static @Nullable LogDeliveryListener deadLetterListener(ElasticsearchConfiguration configuration) {
        Path file = configuration.getDeadLetterFile();
        return file != null ? new FileDeadLetterListener(file) : null;
    }

    @Override
    protected CompletableFuture<TransmissionResult<Map<String, Object>>> sendBulk(List<Map<String, Object>> documents) {
        try {
            BulkRequest.Builder builder = new BulkRequest.Builder();
            for (Map<String, Object> document : documents) {
                String index = indexOf(document);
                builder.operations(operation -> operation
                        .index(action -> action
                                .index(index)
                                .document(document)));
            }

            // Completed by a thread of the client once answered, the library handling the result on its own
            return client.bulk(builder.build()).thenApply(response -> toResult(documents, response));
        } catch (Exception e) {
            // Logged with its cause and retried by the library
            return completedFuture(TransmissionResult.failure(e));
        }
    }

    /**
     * The index a document goes to, suffixed with the date of its event in the default time zone of the system,
     * rather than the date it is sent at: a document logged just before midnight, or retried after it, belongs
     * with the others of its day. A document without a {@code datetime} field in the ISO-8601 format, which a
     * converter of its own may leave out, is dated when sent.
     *
     * @param document The document to send
     * @return The name of its index
     */
    String indexOf(Map<String, Object> document) {
        ElasticsearchConfiguration configuration = getConfiguration();
        if (configuration.getIndexSuffix() == null) {
            return configuration.getIndex();
        }
        OffsetDateTime date = null;
        if (document.get("datetime") instanceof String datetime) {
            try {
                date = OffsetDateTime.parse(datetime).atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime();
            } catch (DateTimeParseException e) {
                // Written by a converter of its own in another format
            }
        }
        return configuration.generateIndexName(date != null ? date : now());
    }

    /**
     * Turns the response to a bulk request into the outcome of its transmission: only the documents
     * Elasticsearch rejected, for a mapping conflict or a full queue for instance, are retried, the others
     * being indexed already, and all of them are when the response does not tell them apart.
     *
     * @param documents The documents sent, in the order of the request
     * @param response  The response to the request
     * @return The outcome of the transmission
     */
    static TransmissionResult<Map<String, Object>> toResult(List<Map<String, Object>> documents, BulkResponse response) {
        if (!response.errors()) {
            return TransmissionResult.success();
        }
        List<BulkResponseItem> items = response.items();
        if (items.size() != documents.size()) {
            return TransmissionResult.failure(new IllegalStateException(
                    "Elasticsearch answered " + items.size() + " items to a bulk of " + documents.size() + " documents"));
        }

        List<Map<String, Object>> rejected = new ArrayList<>();
        BulkResponseItem firstRejection = null;
        for (int i = 0; i < items.size(); i++) {
            BulkResponseItem item = items.get(i);
            if (item.error() != null) {
                rejected.add(documents.get(i));
                if (firstRejection == null) {
                    firstRejection = item;
                }
            }
        }
        if (firstRejection == null) {
            return TransmissionResult.failure(new IllegalStateException(
                    "Elasticsearch reported errors for a bulk without naming the documents in error"));
        }

        BulkResponseItem rejection = firstRejection;
        ElasticsearchException cause = new ElasticsearchException("bulk", ErrorResponse.of(error -> error
                .error(rejection.error())
                .status(rejection.status())));
        return rejected.size() == documents.size()
                ? TransmissionResult.failure(cause)
                : TransmissionResult.partialFailure(rejected, cause);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Counts the action preceding the document in a bulk request as well.
     */
    @Override
    protected long estimateDocumentSize(Map<String, Object> document) {
        return super.estimateDocumentSize(document) + ACTION_OVERHEAD + indexNameLength;
    }

    @Override
    public void close(@Nullable Duration timeout) throws IOException {
        super.close(timeout);
        client.close();
    }

}
