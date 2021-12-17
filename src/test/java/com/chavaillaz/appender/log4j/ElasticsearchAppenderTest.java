package com.chavaillaz.appender.log4j;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static com.chavaillaz.appender.log4j.ElasticsearchUtils.createClient;
import static java.lang.Thread.sleep;
import static java.net.InetAddress.getLocalHost;
import static java.util.stream.Collectors.toList;
import static org.apache.logging.log4j.Level.INFO;
import static org.apache.logging.log4j.LogManager.getRootLogger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
class ElasticsearchAppenderTest {

    public static final String ELASTICSEARCH_USERNAME = "elastic";
    public static final String ELASTICSEARCH_PASSWORD = "changeme";
    public static final DockerImageName ELASTICSEARCH_IMAGE = DockerImageName
            .parse("docker.elastic.co/elasticsearch/elasticsearch")
            .withTag("7.15.2");

    protected static ElasticsearchAppender createAppender(String application, String hostname, String elastic) {
        ElasticsearchAppender.Builder builder = ElasticsearchAppender.builder();
        builder.setName("ElasticAppender");
        builder.setApplicationName(application);
        builder.setHostName(hostname);
        builder.setElasticUrl(elastic);
        // Default user and password for the docker image from Elastic
        builder.setElasticUser(ELASTICSEARCH_USERNAME);
        builder.setElasticPassword(ELASTICSEARCH_PASSWORD);
        // Need to be done synchronously for the test
        builder.setElasticParallelExecution(false);
        ElasticsearchAppender appender = builder.build();
        appender.start();
        return appender;
    }

    protected static List<ElasticsearchLog> searchLog(ElasticsearchClient client, String index, String id) throws IOException {
        return client.search(search -> search
                                .index(index + "*")
                                .query(query -> query
                                        .match(term -> term
                                                .field("logmessage")
                                                .query(value -> value.stringValue(id)))),
                        ElasticsearchLog.class)
                .hits()
                .hits()
                .stream()
                .map(Hit::source)
                .collect(toList());
    }

    @Test
    void systemTestWithElasticsearch() throws Exception {
        try (ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE)) {
            container.start();

            // Given
            String id = UUID.randomUUID().toString();
            ElasticsearchClient client = createClient(container.getHttpHostAddress(), ELASTICSEARCH_USERNAME, ELASTICSEARCH_PASSWORD);
            ElasticsearchAppender appender = createAppender("myApplication", getLocalHost().getHostName(), container.getHttpHostAddress());
            MDC.put("key", "value");

            // When
            Log4jLogEvent event = Log4jLogEvent.newBuilder()
                    .setMessage(new SimpleMessage(id))
                    .setLoggerName(getRootLogger().getName())
                    .setThrown(new RuntimeException())
                    .setLevel(INFO)
                    .build();
            appender.append(event);
            appender.stop();
            sleep(1000);

            // Then
            List<ElasticsearchLog> logs = searchLog(client, appender.getConfiguration().getIndex(), id);
            assertEquals(1, logs.size());
            assertEquals(appender.getConfiguration().getHostName(), logs.get(0).getHost());
            assertEquals(appender.getConfiguration().getEnvironmentName(), logs.get(0).getEnvironment());
            assertEquals(appender.getConfiguration().getApplicationName(), logs.get(0).getApplication());
            assertEquals(INFO.toString(), logs.get(0).getLevel());
            assertEquals(id, logs.get(0).getLogmessage());
            assertEquals("value", logs.get(0).getKey());
            assertNotNull(logs.get(0).getStacktrace());
        }
    }

}
