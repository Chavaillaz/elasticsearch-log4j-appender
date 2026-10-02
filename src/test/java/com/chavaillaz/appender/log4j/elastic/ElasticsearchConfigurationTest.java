package com.chavaillaz.appender.log4j.elastic;

import static com.chavaillaz.appender.log4j.CommonUtils.getProperty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.Test;

import com.chavaillaz.appender.DeliveryOptions;
import com.chavaillaz.appender.LogConfiguration;
import com.chavaillaz.appender.log4j.DefaultLogConverter;
import com.chavaillaz.appender.log4j.LogConverter;

class ElasticsearchConfigurationTest {

    @Test
    void testGenerateIndexNameDate() {
        // Given
        String index = "idx";
        String suffix = "yyyy.MM.dd";
        ElasticsearchConfiguration configuration = ElasticsearchConfiguration.builder()
                .index(index)
                .indexSuffix(DateTimeFormatter.ofPattern("-" + suffix))
                .build();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(suffix);
        OffsetDateTime date = now();

        // When
        String indexName = configuration.generateIndexName(date.toString());

        // Then
        assertThat(indexName).isEqualTo(index + "-" + formatter.format(date));
    }

    @Test
    void testGenerateIndexNameWithoutSuffix() {
        // Given
        ElasticsearchConfiguration configuration = ElasticsearchConfiguration.builder()
                .index("idx")
                .build();

        // When
        String indexName = configuration.generateIndexName(now());

        // Then
        assertThat(indexName).isEqualTo("idx");
    }

