# Elasticsearch Appender

[v1]: https://github.com/Chavaillaz/elasticsearch-log4j-appender/wiki/Appender-1.x-‐-Log4j-1.x
[v2]: https://github.com/Chavaillaz/elasticsearch-log4j-appender/wiki/Appender-2.x-‐-Log4j-2.x

![Quality Gate](https://github.com/chavaillaz/elasticsearch-log4j-appender/actions/workflows/code-quality.yml/badge.svg)
![Dependency Check](https://github.com/chavaillaz/elasticsearch-log4j-appender/actions/workflows/dependency-check.yml/badge.svg)
[![Maven Central](https://maven-badges.sml.io/sonatype-central/com.chavaillaz/elasticsearch-log4j-appender/badge.svg?subject=Maven%20Central&color=blue)](https://maven-badges.sml.io/sonatype-central/com.chavaillaz/elasticsearch-log4j-appender)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Elasticsearch appender allows you to send log events directly from Log4j to an Elasticsearch cluster. The delivery of
logs is asynchronous (i.e. not on the main thread) and therefore will not block execution of the program.

| Appender version | Log4j version | Elastic version | Java version | Documentation   |
|------------------|---------------|-----------------|--------------|-----------------|
| 1.0.0            | 1.2.17        | 7.x             | 11+          | [Version 1][v1] |
| 2.0.0            | 2.17.1        | 7.x             | 17+          | [Version 2][v2] |
| 2.1.0            | 2.17.2        | 8.x             | 17+          | [Version 2][v2] |
| 2.1.1            | 2.19.0        | 8.x             | 17+          | [Version 2][v2] |
| 2.1.2            | 2.20.0        | 8.x             | 17+          | [Version 2][v2] |
| 2.1.3            | 2.24.1        | 8.x             | 17+          | [Version 2][v2] |
| 3.0.0            | 2.24.1        | 8.x             | 17+          | See below       |
| 3.0.1            | 2.24.3        | 8.x             | 17+          | See below       |
| 3.0.2            | 2.25.1        | 8.x             | 17+          | See below       |
| 3.1.0            | 2.25.1        | 9.x             | 17+          | See below       |
| 3.1.1            | 2.25.3        | 9.x             | 17+          | See below       |
| 3.1.2            | 2.25.4        | 9.x             | 17+          | See below       |
| 4.0.0            | 2.26.1        | 9.x             | 21+          | See below       |

## Installation

The dependency is available in Maven Central (see badge and table above for the version):

```xml
<dependency>
    <groupId>com.chavaillaz</groupId>
    <artifactId>elasticsearch-log4j-appender</artifactId>
</dependency>
```

On the module path, the appender is the module `com.chavaillaz.appender.log4j.elastic`, which opens its
package to Log4j2 to be built as a plugin. The Elasticsearch client declaring no module name, the appender
requires it by the names Java derives from its jars, `elasticsearch.java` and `elasticsearch.rest5.client`,
which these jars keep as long as they keep the names Maven gives them.

You then have to configure Log4j to include this appender (see configuration section below).

## Configuration

In the Log4j configuration file, add a new appender `ElasticsearchAppender` using package
`com.chavaillaz.appender.log4j.elastic` with the following properties:

| Appender property          | Environment / System variable | Default value               | Description                                                                                                                               |
|----------------------------|-------------------------------|-----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| Application                | APP                           | `unknown`                   | The name of the application generating the logs.                                                                                          |
| Host                       | HOST                          | Machine host name           | The name of the host on which the application is running.                                                                                 |
| Environment                | ENV                           | `local`                     | The name of the environment in which the application is running.                                                                          |
| Converter                  | CONVERTER                     | `[...].DefaultLogConverter` | The class converting logging events into key/value documents, built through its public constructor taking the configuration.              |
| Index                      | INDEX                         | `ha`                        | The name of the Elasticsearch index to which the documents are sent.                                                                      |
| IndexSuffix                | INDEX_SUFFIX                  | -                           | The suffix added to the index name, the date of each event in a format pattern suitable for `DateTimeFormatter`.                          |
| Url                        | ELASTIC_URL                   | -                           | The address of Elasticsearch in the format `scheme://host:port`.                                                                          |
| UrlTrusted                 | -                             | `false`                     | The flag to trust any certificate Elasticsearch presents over HTTPS, a self-signed one for instance, without verifying its host name.     |
| User                       | ELASTIC_USER                  | -                           | The username to use as credentials to access Elasticsearch.                                                                               |
| Password                   | ELASTIC_PASSWORD              | -                           | The password to use as credentials to access Elasticsearch.                                                                               |
| ApiKey                     | ELASTIC_API_KEY               | -                           | The API key (already encoded) to use as credentials to access Elasticsearch.                                                              |
| DeadLetterFile             | DEAD_LETTER_FILE              | -                           | The file to which the documents the appender discards are saved, one JSON object per line, the date being inserted before its extension.  |
| FlushThreshold             | -                             | `100`                       | The threshold number of messages triggering the transmission of documents to the server.                                                  |
| FlushInterval              | -                             | `5000`                      | The time (ms) between two automatic flushes, which are triggering the transmission of logs, even if not reaching the defined threshold.   |
| FlushLevel                 | -                             | -                           | The minimum level (e.g. `WARN`) of an event flushing the documents right after it, rather than waiting for the threshold or the interval. |
| MaxConcurrentTransmissions | -                             | `4`                         | The maximum number of bulk requests sent at the same time.                                                                                |
| MaxPendingDocuments        | -                             | `10000`                     | The maximum number of documents waiting to be sent or retried, the oldest ones being discarded past it.                                   |
| MaxRequestDocuments        | -                             | `2147483647`                | The maximum number of documents in a bulk request, a bigger batch being split into several requests.                                      |
| MaxPendingBytes            | -                             | `9223372036854775807`       | The maximum estimated size (bytes) of the documents waiting to be sent or retried, the oldest ones being discarded past it.               |
| MaxRequestBytes            | -                             | `10485760`                  | The maximum estimated size (bytes) of a bulk request, a heavier batch being split into several requests.                                  |
| MaxConsecutiveFailures     | -                             | `5`                         | The maximum number of failed attempts in a row for the same documents before they are discarded.                                          |
| MaxQueuedEvents            | -                             | `10000`                     | The maximum number of events waiting to be converted, the oldest one being discarded past it.                                             |
| RetryBackoff               | -                             | `1000`                      | The base delay (ms) before retrying a failed transmission, doubling with each consecutive failure, `0` retrying at once.                  |
| MaxRetryBackoff            | -                             | `30000`                     | The maximum delay (ms) between two retries.                                                                                               |
| CloseTimeout               | -                             | `5000`                      | The maximum time (ms) a stop waits for the documents still pending, while the `shutdownTimeout` of the Log4j configuration is `0`.        |

Note that `Url` is the only mandatory configuration, except if you need to override the default value of other ones.

The properties from `FlushThreshold` on tune the delivery of the documents, as the
[common library](https://github.com/chavaillaz/common-log4j-appender#configuration-reference) details them.
`MaxRequestBytes` keeps each bulk request under the `http.max_content_length` of Elasticsearch (100MB by
default), so that the documents piling up during an outage are sent back in several requests rather than
in a single one Elasticsearch refuses for its size: raise it along with that setting, or lower it for a
cluster accepting less.

The certificate of Elasticsearch is verified unless `UrlTrusted` is set, where versions before 4.0 trusted any certificate.

## XML file example

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="INFO">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
        </Console>
        <ElasticsearchAppender name="ElasticSearch">
            <Application>myApplication</Application>
            <Environment>local</Environment>
            <Converter>com.chavaillaz.appender.log4j.DefaultLogConverter</Converter>
            <Index>ha</Index>
            <IndexSuffix>-yyyy.MM</IndexSuffix>
            <Url>https://localhost:9200</Url>
            <User>elastic</User>
            <Password>changeme</Password>
            <DeadLetterFile>/var/log/myApplication/dead-letters.ndjson</DeadLetterFile>
            <FlushThreshold>100</FlushThreshold>
            <FlushInterval>5000</FlushInterval>
            <FlushLevel>ERROR</FlushLevel>
        </ElasticsearchAppender>
    </Appenders>
    <Loggers>
        <Root level="INFO">
            <AppenderRef ref="Console"/>
            <AppenderRef ref="ElasticSearch"/>
        </Root>
    </Loggers>
</Configuration>
```

The appender reports its own failures, a transmission that failed or documents it had to discard, on the
status logger of Log4j2 rather than through a logger of its own, which would feed them back into the
appender. The `status` attribute of the `Configuration` element shows them from the level it names.

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