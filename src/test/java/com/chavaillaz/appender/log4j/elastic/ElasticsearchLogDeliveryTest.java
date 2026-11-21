package com.chavaillaz.appender.log4j.elastic;

import static com.chavaillaz.appender.log4j.elastic.ElasticsearchLogDelivery.toResult;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.ServerSocket;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.OperationType;
import org.junit.jupiter.api.Test;

import com.chavaillaz.appender.DocumentSizeEstimator;
import com.chavaillaz.appender.TransmissionResult;

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
    void testFailureWhenNoItemNamesTheError() {
        // When
        var result = toResult(List.of(FIRST, SECOND), response(true, indexed(), indexed()));

        // Then
        assertThat(result).isInstanceOf(TransmissionResult.Failure.class);
    }

    @Test
    void testSendingDoesNotWaitForTheAnswer() throws Exception {
        ElasticsearchLogDelivery delivery;
        CompletableFuture<TransmissionResult<Map<String, Object>>> result;

        // Given: a server accepting connections and never answering
        try (ServerSocket server = new ServerSocket(0)) {
            ElasticsearchConfiguration configuration = ElasticsearchConfiguration.builder()
                    .url("http://localhost:" + server.getLocalPort())
                    .build();
            delivery = new ElasticsearchLogDelivery(configuration);

            // When
            long start = System.nanoTime();
            result = delivery.sendBulk(List.of(FIRST));
            Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

            // Then: the request is sent without any thread waiting for its answer
            assertThat(elapsed).isLessThan(Duration.ofSeconds(5));
            assertThat(result).isNotDone();
        }

        // Cleanup: the server gone first, the request failing rather than being left for the close to wait on
        assertThat(result).failsWithin(Duration.ofSeconds(10));
        delivery.close(Duration.ZERO);
    }

    @Test
    void testIndexIsNamedAfterTheDateOfTheEvent() throws Exception {
        // Given: a daily index, and an event of a day long gone
        ElasticsearchLogDelivery delivery = new ElasticsearchLogDelivery(dailyIndexConfiguration());

        // When
        String index = delivery.indexOf(Map.of("datetime", "2020-01-15T12:00:00.000Z"));

        // Then: the day of the event, whenever it is sent
        assertThat(index).isEqualTo("logs-2020.01.15");
        delivery.close();
    }

    @Test
    void testIndexOfADocumentWithoutItsDateIsTheOneOfTheDayItIsSent() throws Exception {
        // Given
        ElasticsearchLogDelivery delivery = new ElasticsearchLogDelivery(dailyIndexConfiguration());

        // When: no date, or one in a format of its own
        String withoutDate = delivery.indexOf(Map.of("logmessage", "first"));
        String withOtherDate = delivery.indexOf(Map.of("datetime", "15/01/2020"));

        // Then
        String today = "logs" + now().format(DateTimeFormatter.ofPattern("-yyyy.MM.dd"));
        assertThat(withoutDate).isEqualTo(today);
        assertThat(withOtherDate).isEqualTo(today);
        delivery.close();
    }

    private static ElasticsearchConfiguration dailyIndexConfiguration() {
        return ElasticsearchConfiguration.builder()
                .url("http://localhost:1")
                .index("logs")
                .indexSuffix(DateTimeFormatter.ofPattern("-yyyy.MM.dd"))
                .build();
    }

}
