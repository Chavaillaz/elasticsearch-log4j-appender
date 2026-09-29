package com.chavaillaz.appender.log4j.elastic;

import static com.chavaillaz.appender.log4j.elastic.ElasticsearchUtils.createClient;
import static java.net.InetAddress.getLocalHost;
import static java.time.Duration.ofSeconds;
import static org.apache.logging.log4j.Level.INFO;
import static org.apache.logging.log4j.LogManager.getRootLogger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.security.cert.CertPathBuilderException;
import java.util.List;
import java.util.UUID;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.security.CreateApiKeyRequest;
import javax.net.ssl.SSLContext;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

class ElasticsearchAppenderTest {

    // Default user and password for the docker image from Elastic
    public static final String ELASTICSEARCH_USERNAME = "elastic";
    public static final String ELASTICSEARCH_PASSWORD = "changeme";
    public static final DockerImageName ELASTICSEARCH_IMAGE = DockerImageName
            .parse("docker.elastic.co/elasticsearch/elasticsearch")
            .withTag("9.4.4");

    // A single cluster for every test, starting one taking most of their time
    private static final ElasticsearchContainer CONTAINER = new ElasticsearchContainer(ELASTICSEARCH_IMAGE);

    protected static String createApiKey(ElasticsearchClient client) throws IOException {
        return client.security()
                .createApiKey(new CreateApiKeyRequest.Builder()
                        .name("TestKey")
                        .refresh(Refresh.False)
                        .build())
                .encoded();
    }

    protected static ElasticsearchAppender createAppender(String application, String hostname, String elastic) {
        ElasticsearchAppender.Builder builder = ElasticsearchAppender.builder();
        builder.setName("ElasticAppender");
        builder.setApplicationName(application);
        builder.setHostName(hostname);
        builder.setElasticUrl(elastic);
        builder.setElasticUrlTrusted(true);
        builder.setElasticUser(ELASTICSEARCH_USERNAME);
        builder.setElasticPassword(ELASTICSEARCH_PASSWORD);
        builder.setFlushInterval(500);
        builder.setFlushThreshold(5);
        return builder.build();
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
                .toList();
    }

    @BeforeAll
    static void startContainer() {
        CONTAINER.start();
    }

    @AfterAll
    static void stopContainer() {
        CONTAINER.stop();
    }

    @Test
    void systemTestWithElasticsearch() throws Exception {
        // Given
        String id = UUID.randomUUID().toString();
        String logger = getRootLogger().getClass().getCanonicalName();
        String address = "https://" + CONTAINER.getHttpHostAddress();
        SSLContext ssl = CONTAINER.createSslContextFromCa();
        ElasticsearchClient client = new ElasticsearchClient(createClient(address, ssl, ELASTICSEARCH_USERNAME, ELASTICSEARCH_PASSWORD)._transport());
        ElasticsearchClient apiClient = new ElasticsearchClient(createClient(address, ssl, createApiKey(client))._transport());
        ElasticsearchAppender appender = createAppender("myApplication", getLocalHost().getHostName(), address);
        ThreadContext.put("key", "value");

        // When
        appender.start();
        Log4jLogEvent event = Log4jLogEvent.newBuilder()
                .setMessage(new SimpleMessage(id))
                .setLoggerFqcn(logger)
                .setThrown(new RuntimeException())
                .setLevel(INFO)
                .build();
        appender.append(event);
        Thread.sleep(ofSeconds(5));
        appender.stop();

        // Then
        List<ElasticsearchLog> logs = searchLog(apiClient, appender.getLogConfiguration().getIndex(), id);
        assertEquals(1, logs.size());
        ElasticsearchLog log = logs.get(0);
        assertEquals(appender.getLogConfiguration().getHost(), log.getHost());
        assertEquals(appender.getLogConfiguration().getEnvironment(), log.getEnvironment());
        assertEquals(appender.getLogConfiguration().getApplication(), log.getApplication());
        assertEquals(logger, log.getLogger());
        assertEquals(INFO.toString(), log.getLevel());
        assertEquals(id, log.getLogmessage());
        assertEquals("value", log.getKey());
        assertNotNull(log.getStacktrace());
    }

    @Test
    void testCertificateIsVerifiedUnlessTrusted() throws Exception {
        // Given: the self-signed certificate of the container, which the JVM does not trust
        ElasticsearchConfiguration configuration = ElasticsearchConfiguration.builder()
                .url("https://" + CONTAINER.getHttpHostAddress())
                .user(ELASTICSEARCH_USERNAME)
                .password(ELASTICSEARCH_PASSWORD)
                .build();

        // When / Then: the connection is refused, unless trusting any certificate
        try (ElasticsearchAsyncClient client = createClient(configuration)) {
            assertThat(client.info()).failsWithin(ofSeconds(30))
                    .withThrowableThat()
                    .havingRootCause()
                    .isInstanceOf(CertPathBuilderException.class);
        }
        try (ElasticsearchAsyncClient client = createClient(configuration.toBuilder().urlTrusted(true).build())) {
            assertThat(client.info()).succeedsWithin(ofSeconds(30));
        }
    }

}