    @Test
    void testDeliveryOptionsFromAttributes() {
        // Given
        ElasticsearchAppender.Builder builder = ElasticsearchAppender.builder()
                .setName("appender")
                .setFlushLevel("warn")
                .setMaxRequestBytes(5_242_880)
                .setRetryBackoff(0)
                .setCloseTimeout(5_000);

        // When
        DeliveryOptions options = builder.build().getLogConfiguration().getDeliveryOptions();

        // Then: the attributes set are applied, the others keeping their defaults
        assertThat(options.flushLevel()).isEqualTo(Level.WARN);
        assertThat(options.maxRequestBytes()).isEqualTo(5_242_880);
        assertThat(options.retryBackoff()).isZero();
        assertThat(options.closeTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(options.flushThreshold()).isEqualTo(100);
        assertThat(options.maxPendingDocuments()).isEqualTo(DeliveryOptions.defaults().maxPendingDocuments());
    }

    @Test
    void testDefaultsSplitRequestsPastTenMebibytes() {
        // When: neither attributes nor options given
        DeliveryOptions fromAttributes = ElasticsearchAppender.builder()
                .setName("appender")
                .build()
                .getLogConfiguration()
                .getDeliveryOptions();
        DeliveryOptions fromConfiguration = ElasticsearchConfiguration.builder()
                .build()
                .getDeliveryOptions();

        // Then: the same defaults either way, a bulk request never growing past 10 MiB
        assertThat(fromAttributes).isEqualTo(ElasticsearchConfiguration.DEFAULT_DELIVERY_OPTIONS);
        assertThat(fromConfiguration).isEqualTo(ElasticsearchConfiguration.DEFAULT_DELIVERY_OPTIONS);
        assertThat(fromAttributes.maxRequestBytes()).isEqualTo(10_485_760);
        assertThat(fromAttributes.flushThreshold()).isEqualTo(100);
    }

    @Test
    void testHostNameSetIsKept() {
        // Given
        ElasticsearchAppender.Builder builder = ElasticsearchAppender.builder()
                .setName("appender")
                .setHostName("my-host");

        // When
        String host = builder.build().getLogConfiguration().getHost();

        // Then
        assertThat(host).isEqualTo("my-host");
    }

    @Test
    void testHostNameIsOnlyResolvedWhenBuilding() {
        // Given: a builder, which Log4j2 creates before setting the attributes of the configuration
        ElasticsearchAppender.Builder builder = ElasticsearchAppender.builder()
                .setName("appender");

        // When / Then: nothing looked up yet, however long the lookup takes, the host possibly being given later
        assertThat(builder.getHostName()).isNull();
        assertThat(builder.build().getLogConfiguration().getHost()).isNotBlank();
    }

    @Test
    void testUnknownFlushLevelFailsTheConfiguration() {
        // Given
        ElasticsearchAppender.Builder builder = ElasticsearchAppender.builder()
                .setName("appender")
                .setFlushLevel("WARNING");

        // When / Then: a misspelled level fails the configuration rather than disabling the flush silently
        assertThatThrownBy(builder::build).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testCredentialsAreHiddenFromTheStatusLogger() throws Exception {
        // Given: every message of the status logger, down to the debug one listing the attributes of the appender
        List<String> messages = new CopyOnWriteArrayList<>();
        StatusListener listener = new StatusListener() {

            @Override
            public void log(StatusData data) {
                messages.add(data.getFormattedStatus());
            }

            @Override
            public Level getStatusLevel() {
                return Level.DEBUG;
            }

            @Override
            public void close() {
                // Nothing to release
            }

        };
        StatusLogger.getLogger().registerListener(listener);

        // When
        try {
            startContext("""
                    <ElasticsearchAppender name="Elasticsearch" Url="http://localhost:1"
                                           User="elastic" Password="my-password" ApiKey="my-api-key"/>
                    """).stop();
        } finally {
            StatusLogger.getLogger().removeListener(listener);
        }

        // Then: the attributes are listed, but for the values of the password and the API key
        assertThat(messages).anyMatch(message -> message.contains("User=\"elastic\""));
        assertThat(messages).noneMatch(message -> message.contains("my-password") || message.contains("my-api-key"));
    }

    @Test
    void testMissingUrlFailsTheConfiguration() throws Exception {
        // Given: no URL given by the ELASTIC_URL variable either
        assumeTrue(getProperty("ELASTIC_URL", null) == null);

        // When
        LoggerContext context = startContext("""
                <ElasticsearchAppender name="Elasticsearch"/>
                """);

        // Then: the appender is not created, rather than created unable to send anything
        try {
            assertThat(context.getConfiguration().getAppenders()).doesNotContainKey("Elasticsearch");
        } finally {
            context.stop();
        }
    }

    @Test
    void testUrlGivenByTheVariableIsEnough() throws Exception {
        // Given: the URL given by the ELASTIC_URL variable, here as a system property
        assumeTrue(System.getenv("ELASTIC_URL") == null);
        System.setProperty("ELASTIC_URL", "http://localhost:1");

        // When
        LoggerContext context;
        try {
            context = startContext("""
                    <ElasticsearchAppender name="Elasticsearch"/>
                    """);
        } finally {
            System.clearProperty("ELASTIC_URL");
        }

        // Then
        try {
            ElasticsearchAppender appender = context.getConfiguration().getAppender("Elasticsearch");
            assertThat(appender).isNotNull();
            assertThat(appender.getLogConfiguration().getUrl()).isEqualTo("http://localhost:1");
        } finally {
            context.stop();
        }
    }

    @Test
    void testWrongConverter() {
        // Given
        ElasticsearchAppender appender = ElasticsearchAppender.builder()
                .setName("appender")
                .setElasticConverter("package.does.not.exist.NotFoundConverter")
                .build();

        // When
        LogConverter converter = appender.getLogConverter();

        // Then
        assertThat(converter.getClass()).isEqualTo(DefaultLogConverter.class);
    }

    @Test
    void testNamedConverterIsBuiltWithTheConfiguration() {
        // Given
        ElasticsearchAppender appender = ElasticsearchAppender.builder()
                .setName("appender")
                .setApplicationName("my-application")
                .setElasticConverter(ApplicationConverter.class.getName())
                .build();

        // When
        LogConverter converter = appender.getLogConverter();

        // Then
        assertThat(converter).isInstanceOf(ApplicationConverter.class);
        assertThat(converter.convert(Log4jLogEvent.newBuilder().build())).containsEntry("application", "my-application");
    }

    /**
     * Starts a dedicated context configured in XML, as an application configures the appender, rather than
     * reconfiguring the one the other tests log through.
     *
     * @param appender The XML element of the appender, named {@code Elasticsearch}
     * @return The started context, to stop once done with it
     * @throws IOException If the configuration cannot be read
     */
    private static LoggerContext startContext(String appender) throws IOException {
        String xml = """
                <Configuration status="ERROR">
                    <Appenders>
                        %s
                    </Appenders>
                    <Loggers>
                        <Root level="INFO">
                            <AppenderRef ref="Elasticsearch"/>
                        </Root>
                    </Loggers>
                </Configuration>
                """.formatted(appender);
        LoggerContext context = new LoggerContext(ElasticsearchConfigurationTest.class.getSimpleName());
        context.start(new XmlConfiguration(context, new ConfigurationSource(new ByteArrayInputStream(xml.getBytes(UTF_8)))));
        return context;
    }

    public static class ApplicationConverter implements LogConverter {

        private final LogConfiguration configuration;

        public ApplicationConverter(LogConfiguration configuration) {
            this.configuration = configuration;
        }

        @Override
        public Map<String, Object> convert(LogEvent event) {
            return Map.of("application", configuration.getApplication());
        }

    }

}
