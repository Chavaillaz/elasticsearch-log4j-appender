# Elasticsearch Appender

[v1]: https://github.com/Chavaillaz/elasticsearch-log4j-appender/wiki/Appender-1.x-‐-Log4j-1.x
[v2]: https://github.com/Chavaillaz/elasticsearch-log4j-appender/wiki/Appender-2.x-‐-Log4j-2.x

![Quality Gate](https://github.com/chavaillaz/elasticsearch-log4j-appender/actions/workflows/sonarcloud.yml/badge.svg)
![Dependency Check](https://github.com/chavaillaz/elasticsearch-log4j-appender/actions/workflows/snyk.yml/badge.svg)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.chavaillaz/elasticsearch-log4j-appender/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.chavaillaz/elasticsearch-log4j-appender)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Elasticsearch appender allows you to send log events directly from Log4j to an elastic cluster. The delivery of logs is
asynchronous (i.e. not on the main thread) and therefore will not block execution of the program.

| Appender version | Log4j version | Elasticsearch version | Documentation   |
|------------------|---------------|-----------------------|-----------------|
| 1.0.0            | 1.2.17        | 7.x                   | [Version 1][v1] |
| 2.0.0            | 2.17.1        | 7.x                   | [Version 2][v2] |
| 2.1.0            | 2.17.2        | 8.x                   | [Version 2][v2] |
| 2.1.1            | 2.19.0        | 8.x                   | [Version 2][v2] |
| 2.1.2            | 2.20.0        | 8.x                   | [Version 2][v2] |
| 2.1.3            | 2.24.1        | 8.x                   | [Version 2][v2] |
| 3.0.0            | 2.24.1        | 8.x                   | See below       |
| 3.0.1            | 2.24.3        | 8.x                   | See below       |

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

In the Log4j configuration file, add a new appender `ElasticsearchAppender` using package 
`com.chavaillaz.appender.log4j.elastic` with the following properties:

| Appender property | Environment / System variable | Default value               | Description                                                                                                                             |
|-------------------|-------------------------------|-----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| Application       | APP                           | `unknown`                   | The name of the application generating the logs.                                                                                        |
| Host              | HOST                          | Machine host name           | The name of the host on which the application is running.                                                                               |
| Environment       | ENV                           | `local`                     | The name of the environment in which the application is running.                                                                        |
| Converter         | CONVERTER                     | `[...].DefaultLogConverter` | The path of the class used to convert logging events into key/value documents to be stored.                                             |
| Index             | INDEX                         | `ha`                        | The name of the Elasticsearch index to which the documents are sent.                                                                    |
| IndexSuffix       | INDEX_SUFFIX                  | -                           | The suffix added to the index name (using current date) in a format pattern suitable for `DateTimeFormatter`.                           |
| Url               | ELASTIC_URL                   | -                           | The address of Elasticsearch in the format `scheme://host:port`.                                                                        |
| User              | ELASTIC_USER                  | -                           | The username to use as credentials to access Elasticsearch.                                                                             |
| Password          | ELASTIC_PASSWORD              | -                           | The password to use as credentials to access Elasticsearch.                                                                             |
| ApiKey            | ELASTIC_API_KEY               | -                           | The API key (already encoded) to use as credentials to access Elasticsearch.                                                            |
| FlushThreshold    | -                             | `100`                       | The threshold number of messages triggering the transmission of documents to the server.                                                |
| FlushInterval     | -                             | `5000`                      | The time (ms) between two automatic flushes, which are triggering the transmission of logs, even if not reaching the defined threshold. |

Note that `Url` is the only mandatory configuration, except if you need to overwrite the default value of another ones.

## XML file example

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="info" packages="com.chavaillaz.appender.log4j.elastic">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
        </Console>
        <ElasticsearchAppender name="Elasticsearch">
            <PatternLayout pattern="%msg"/>
            <Application>myApplication</Application>
            <Environment>local</Environment>
            <Converter>com.chavaillaz.appender.log4j.DefaultLogConverter</Converter>
            <Index>ha</Index>
            <IndexSuffix>-yyyy.MM.dd</IndexSuffix>
            <Url>http://localhost:9300</Url>
            <User>elastic</User>
            <Password>changeme</Password>
            <FlushThreshold>100</FlushThreshold>
            <FlushInterval>5000</FlushInterval>
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