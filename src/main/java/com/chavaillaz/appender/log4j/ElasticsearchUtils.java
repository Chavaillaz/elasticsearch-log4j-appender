package com.chavaillaz.appender.log4j;

import co.elastic.clients.base.Transport;
import co.elastic.clients.base.rest_client.RestClientTransport;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

import static org.apache.http.auth.AuthScope.ANY;

@Slf4j
@UtilityClass
public class ElasticsearchUtils {

    public static String getProperty(String key, String defaultValue) {
        return Optional.ofNullable(System.getenv(key)).orElse(System.getProperty(key, defaultValue));
    }

    public static ElasticsearchClient createClient(String url, String username, String password) {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(ANY, new UsernamePasswordCredentials(username, password));

        RestClient restClient = RestClient.builder(HttpHost.create(url))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
                .build();

        JacksonJsonpMapper jsonMapper = new JacksonJsonpMapper();
        jsonMapper.objectMapper().registerModule(new JavaTimeModule());
        Transport transport = new RestClientTransport(restClient, jsonMapper);
        return new ElasticsearchClient(transport);
    }

    public static String getInitialHostname() {
        String host = "localhost";
        try {
            host = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            log.warn("Host cannot be determined", e);
        }
        return host;
    }

}
