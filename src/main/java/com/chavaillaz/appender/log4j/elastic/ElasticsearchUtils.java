package com.chavaillaz.appender.log4j.elastic;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URISyntaxException;
import java.util.Base64;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;
import javax.net.ssl.SSLContext;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.ssl.SSLContexts;
import org.jspecify.annotations.Nullable;

/**
 * Elasticsearch specific utility methods.
 */
@UtilityClass
public class ElasticsearchUtils {

    /**
     * Creates a new asynchronous Elasticsearch client, verifying the certificate Elasticsearch presents unless
     * the configuration trusts its URL, and authenticating with the API key, or the user when there is none, or
     * not at all for a cluster without security.
     *
     * @param configuration The configuration to use
     * @return The Elasticsearch client with the given configuration
     */
    public static ElasticsearchAsyncClient createClient(ElasticsearchConfiguration configuration) {
        Rest5ClientBuilder builder = Rest5Client.builder(toHost(configuration.getUrl()));
        String apiKey = configuration.getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            builder.setDefaultHeaders(new Header[]{apiKeyAuthorization(apiKey)});
        } else if (configuration.getUser() != null) {
            builder.setDefaultHeaders(new Header[]{basicAuthorization(configuration.getUser(), configuration.getPassword())});
        }
        if (configuration.isUrlTrusted()) {
            SSLContext permissiveContext = createPermissiveContext();
            builder.setConnectionManagerCallback(connectionManager -> connectionManager
                    .setTlsStrategy(new DefaultClientTlsStrategy(permissiveContext, NoopHostnameVerifier.INSTANCE)));
        }
        return createClient(builder.build());
    }

    /**
     * Creates a new asynchronous Elasticsearch client.
     *
     * @param url        The URL of the Elasticsearch instance to reach
     * @param sslContext The secure socket protocol implementation, used without verifying the host name
     * @param username   The username to authenticate
     * @param password   The password corresponding to the given username
     * @return The Elasticsearch client with the given configuration
     */
    public static ElasticsearchAsyncClient createClient(String url, SSLContext sslContext, String username, String password) {
        return createClient(Rest5Client
                .builder(toHost(url))
                .setDefaultHeaders(new Header[]{basicAuthorization(username, password)})
                .setConnectionManagerCallback(connectionManager -> connectionManager
                        .setTlsStrategy(new DefaultClientTlsStrategy(sslContext, NoopHostnameVerifier.INSTANCE)))
                .build());
    }

    /**
     * Creates a new asynchronous Elasticsearch client.
     *
     * @param url        The URL of the Elasticsearch instance to reach
     * @param sslContext The secure socket protocol implementation, used without verifying the host name
     * @param apiKey     The encoded API key to authenticate
     * @return The Elasticsearch client with the given configuration
     */
    public static ElasticsearchAsyncClient createClient(String url, SSLContext sslContext, String apiKey) {
        return createClient(Rest5Client
                .builder(toHost(url))
                .setDefaultHeaders(new Header[]{apiKeyAuthorization(apiKey)})
                .setConnectionManagerCallback(connectionManager -> connectionManager
                        .setTlsStrategy(new DefaultClientTlsStrategy(sslContext, NoopHostnameVerifier.INSTANCE)))
                .build());
    }

    /**
     * Creates a new asynchronous Elasticsearch client sending its requests through the given REST client, and
     * mapping the documents to JSON with Jackson.
     *
     * @param restClient The REST client to use
     * @return The Elasticsearch client with the given configuration
     */
    public static ElasticsearchAsyncClient createClient(Rest5Client restClient) {
        return new ElasticsearchAsyncClient(new Rest5ClientTransport(restClient, new Jackson3JsonpMapper()));
    }

    /**
     * Creates a permissive SSL context trusting everything.
     *
     * @return The SSL context
     */
    @SneakyThrows
    public static SSLContext createPermissiveContext() {
        return SSLContexts.custom()
                .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                .build();
    }

    /**
     * Parses the address of Elasticsearch.
     *
     * @param url The address, in the format {@code scheme://host:port}, the scheme defaulting to {@code http}
     * @return The host to send the requests to
     * @throws IllegalArgumentException If the address is not in that format
     */
    private static HttpHost toHost(String url) {
        try {
            return HttpHost.create(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL of Elasticsearch: " + url, e);
        }
    }

    /**
     * Creates the header authenticating with the given user. Sent with every request, rather than in answer to
     * the challenge of Elasticsearch, which the REST client would otherwise meet anew for each of them, not
     * keeping the authentication from one request to the next.
     *
     * @param username The username to authenticate
     * @param password The password corresponding to the given username, or {@code null} for none
     * @return The authorization header
     */
    private static Header basicAuthorization(String username, @Nullable String password) {
        String credentials = username + ":" + (password != null ? password : "");
        return new BasicHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(UTF_8)));
    }

    /**
     * Creates the header authenticating with the given API key.
     *
     * @param apiKey The encoded API key to authenticate
     * @return The authorization header
     */
    private static Header apiKeyAuthorization(String apiKey) {
        return new BasicHeader("Authorization", "ApiKey " + apiKey);
    }

}
