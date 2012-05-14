/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */

package scalatronRemote.impl

import scalatronRemote.api.ScalatronRemote
import scalatronRemote.impl.Connection.HttpFailureCodeException
import org.apache.http.HttpStatus
import scalatronRemote.api.ScalatronRemote.{ScalatronException, SourceFile}


case class ScalatronVersion(
    id: String,
    label: String,
    date: Long,
    resourceUrl: String, // e.g. "/api/users/{user}/versions/{id}"
    user: ScalatronUser)
    extends ScalatronRemote.Version
{
    override def toString = id + " -> " + resourceUrl

    def sourceFiles = {
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
            val jsonOpt = user.scalatron.connection.GET_json(resourceUrl)
            val jsonMap = jsonOpt.asMap
            val sourceFilePairs = jsonMap.asKVStrings("files", "filename", "code")
            sourceFilePairs.map(sf => SourceFile(sf._1, sf._2))
        } catch {
            case e: HttpFailureCodeException =>
                e.httpCode match {
                    case HttpStatus.SC_UNAUTHORIZED =>
                        // not logged on as this user or as Administrator
                        throw ScalatronException.NotAuthorized(e.reason)
                    case HttpStatus.SC_NOT_FOUND =>
                        // this user does not exist on the server
                        throw ScalatronException.NotFound(e.reason)
                    case HttpStatus.SC_INTERNAL_SERVER_ERROR =>
                        // the user's source files could not be read
                        throw ScalatronException.InternalServerError(e.reason)
                    case _ =>
                        throw e // rethrow
                }
        }
    }
}