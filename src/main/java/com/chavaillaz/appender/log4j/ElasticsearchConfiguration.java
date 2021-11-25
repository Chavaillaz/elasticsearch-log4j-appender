package com.chavaillaz.appender.log4j;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Getter
@Setter
public class ElasticsearchConfiguration {

    private String user;
    private String password;
    private String url;
    private String index;
    private String indexSuffix;
    private DateTimeFormatter indexSuffixFormatter;
    private int batchSize;

    public void setIndexSuffix(String indexSuffix) {
        this.indexSuffix = indexSuffix;
        try {
            this.indexSuffixFormatter = DateTimeFormatter.ofPattern(indexSuffix);
        } catch (Exception e) {
            log.error("Format error for elasticIndexSuffix '{}'", indexSuffix, e);
        }
    }

    public boolean hasCredentials() {
        return getUser() != null && getPassword() != null;
    }

    public String generateIndexName(String utcDateTime) {
        if (utcDateTime == null || indexSuffixFormatter == null) {
            return getIndex();
        } else {
            OffsetDateTime odt = OffsetDateTime.parse(utcDateTime);
            return getIndex() + odt.format(indexSuffixFormatter);
        }
    }

}
