package com.chavaillaz.appender.log4j;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static com.chavaillaz.appender.log4j.ElasticsearchUtils.createClient;
import static java.lang.Thread.sleep;
import static java.net.InetAddress.getLocalHost;
import static java.util.stream.Collectors.toList;
import static org.apache.log4j.Level.INFO;
import static org.apache.log4j.LogManager.getRootLogger;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class ElasticsearchAppenderTest {

    public static final String ELASTICSEARCH_USERNAME = "elastic";
    public static final String ELASTICSEARCH_PASSWORD = "changeme";
    public static final DockerImageName ELASTICSEARCH_IMAGE = DockerImageName
            .parse("docker.elastic.co/elasticsearch/elasticsearch")
            .withTag("7.15.2");

    protected static ElasticsearchAppender createAppender(String application, String hostname, String elastic) {
        ElasticsearchAppender appender = new ElasticsearchAppender();
        appender.setApplicationName(application);
        appender.setHostName(hostname);
        appender.setElasticUrl(elastic);
        // Default user and password for the docker image from Elastic
        appender.setElasticUser(ELASTICSEARCH_USERNAME);
        appender.setElasticPassword(ELASTICSEARCH_PASSWORD);
        // Need to be done synchronously for the test
        appender.setElasticParallelExecution(false);
        appender.activateOptions();
        return appender;
    }

    protected static List<ElasticsearchLog> searchLog(ElasticsearchClient client, String index, String id) throws IOException {
        return client.search(search -> search
                                .index(index + "*")
                                .query(query -> query
                                        .match(term -> term
                                                .field("logmessage")
                                                .query(id))),
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

            // When
            appender.append(new LoggingEvent(getClass().getSimpleName(), getRootLogger(), INFO, id, null));
            sleep(1000);

            // Then
            List<ElasticsearchLog> logs = searchLog(client, appender.getElasticIndex(), id);
            assertEquals(1, logs.size());
            assertEquals(appender.getHostName(), logs.get(0).getHost());
            assertEquals(appender.getEnvironmentName(), logs.get(0).getEnvironment());
            assertEquals(appender.getApplicationName(), logs.get(0).getApplication());
            assertEquals(INFO.toString(), logs.get(0).getLevel());
            assertEquals(id, logs.get(0).getLogmessage());
        }
    }

}
