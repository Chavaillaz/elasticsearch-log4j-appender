/**
 * The Log4j2 appender shipping logs to Elasticsearch, a plugin Log4j2 builds by reflection, which its package
 * is opened to.
 * <p>
 * The Elasticsearch client and its REST client declare no module name: they are required by the names the JDK
 * derives from their jars, {@code elasticsearch.java} and {@code elasticsearch.rest.client}, which the jars keep
 * as long as they are not renamed. The warning of javac against a transitive dependency on them is
 * suppressed, the API of the appender exposing the client as it is.
 */
@SuppressWarnings("requires-transitive-automatic")
module com.chavaillaz.appender.log4j.elastic {

    requires transitive com.chavaillaz.appender;
    requires transitive elasticsearch.java;
    requires transitive elasticsearch.rest.client;
    requires org.apache.httpcomponents.httpasyncclient;
    requires org.apache.httpcomponents.httpclient;
    requires org.apache.httpcomponents.httpcore;
    requires tools.jackson.databind;
    requires static lombok;

    // Declared on behalf of the clients, which as automatic modules declare none of what they need, so that the
    // modules they load classes from are resolved along with them on the module path
    requires jakarta.json;
    requires org.apache.commons.logging;

    exports com.chavaillaz.appender.log4j.elastic;
    opens com.chavaillaz.appender.log4j.elastic to org.apache.logging.log4j.core;

}
