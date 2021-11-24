package com.chavaillaz.appender.log4j;

import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class ElasticsearchConfiguration {

    private String user;
    private String password;
    private String url;
    private String index;
    private String indexSuffix;
    private int batchSize;

    private DateTimeFormatter indexSuffixFormatter;

    public String generateIndexName(String utcDateTime) {
        if (utcDateTime == null || indexSuffixFormatter == null) {
            return getIndex();
        } else {
            OffsetDateTime odt = OffsetDateTime.parse(utcDateTime);
            return getIndex() + odt.format(indexSuffixFormatter);
        }
    }

    public String getUser() {
        return user;
    }

    public ElasticsearchConfiguration setUser(String user) {
        this.user = user;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public ElasticsearchConfiguration setPassword(String password) {
        this.password = password;
        return this;
    }

    public boolean hasCredentials() {
        return getUser() != null && getPassword() != null;
    }

    public String getUrl() {
        return url;
    }

    public ElasticsearchConfiguration setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getIndex() {
        return index;
    }

    public ElasticsearchConfiguration setIndex(String index) {
        this.index = index;
        return this;
    }

    public String getIndexSuffix() {
        return indexSuffix;
    }

    public ElasticsearchConfiguration setIndexSuffix(String indexSuffix) {
        this.indexSuffix = indexSuffix;
        try {
            indexSuffixFormatter = DateTimeFormatter.ofPattern(indexSuffix);
        } catch (Exception e) {
            log.error("Format error for elasticIndexSuffix '{}':", indexSuffix, e);
        }
        return this;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public ElasticsearchConfiguration setBatchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }
}
