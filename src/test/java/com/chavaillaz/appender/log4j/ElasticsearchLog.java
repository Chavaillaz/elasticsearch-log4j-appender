package com.chavaillaz.appender.log4j;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ElasticsearchLog {

    private LocalDateTime datetime;
    private String host;
    private String environment;
    private String application;
    private String logger;
    private String level;
    private String logmessage;
    private String thread;
    private String stacktrace;

}