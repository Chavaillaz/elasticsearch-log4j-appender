package com.chavaillaz.appender.log4j;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import javax.net.ssl.SSLContext;
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

class ElasticsearchAppenderTest {

    public static final String ELASTICSEARCH_USERNAME = "elastic";
    public static final String ELASTICSEARCH_PASSWORD = "changeme";
    public static final DockerImageName ELASTICSEARCH_IMAGE = DockerImageName
            .parse("docker.elastic.co/elasticsearch/elasticsearch")
            .withTag("8.1.2");

    protected static ElasticsearchAppender createAppender(String application, String hostname, String elastic, boolean parallel) {
        ElasticsearchAppender.Builder builder = ElasticsearchAppender.builder();
        builder.setName("ElasticAppender");
        builder.setApplicationName(application);
        builder.setHostName(hostname);
        builder.setElasticUrl(elastic);
        // Default user and password for the docker image from Elastic
        builder.setElasticUser(ELASTICSEARCH_USERNAME);
        builder.setElasticPassword(ELASTICSEARCH_PASSWORD);
        // Need to be done synchronously for the test
        builder.setElasticParallelExecution(parallel);
        builder.setElasticBatchDelay(10);
        builder.setElasticBatchSize(5);
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

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void systemTestWithElasticsearch(boolean parallel) throws Exception {
        try (ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE)) {
            container.start();

            // Given
            String id = UUID.randomUUID().toString();
            String logger = getRootLogger().getClass().getCanonicalName();
            String address = "https://" + container.getHttpHostAddress();
            SSLContext ssl = new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build(); //container.createSslContextFromCa()
            ElasticsearchClient client = createClient(address, ssl, ELASTICSEARCH_USERNAME, ELASTICSEARCH_PASSWORD);
            ElasticsearchAppender appender = createAppender("myApplication", getLocalHost().getHostName(), address, parallel);
            ThreadContext.put("key", "value");

            // When
            Log4jLogEvent event = Log4jLogEvent.newBuilder()
                    .setMessage(new SimpleMessage(id))
                    .setLoggerFqcn(logger)
                    .setThrown(new RuntimeException())
                    .setLevel(INFO)
                    .build();
            appender.append(event);
            sleep(5000);
            appender.stop();

            // Then
            List<ElasticsearchLog> logs = searchLog(client, appender.getConfiguration().getIndex(), id);
            assertEquals(1, logs.size());
            ElasticsearchLog log = logs.get(0);
            assertEquals(appender.getConfiguration().getHostName(), log.getHost());
            assertEquals(appender.getConfiguration().getEnvironmentName(), log.getEnvironment());
            assertEquals(appender.getConfiguration().getApplicationName(), log.getApplication());
            assertEquals(logger, log.getLogger());
            assertEquals(INFO.toString(), log.getLevel());
            assertEquals(id, log.getLogmessage());
            assertEquals("value", log.getKey());
            assertNotNull(log.getStacktrace());
        }
    }

}
