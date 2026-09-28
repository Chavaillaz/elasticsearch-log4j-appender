package com.chavaillaz.appender.log4j.elastic;

import static org.apache.http.auth.AuthScope.ANY;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import javax.net.ssl.SSLContext;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
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
import org.elasticsearch.client.RestClientBuilder;

/**
 * Elasticsearch specific utility methods.
 */
@UtilityClass
public class ElasticsearchUtils {

    /**
     * Creates a new Elasticsearch client, verifying the certificate Elasticsearch presents unless the
     * configuration trusts its URL, and authenticating with the API key, or the user when there is none.
     *
     * @param configuration The configuration to use
     * @return The Elasticsearch client with the given configuration
     */
    public static ElasticsearchClient createClient(ElasticsearchConfiguration configuration) {
        RestClientBuilder builder = RestClient.builder(HttpHost.create(configuration.getUrl()));
        String apiKey = configuration.getApiKey();
        boolean withApiKey = apiKey != null && !apiKey.isBlank();
        if (withApiKey) {
            builder.setDefaultHeaders(new Header[]{new BasicHeader("Authorization", "ApiKey " + apiKey)});
        }
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            if (!withApiKey) {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(ANY, new UsernamePasswordCredentials(configuration.getUser(), configuration.getPassword()));
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }
            if (configuration.isUrlTrusted()) {
                httpClientBuilder
                        .setSSLContext(createPermissiveContext())
                        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            }
            return httpClientBuilder;
        });
        return createClient(builder.build());
    }

    /**
     * Creates a new Elasticsearch client.
     *
     * @param url        The URL of the Elasticsearch instance to reach
     * @param sslContext The secure socket protocol implementation, used without verifying the host name
     * @param username   The username to authenticate
     * @param password   The password corresponding to the given username
     * @return The Elasticsearch client with the given configuration
     */
    public static ElasticsearchClient createClient(String url, SSLContext sslContext, String username, String password) {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(ANY, new UsernamePasswordCredentials(username, password));
        return createClient(RestClient
                .builder(HttpHost.create(url))
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder
                                .setDefaultCredentialsProvider(credentialsProvider)
                                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                                .setSSLContext(sslContext))
                .build());
    }

    /**
     * Creates a new Elasticsearch client.
     *
     * @param url        The URL of the Elasticsearch instance to reach
     * @param sslContext The secure socket protocol implementation, used without verifying the host name
     * @param apiKey     The encoded API key to authenticate
     * @return The Elasticsearch client with the given configuration
     */
    public static ElasticsearchClient createClient(String url, SSLContext sslContext, String apiKey) {
        Header headerApiKey = new BasicHeader("Authorization", "ApiKey " + apiKey);
        return createClient(RestClient
                .builder(HttpHost.create(url))
                .setDefaultHeaders(new Header[]{headerApiKey})
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder
                                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                                .setSSLContext(sslContext))
                .build());
    }

    /**
     * Creates a new Elasticsearch client using the given REST client
     * and using a customized JSON Mapper with Java 8 Date/Time Module.
     *
     * @param restClient The REST client to use
     * @return The Elasticsearch client with the given configuration
     */
    public static ElasticsearchClient createClient(RestClient restClient) {
        return new ElasticsearchClient(new RestClientTransport(restClient, new Jackson3JsonpMapper()));
    }

    /**
     * Creates a permissive SSL context trusting everything.
     *
     * @return The SSL context
     */
    @SneakyThrows
    public static SSLContext createPermissiveContext() {
        return new SSLContextBuilder()
                .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                .build();
    }

}
