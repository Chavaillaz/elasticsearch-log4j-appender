package com.chavaillaz.appender.log4j;

import static org.apache.http.auth.AuthScope.ANY;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javax.net.ssl.SSLContext;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.elasticsearch.client.RestClient;

@Log4j2
@UtilityClass
public class ElasticsearchUtils {

    /**
     * Searches an environment or system property value.
     *
     * @param key          The key to search in the properties
     * @param defaultValue The default value to use in case the given key is not found
     * @return The value found in environment/system properties or the given default value
     */
    public static String getProperty(String key, String defaultValue) {
        return Optional.ofNullable(System.getenv(key)).orElse(System.getProperty(key, defaultValue));
    }

    /**
     * Creates a new Elasticsearch client.
     *
     * @param url        The URL of the Elasticsearch instance to reach
     * @param sslContext The secure socket protocol implementation
     * @param username   The username to authenticate
     * @param password   The password corresponding to the given username
     * @return The Elasticsearch client with the given configuration
     */
    public static ElasticsearchClient createClient(String url, SSLContext sslContext, String username, String password) {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(ANY, new UsernamePasswordCredentials(username, password));
        return createClient(createRestClient(url, sslContext, credentialsProvider));
    }

    public static ElasticsearchClient createClient(String url, SSLContext sslContext, String apiKey) {
        Header headerApiKey = new BasicHeader("Authorization", "ApiKey " + apiKey);
        return createClient(createRestClient(url, sslContext, null, headerApiKey));
    }

    public static ElasticsearchClient createClient(RestClient restClient) {
        JacksonJsonpMapper jsonMapper = new JacksonJsonpMapper();
        jsonMapper.objectMapper().registerModule(new JavaTimeModule());
        return new ElasticsearchClient(new RestClientTransport(restClient, jsonMapper));
    }

    public static RestClient createRestClient(String url, SSLContext sslContext, CredentialsProvider credentialsProvider, Header... defaultHeaders) {
        return RestClient.builder(HttpHost.create(url))
                .setDefaultHeaders(defaultHeaders)
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder
                                .setDefaultCredentialsProvider(credentialsProvider)
                                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                                .setSSLContext(sslContext))
                .build();
    }

    @SneakyThrows
    public static SSLContext createSSLContext() {
        return new SSLContextBuilder()
                .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                .build();
    }

    /**
     * Gets the current host name of the machine.
     *
     * @return The host name or {@code localhost} if it cannot be determined
     */
    public static String getInitialHostname() {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            log.warn("Host cannot be determined", e);
            return "localhost";
        }
    }

}
