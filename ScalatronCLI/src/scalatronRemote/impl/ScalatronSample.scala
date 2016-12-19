package scalatronRemote.impl

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
import scalatronRemote.api.ScalatronRemote
import scala.util.parsing.json.JSONFormat
import scalatronRemote.api.ScalatronRemote._
import scalatronRemote.impl.Connection.HttpFailureCodeException
import org.apache.http.HttpStatus
import scalatronRemote.api.ScalatronRemote.BuildResult.BuildMessage

case class ScalatronSample(name: String,
                           resourceUrl: String, // e.g. "/api/samples/{sample}"
                           scalatron: ScalatronRemoteImpl)
    extends ScalatronRemote.Sample {
  override def toString = name + " -> " + resourceUrl

  def sourceFiles: SourceFileCollection = {
    try {
      /*
           {
               "files" :
               [
                   { "filename" : "Bot.scala", "code" : "class ControlFunctionFactory { ... }" },
                   { "filename" : "Util.scala", "code" : "class View { ... }" }
               ]
           }
       */
      val jsonOpt = scalatron.connection.GET_json(resourceUrl)
      val jsonMap = jsonOpt.asMap
      val sourceFilePairs = jsonMap.asKVStrings("files", "filename", "code")
      sourceFilePairs.map(sf => SourceFile(sf._1, sf._2))
    } catch {
      case e: HttpFailureCodeException =>
        e.httpCode match {
          case HttpStatus.SC_UNAUTHORIZED =>
            // not logged on as a user or as Administrator
            throw ScalatronException.NotAuthorized(e.reason)
          case HttpStatus.SC_NOT_FOUND =>
            // this sample does not exist on the server
            throw ScalatronException.NotFound(e.reason)
          case HttpStatus.SC_INTERNAL_SERVER_ERROR =>
            // the sample's source files could not be read
            throw ScalatronException.InternalServerError(e.reason)
          case _ =>
            throw e // rethrow
        }
    }
  }

  def delete(): Unit = {
    try {
      scalatron.connection.DELETE(resourceUrl)
    } catch {
      case e: HttpFailureCodeException =>
        e.httpCode match {
          case HttpStatus.SC_FORBIDDEN =>
            // tried to delete a protected (built-in) sample
            throw ScalatronException.Forbidden(e.reason)
          case HttpStatus.SC_UNAUTHORIZED =>
            // not logged on as Administrator
            throw ScalatronException.NotAuthorized(e.reason)
          case HttpStatus.SC_NOT_FOUND =>
            // this sample does not exist on the server
            throw ScalatronException.NotFound(e.reason)
          case HttpStatus.SC_BAD_REQUEST =>
            // sample name contained invalid characters
            throw ScalatronException.IllegalUserName(e.reason)
          case HttpStatus.SC_INTERNAL_SERVER_ERROR =>
            // this sample 's files could not be deleted
            throw ScalatronException.InternalServerError(e.reason)
          case _ =>
            throw e // rethrow
        }
    }
  }
}
