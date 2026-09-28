package com.chavaillaz.appender.log4j.elastic;

import static com.chavaillaz.appender.log4j.elastic.ElasticsearchLogDelivery.toResult;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.OperationType;
import com.chavaillaz.appender.DeliveryOptions;
import com.chavaillaz.appender.DocumentSizeEstimator;
import com.chavaillaz.appender.TransmissionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ElasticsearchLogDeliveryTest {

    private static final Map<String, Object> FIRST = Map.of("logmessage", "first");
    private static final Map<String, Object> SECOND = Map.of("logmessage", "second");
    private static final Map<String, Object> THIRD = Map.of("logmessage", "third");

    private static BulkResponseItem indexed() {
        return BulkResponseItem.of(item -> item
                .operationType(OperationType.Index)
                .index("ha")
                .status(201));
    }

    private static BulkResponseItem rejected() {
        return BulkResponseItem.of(item -> item
                .operationType(OperationType.Index)
                .index("ha")
                .status(400)
                .error(error -> error
                        .type("document_parsing_exception")
                        .reason("failed to parse field [datetime]")));
    }

    private static BulkResponse response(boolean errors, BulkResponseItem... items) {
        return BulkResponse.of(response -> response
                .errors(errors)
                .took(1)
                .items(List.of(items)));
    }

    @Test
    void testSuccessWithoutErrors() {
        // When
        var result = toResult(List.of(FIRST, SECOND), response(false, indexed(), indexed()));

        // Then
        assertThat(result).isInstanceOf(TransmissionResult.Success.class);
    }

    @Test
    void testPartialFailureRetriesOnlyTheRejectedDocuments() {
        // When
        var result = toResult(List.of(FIRST, SECOND, THIRD), response(true, indexed(), rejected(), indexed()));

        // Then: only the rejected document is handed back, as the very instance sent, with the reason given
        assertThat(result).isInstanceOf(TransmissionResult.PartialFailure.class);
        var partial = (TransmissionResult.PartialFailure<Map<String, Object>>) result;
        assertThat(partial.rejected()).singleElement().isSameAs(SECOND);
        assertThat(partial.cause())
                .isInstanceOf(ElasticsearchException.class)
                .hasMessageContaining("document_parsing_exception");
    }

    @Test
    void testFailureWhenEveryDocumentIsRejected() {
        // When
        var result = toResult(List.of(FIRST, SECOND), response(true, rejected(), rejected()));

        // Then
        assertThat(result).isInstanceOf(TransmissionResult.Failure.class);
        assertThat(((TransmissionResult.Failure<Map<String, Object>>) result).cause())
                .isInstanceOf(ElasticsearchException.class);
    }

    @Test
    void testFailureWhenTheItemsDoNotMatchTheDocuments() {
        // When: fewer items than documents, which leaves no telling which one was indexed
        var result = toResult(List.of(FIRST, SECOND), response(true, rejected()));

        // Then: all of them are retried rather than some lost
        assertThat(result).isInstanceOf(TransmissionResult.Failure.class);
    }

    @Test
    void testSizeCountsTheActionOfTheDocument() throws Exception {
        // Given
        ElasticsearchConfiguration configuration = ElasticsearchConfiguration.builder()
                .url("http://localhost:1")
                .index("logs")
                .build();
        ElasticsearchLogDelivery delivery = new ElasticsearchLogDelivery(configuration);

        // When
        long size = delivery.estimateDocumentSize(FIRST);

        // Then: the document, and the action preceding it, with the line feeds ending both lines
        assertThat(size).isEqualTo(DocumentSizeEstimator.estimate(FIRST) + "{\"index\":{\"_index\":\"logs\"}}".length() + 2);
        delivery.close();
    }

    @Test
    void testDroppedDocumentsAreSavedToTheDeadLetterFile(@TempDir Path directory) throws Exception {
        // Given: a cluster that cannot be reached, and a single attempt per document
        ElasticsearchConfiguration configuration = ElasticsearchConfiguration.builder()
                .url("http://localhost:1")
                .deadLetterFile(directory.resolve("dead-letters.ndjson"))
                .deliveryOptions(DeliveryOptions.builder()
                        .maxConsecutiveFailures(1)
                        .build())
                .build();
        ElasticsearchLogDelivery delivery = new ElasticsearchLogDelivery(configuration);

        // When
        delivery.send(Map.of("logmessage", "saved"));
        delivery.close(Duration.ofSeconds(10));

        // Then: the document given up on is saved in the file of the day
        try (Stream<Path> files = Files.list(directory)) {
            List<Path> saved = files.toList();
            assertThat(saved).hasSize(1);
            assertThat(Files.readString(saved.getFirst())).contains("\"logmessage\":\"saved\"");
        }
    }

    @Test
    void testFailureWhenNoItemNamesTheError() {
        // When
        var result = toResult(List.of(FIRST, SECOND), response(true, indexed(), indexed()));

        // Then
        assertThat(result).isInstanceOf(TransmissionResult.Failure.class);
    }

    @Test
    void testSendingDoesNotWaitForTheAnswer() throws Exception {
        // Given: a server accepting connections and never answering
        try (ServerSocket server = new ServerSocket(0)) {
            ElasticsearchConfiguration configuration = ElasticsearchConfiguration.builder()
                    .url("http://localhost:" + server.getLocalPort())
                    .build();
            ElasticsearchLogDelivery delivery = new ElasticsearchLogDelivery(configuration);

            // When
            long start = System.nanoTime();
            var result = delivery.sendBulk(List.of(FIRST));
            Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

            // Then: the request is sent without any thread waiting for its answer
            assertThat(elapsed).isLessThan(Duration.ofSeconds(5));
            assertThat(result).isNotDone();

            // Cleanup: the server gone first, the request failing rather than being left for the close to wait on
            server.close();
            assertThat(result).failsWithin(Duration.ofSeconds(10));
            delivery.close(Duration.ZERO);
        }
    }

}
