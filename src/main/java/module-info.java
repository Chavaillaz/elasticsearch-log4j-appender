/**
 * The Log4j2 appender shipping logs to Elasticsearch, a plugin Log4j2 builds by reflection, which its package
 * is opened to.
 * <p>
 * The Elasticsearch client and its REST client declare no module name: they are required by the names the JDK
 * derives from their jars, {@code elasticsearch.java} and {@code elasticsearch.rest5.client}, which the jars keep
 * as long as they are not renamed. The warnings of javac against requiring such an automatic module, as the
 * jars of HttpClient 5 are too, and against requiring one transitively, are suppressed: these libraries offer
 * no other module to require, and the API of the appender exposes the client as it is.
 */
@SuppressWarnings({"requires-automatic", "requires-transitive-automatic"})
module com.chavaillaz.appender.log4j.elastic {

    requires transitive com.chavaillaz.appender;
    requires transitive elasticsearch.java;
    requires transitive elasticsearch.rest5.client;
    requires org.apache.httpcomponents.client5.httpclient5;
    requires org.apache.httpcomponents.core5.httpcore5;
    requires tools.jackson.databind;

    // Declared on behalf of the clients, which as automatic modules declare none of what they need, so that the
    // modules they load classes from are resolved along with them on the module path
    requires jakarta.json;
    requires org.apache.commons.logging;

    exports com.chavaillaz.appender.log4j.elastic;
    opens com.chavaillaz.appender.log4j.elastic to org.apache.logging.log4j.core;

}
