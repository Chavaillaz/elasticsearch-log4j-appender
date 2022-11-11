# Elasticsearch Appender

![Quality Gate](https://github.com/chavaillaz/elasticsearch-log4j-appender/actions/workflows/sonarcloud.yml/badge.svg)
![Dependency Check](https://github.com/chavaillaz/elasticsearch-log4j-appender/actions/workflows/snyk.yml/badge.svg)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.chavaillaz/elasticsearch-log4j-appender/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.chavaillaz/elasticsearch-log4j-appender)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Elasticsearch appender allows you to send log events directly from Log4j to an elastic cluster. The delivery of logs is
asynchronous (i.e. not on the main thread) and therefore will not block execution of the program.

| Appender version | Log4j version | Elasticsearch version |
|------------------|---------------|-----------------------|
| 1.0.0            | 1.2.17        | 7.x                   |
| 2.0.0            | 2.17.1        | 7.x                   |
| 2.1.0            | 2.17.2        | 8.x                   |
| 2.1.1            | 2.19.0        | 8.x                   |

## Installation

The dependency is available in maven central (see badge and table above for the version):

```xml

<dependency>
    <groupId>com.chavaillaz</groupId>
    <artifactId>elasticsearch-log4j-appender</artifactId>
</dependency>
```

You then have to configure log4j in order to include this appender (see configuration section below).

## Configuration

In the configuration file, add a new appender `ElasticsearchAppender` (from package `com.chavaillaz.appender.log4j`)
with the following properties **(please note that for Log4j2 they all start with an uppercase letter)**:

- `applicationName` (default `unknown`). It can also be specified as environment variable or system
  property `APPLICATION`.
- `hostName` (default is the machine host name). It can also be specified as environment variable or system
  property `HOST`.
- `environmentName` (default `local`). It can be specified also as environment variable or system property `ENV`.
- `elasticConverter` (default `com.chavaillaz.appender.log4j.converter.DefaultEventConverter`) is the class used to
  convert a logging event into a key/value document to be stored in Elasticsearch. It can also be specified as
  environment variable or system property `CONVERTER`.
- `elasticIndex` (default `ha`) and `elasticIndexSuffix` (default `-yyyy.MM.dd`) form together the index name where the
  messages are sent to. Note that `elasticIndexSuffix` must contain a format pattern suitable for `DateTimeFormatter`.
  They can also both be specified with environment variables or system properties `INDEX` and `INDEX_SUFFIX`.
- `elasticUrl` is the address of the server (or its load balancer) in the format `[scheme://][host]:[port]`. The scheme
  is optional and defaults to `http`.
- `elasticUser` and `elasticPassword` are the credentials for the server.
- `elasticParallelExecution` (default `true`) specifies the way the messages are sent to the server
  (`true` send them in a separate thread and `false` send them sequentially).
- `elasticBatchSize` (default `1`) is the number of messages threshold triggering the sending to the server.
- `elasticBatchInitialDelay` (default `1000`) is the time in milliseconds before a first batch of messages is sent to
  the server (after appender startup, even if the batch of messages is incomplete, meaning not reaching the threshold).
- `elasticBatchDelay` (default `1000`) is the time in milliseconds between two cleanups of incomplete batches. If after
  this time there are less than `elasticBatchSize` messages waiting to be sent, the batch is sent nonetheless. Note that
  once `elasticBatchSize` messages are waiting, the batch is sent immediately, without any delay.

Note that `elasticUrl` is the only mandatory configuration to give, except if you need to overwrite the default value of
another ones.

### XML file example (log4j version 2)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="info" packages="com.chavaillaz.appender.log4j">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
        </Console>
        <ElasticsearchAppender name="Elasticsearch">
            <PatternLayout pattern="%msg"/>
            <ApplicationName>myApplication</ApplicationName>
            <EnvironmentName>local</EnvironmentName>
            <ElasticConverter>com.chavaillaz.appender.log4j.converter.DefaultEventConverter</ElasticConverter>
            <ElasticUrl>http://localhost:9300</ElasticUrl>
            <ElasticIndex>ha</ElasticIndex>
            <ElasticIndexSuffix>-yyyy.MM.dd</ElasticIndexSuffix>
            <ElasticUser>elastic</ElasticUser>
            <ElasticPassword>changeme</ElasticPassword>
            <ElasticParallelExecution>true</ElasticParallelExecution>
            <ElasticBatchSize>10</ElasticBatchSize>
            <ElasticBatchInitialDelay>1000</ElasticBatchInitialDelay>
            <ElasticBatchDelay>1000</ElasticBatchDelay>
        </ElasticsearchAppender>
    </Appenders>
    <Loggers>
        <Root level="info">
            <AppenderRef ref="Console"/>
            <AppenderRef ref="Elasticsearch" additivity="false"/>
        </Root>
    </Loggers>
</Configuration>
```

### XML file example (log4j version 1)

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
        <param name="elasticConverter" value="com.package.CustomEventConverter"/>
        <param name="elasticUrl" value="myElasticsearchUrl"/>
        <param name="elasticIndex" value="myIndex"/>
        <param name="elasticIndexSuffix" value="-yyyy.MM.dd"/>
        <param name="elasticUser" value="myUser"/>
        <param name="elasticPassword" value="myPassword"/>
        <param name="elasticParallelExecution" value="true"/>
        <param name="elasticBatchSize" value="10"/>
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

### Property file example (log4j version 1)

```
log4j.appender.ELASTIC=com.chavaillaz.appender.log4j.ElasticsearchAppender
log4j.appender.ELASTIC.applicationName=myApplication
log4j.appender.ELASTIC.environmentName=myEnvironment
log4j.appender.ELASTIC.elasticConverter=com.package.CustomEventConverter
log4j.appender.ELASTIC.elasticUrl=myElasticsearchUrl
log4j.appender.ELASTIC.elasticIndex=myIndex
log4j.appender.ELASTIC.elasticIndexSuffix=-yyyy.MM.dd
log4j.appender.ELASTIC.elasticUser=myUser
log4j.appender.ELASTIC.elasticPassword=myPassword
log4j.appender.ELASTIC.elasticParallelExecution=true
log4j.appender.ELASTIC.elasticBatchSize=10
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