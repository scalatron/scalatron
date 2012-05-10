package scalatron.webServer.rest

import org.codehaus.jackson.jaxrs.JacksonJsonProvider
import java.lang.reflect.Type
import java.io.OutputStream
import resources._
import scalatron.scalatron.api.Scalatron
import javax.ws.rs.Produces
import java.lang.Class
import javax.ws.rs.core.{Response, MultivaluedMap, MediaType}
import java.util.logging.Level
import javax.ws.rs.ext.{ContextResolver, ExceptionMapper, Provider}


/** The central init point for the Scalatron RESTful web API.
  */
case class RestApplication(scalatron: Scalatron, verbose: Boolean) extends javax.ws.rs.core.Application {

    java.util.logging.Logger.getLogger("com.sun.jersey").setLevel(Level.FINER);

    val resources = new java.util.HashSet[Object]();
    resources.add(new MapProvider())
    resources.add(new ListProvider())
    resources.add(new JacksonJsonProvider())
    resources.add(new ScalatronProvider(scalatron))
    resources.add(new VerbosityProvider(Verbosity(verbose)))
    resources.add(new UnauthorizedExceptionMapper());

    val resourcesCls = new java.util.HashSet[Class[_]]();
    resourcesCls.add(classOf[ApiResource])
    resourcesCls.add(classOf[UsersResource])
    resourcesCls.add(classOf[SessionResource])
    resourcesCls.add(classOf[SandboxesResource])
    resourcesCls.add(classOf[SourcesResource])
    resourcesCls.add(classOf[VersionsResource])
    resourcesCls.add(classOf[UnpublishedPublishResource])
    resourcesCls.add(classOf[SourcesBuildResource])
    resourcesCls.add(classOf[PublishResource])
    resourcesCls.add(classOf[UnpublishedResource])
    resourcesCls.add(classOf[SamplesResource])

    override def getSingletons = resources

    override def getClasses = resourcesCls

    def getUserName = "Hello"
}


@Provider
@Produces(Array("application/json"))
class UnauthorizedExceptionMapper extends ExceptionMapper[SecurityException] {
    def toResponse(p1: SecurityException) = Response.status(401).build();
}


/**
  * Will translate scala Lists to java Lists to make serialization work.
  */
@Provider
@Produces(Array("application/json"))
class ListProvider extends JacksonJsonProvider {

    private val arrayListClass = classOf[java.util.ArrayList[_]];

    override def isWriteable(c: Class[_],
        gt: Type,
        annotations: Array[java.lang.annotation.Annotation],
        mediaType: MediaType): Boolean = {
        classOf[List[_]].isAssignableFrom(c) &&
            super.isWriteable(arrayListClass, arrayListClass,
                annotations, MediaType.APPLICATION_JSON_TYPE);
    }

    override def writeTo(t: Object,
        c: Class[_],
        gt: Type,
        annotations: Array[java.lang.annotation.Annotation],
        mediaType: MediaType,
        httpHeaders: MultivaluedMap[String, Object],
        entityStream: OutputStream) {

        val l = t.asInstanceOf[List[_]];
        val al = new java.util.ArrayList[Any]();
        for(m <- l) {
            al.add(m)
        }

        super.writeTo(al, arrayListClass, arrayListClass,
            annotations, mediaType, httpHeaders, entityStream)
    }
}


/**
  * Will translate scala Maps into java Maps to make serialization work.
  */
@Provider
@Produces(Array("application/json"))
class MapProvider extends JacksonJsonProvider {

    private val mapClass = classOf[java.util.HashMap[_, _]];

    override def isWriteable(c: Class[_], gt: Type, annotations: Array[java.lang.annotation.Annotation], mediaType: MediaType): Boolean = {

        classOf[Map[_, _]].isAssignableFrom(c) && super.isWriteable(mapClass, mapClass,
            annotations, MediaType.APPLICATION_JSON_TYPE);
    }

    override def writeTo(t: Object,
        c: Class[_],
        gt: Type,
        annotations: Array[java.lang.annotation.Annotation],
        mediaType: MediaType,
        httpHeaders: MultivaluedMap[String, Object],
        entityStream: OutputStream) {

        val l = t.asInstanceOf[Map[_, _]];
        val al = new java.util.HashMap[Any, Any]();
        for((k, v) <- l) {
            al.put(k, v)
        }

        super.writeTo(al, mapClass, mapClass,
            annotations, mediaType, httpHeaders, entityStream)
    }
}


/** This context is used to inject the scalatron instance into the resources.
  * @param scalatron the scalatron instance.
  */
@Provider
class ScalatronProvider(scalatron: Scalatron) extends ContextResolver[Scalatron] {
    def getContext(cls: Class[_]) = if(cls.isInstance(scalatron)) scalatron else null
}


/** This context is used to inject a verbosity flag into the resources.
  * @param verbosity the verbosity flag
  */
@Provider
class VerbosityProvider(verbosity: Verbosity) extends ContextResolver[Verbosity] {
    def getContext(cls: Class[_]) = if(cls.isInstance(verbosity)) verbosity else null
}

case class Verbosity(verbose: Boolean)
