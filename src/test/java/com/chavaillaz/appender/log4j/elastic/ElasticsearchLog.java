package com.chavaillaz.appender.log4j.elastic;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ElasticsearchLog(
        LocalDateTime datetime,
        String host,
        String environment,
        String application,
        String logger,
        String level,
        String logmessage,
        String thread,
        String stacktrace,
        String key) {
}
