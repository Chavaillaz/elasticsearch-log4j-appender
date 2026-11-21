package com.chavaillaz.appender.log4j.elastic;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.chavaillaz.appender.DeliveryOptions;

class ElasticsearchConfigurationTest {

    private static final DateTimeFormatter DAILY = DateTimeFormatter.ofPattern("-yyyy.MM.dd");

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
                .setHost("my-host");

        // When
        String host = builder.build().getLogConfiguration().getHost();

        // Then
        assertThat(host).isEqualTo("my-host");
    }

    @Test
    void testHostDefaultsToTheMachine() {
        // Given: a builder created in code, which no configuration of Log4j2 fills
        ElasticsearchAppender.Builder builder = ElasticsearchAppender.builder()
                .setName("appender");

        // When / Then: the name of the machine, as the ${hostName} of a configuration gives it
        assertThat(builder.build().getLogConfiguration().getHost()).isEqualTo(NetUtils.getLocalHostname());
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
    void testUrlGivenThroughALookup() throws Exception {
        // Given: the URL given by a system property, as an environment variable through ${env:ELASTIC_URL}
        System.setProperty("ELASTIC_URL", "http://localhost:1");

        // When
        LoggerContext context;
        try {
            context = startContext("""
                    <ElasticsearchAppender name="Elasticsearch" Url="${sys:ELASTIC_URL}"/>
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
    void testDroppedDocumentsReachTheNestedDeadLetterAppender(@TempDir Path directory) throws Exception {
        // Given: a cluster that cannot be reached, a single attempt per document, and a file appender nested in
        // the element for the documents dropped
        Path file = directory.resolve("dead-letters.ndjson");
        LoggerContext context = startContext("""
                <ElasticsearchAppender name="Elasticsearch" Url="http://localhost:1" FlushThreshold="1" MaxConsecutiveFailures="1">
                    <File name="DeadLetters" fileName="%s">
                        <PatternLayout pattern="%%m%%n"/>
                    </File>
                </ElasticsearchAppender>
                """.formatted(file));

        // When
        context.getLogger("dead-letters").error("saved");
        context.stop();

        // Then: the document given up on is a line of the file, which the appender closed as it stopped
        assertThat(Files.readAllLines(file, UTF_8)).singleElement()
                .satisfies(line -> assertThat(line).contains("\"logmessage\":\"saved\""));
    }

    @Test
    void testConfigurationsWithTheSameSettingsAreEqual() {
        // When
        ElasticsearchConfiguration first = configurationWithEverySetting();
        ElasticsearchConfiguration second = configurationWithEverySetting();

        // Then
        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second).isNotEqualTo("configuration");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("otherSettings")
    void testConfigurationsDifferingInOneSettingAreNotEqual(
            String setting, UnaryOperator<ElasticsearchConfiguration.ElasticsearchConfigurationBuilder> change) {
        // Given
        ElasticsearchConfiguration configuration = configurationWithEverySetting();

        // When
        ElasticsearchConfiguration other = change.apply(configuration.toBuilder()).build();

        // Then
        assertThat(other).isNotEqualTo(configuration);
    }

    static Stream<Arguments> otherSettings() {
        return Stream.of(
                otherSetting("application", builder -> builder.application("other")),
                otherSetting("host", builder -> builder.host("other")),
                otherSetting("environment", builder -> builder.environment("other")),
                otherSetting("index", builder -> builder.index("other")),
                otherSetting("indexSuffix", builder -> builder.indexSuffix(DateTimeFormatter.ofPattern("-yyyy"))),
                otherSetting("url", builder -> builder.url("https://other:9200")),
                otherSetting("urlTrusted", builder -> builder.urlTrusted(false)),
                otherSetting("user", builder -> builder.user("other")),
                otherSetting("password", builder -> builder.password("other")),
                otherSetting("apiKey", builder -> builder.apiKey("other")),
                otherSetting("deliveryOptions", builder -> builder.deliveryOptions(DeliveryOptions.defaults())));
    }

    @Test
    void testToBuilderKeepsEverySetting() {
        // Given
        ElasticsearchConfiguration configuration = configurationWithEverySetting();

        // When
        ElasticsearchConfiguration copy = configuration.toBuilder().build();

        // Then
        assertThat(copy).isEqualTo(configuration);
    }

    @Test
    void testToStringLeavesTheCredentialsOut() {
        // When
        String text = configurationWithEverySetting().toString();

        // Then: the settings are listed, but for the values of the password and the API key
        assertThat(text).contains("application=application", "url=https://localhost:9200", "user=elastic");
        assertThat(text).doesNotContain("my-password", "my-api-key");
    }

    @Test
    void testBuilderKeepsTheAttributesSet() {
        // When
        ElasticsearchAppender.Builder builder = ElasticsearchAppender.builder()
                .setIndex("logs")
                .setIndexSuffix("-yyyy.MM.dd")
                .setUrl("https://localhost:9200")
                .setUrlTrusted(true)
                .setUser("elastic")
                .setPassword("my-password")
                .setApiKey("my-api-key");

        // Then
        assertThat(builder.getIndex()).isEqualTo("logs");
        assertThat(builder.getIndexSuffix()).isEqualTo("-yyyy.MM.dd");
        assertThat(builder.getUrl()).isEqualTo("https://localhost:9200");
        assertThat(builder.isUrlTrusted()).isTrue();
        assertThat(builder.getUser()).isEqualTo("elastic");
        assertThat(builder.getPassword()).isEqualTo("my-password");
        assertThat(builder.getApiKey()).isEqualTo("my-api-key");
    }

    private static Arguments otherSetting(
            String setting, UnaryOperator<ElasticsearchConfiguration.ElasticsearchConfigurationBuilder> change) {
        return Arguments.of(setting, change);
    }

    /**
     * Builds a configuration giving each setting a value of its own.
     *
     * @return The configuration
     */
    private static ElasticsearchConfiguration configurationWithEverySetting() {
        return ElasticsearchConfiguration.builder()
                .application("application")
                .host("host")
                .environment("environment")
                .index("logs")
                .indexSuffix(DAILY)
                .url("https://localhost:9200")
                .urlTrusted(true)
                .user("elastic")
                .password("my-password")
                .apiKey("my-api-key")
                .deliveryOptions(ElasticsearchConfiguration.DEFAULT_DELIVERY_OPTIONS)
                .build();
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

}
