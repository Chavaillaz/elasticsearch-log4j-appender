# Elasticsearch Appender

[![Build Status](https://travis-ci.org/Chavaillaz/elasticsearch-log4j-appender.svg?branch=master)](https://travis-ci.org/Chavaillaz/elasticsearch-log4j-appender)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=com.chavaillaz%3Aelasticsearch-log4j-appender&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.chavaillaz%3Aelasticsearch-log4j-appender)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.chavaillaz/elasticsearch-log4j-appender/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.chavaillaz/elasticsearch-log4j-appender)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Elasticsearch appender allow you to send log events directly from log4j 1.2 to an elastic cluster. 
The delivery of logs is asynchronous (i.e. not on the main thread) and therefore will not block execution of the program.

## Installation

The dependency is available in maven central (see badge for the version):
```xml
<dependency>
    <groupId>com.chavaillaz</groupId>
    <artifactId>elasticsearch-log4j-appender</artifactId>
</dependency>
```

You then have to configure log4j in order to include this appender (see configuration section below).

## Configuration

In the configuration file `log4j.properties` or `log4j.xml`, add a new appender 
`com.chavaillaz.appender.log4j.ElasticsearchAppender` with the following properties:

- `applicationName` (default `unknown`) is sent as field `application`.
- `environmentName` (default `local`) is sent as field `environment`.
  It can be specified also as environment variable or system property `ENV`.
- `elasticUrl` is the address of the server (or its load balancer) in the format `[scheme://][host]:[port]`. 
  The scheme is optional and defaults to `http`.
- `elasticIndex` (default `ha`) and `elasticIndexSuffix` (default `-yyyy.MM.dd`) form together 
  the index name where the messages are sent to. Note that `elasticIndexSuffix` 
  must contain a format pattern suitable for `DateTimeFormatter`.
- `elasticUser` and `elasticPassword` are the credentials for the server.
- `elasticParallelExecution` (default `true`) specifies the way the messages are sent to the server 
  (`true` send them in a separate thread and `false` send them sequentially).
- `elasticBatchSize` (default `1`) is the number of messages triggering the sending to the server.
- `elasticBatchInitialDelay` (default `1000`) is the time in milliseconds before a first batch of messages 
  is sent to the server (after the appender startup, even if it's incomplete).
- `elasticBatchDelay` (default `1000`) is the time in milliseconds between two cleanups of incomplete batches. 
  If after this time there are less than `elasticBatchSize` messages waiting to be sent, the batch is sent nonetheless. 
  Note that once `elasticBatchSize` messages are waiting, the batch is sent immediately, without any delay.

### Property file example

```
log4j.appender.ELASTIC=com.chavaillaz.appender.log4j.ElasticsearchAppender
log4j.appender.ELASTIC.applicationName=myApplication
log4j.appender.ELASTIC.environmentName=myEnvironment
log4j.appender.ELASTIC.elasticUrl=myElasticsearch
log4j.appender.ELASTIC.elasticIndex=myIndex
log4j.appender.ELASTIC.elasticIndexSuffix=-yyyy.MM.dd
log4j.appender.ELASTIC.elasticUser=myUser
log4j.appender.ELASTIC.elasticPassword=myPassword
log4j.appender.ELASTIC.elasticParallelExecution=true
log4j.appender.ELASTIC.elasticBatchSize=1
log4j.appender.ELASTIC.elasticBatchInitialDelay=1000
log4j.appender.ELASTIC.elasticBatchDelay=1000

# Avoid to propagate the logs from ElasticAppender to itself (cyclic)
log4j.logger.com.chavaillaz.appender=WARN, CONSOLE
log4j.logger.org.apache.http=WARN, CONSOLE
log4j.logger.org.elasticsearch.client=WARN, CONSOLE
log4j.additivity.com.chavaillaz.appender=false
log4j.additivity.org.apache.http=false
log4j.additivity.org.elasticsearch.client=false
```

### XML file example
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE log4j:configuration SYSTEM "log4j.dtd">
<log4j:configuration xmlns:log4j="http://jakarta.apache.org/log4j/">

    <appender name="ConsoleAppender" class="org.apache.log4j.ConsoleAppender">
        <layout class="org.apache.log4j.PatternLayout">
            <param name="ConversionPattern" value="%d{ISO8601} %p (%t) [%c{1}::%M] - %m%n"/>
        </layout>
    </appender>

    <appender name="ElasticAppender" class="com.chavaillaz.appender.log4j.ElasticsearchAppender">
        <param name="applicationName" value="myApplication"/>
        <param name="environmentName" value="myEnvironment"/>
        <param name="elasticUrl" value="myElasticsearch"/>
        <param name="elasticIndex" value="myIndex"/>
        <param name="elasticIndexSuffix" value="-yyyy.MM.dd"/>
        <param name="elasticUser" value="myUser"/>
        <param name="elasticPassword" value="myPassword"/>
        <param name="elasticParallelExecution" value="true"/>
        <param name="elasticBatchSize" value="1"/>
        <param name="elasticBatchInitialDelay" value="1000"/>
        <param name="elasticBatchDelay" value="1000"/>
    </appender>

    <!-- Avoid to propagate the logs from ElasticAppender to itself (cyclic) -->
    <category name="com.chavaillaz.appender" additivity="false">
        <priority value="warn"/>
        <appender-ref ref="ConsoleAppender"/>
    </category>

    <root>
        <priority value="info"/>
        <appender-ref ref="ConsoleAppender"/>
        <appender-ref ref="ElasticAppender"/>
    </root>

</log4j:configuration>
```

## Contributing

If you have a feature request or found a bug, you can:

- Write an issue
- Create a pull request

If you want to contribute then

- Please write tests covering all your changes
- Ensure you didn't break the build by running `mvn test`
- Fork the repo and create a pull request

## License

This project is under Apache 2.0 License.