/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatronRemote.impl

import scalatronRemote.api.ScalatronRemote
import scala.util.parsing.json.JSONFormat
import scalatronRemote.api.ScalatronRemote._
import scalatronRemote.impl.Connection.HttpFailureCodeException
import org.apache.http.HttpStatus
import scalatronRemote.api.ScalatronRemote.BuildResult.BuildMessage


case class ScalatronUser(
    name: String,
    sessionUrl: String, // e.g. "/api/users/Administrator/session"
    resourceUrl: String, // e.g. "/api/users/Administrator"
    scalatron: ScalatronRemoteImpl)
    extends ScalatronRemote.User
{
    override def toString = name + " -> " + sessionUrl

    def isAdministrator = name == ScalatronRemote.Constants.AdminUserName

    var resourceMap: Option[Map[String, String]] = None // valid after log-in

    def resource(key: String): String = resourceMap match {
        case None => throw new IllegalStateException("not logged on")
        case Some(map) => map.get(key) match {
            case None => throw new IllegalStateException("unknown resource: " + key)
            case Some(url) => url
        }
    }

    //----------------------------------------------------------------------------------------------
    // authentication
    //----------------------------------------------------------------------------------------------

    def logOn(password: String): Unit = {
        try {
            val jsonOpt = scalatron.connection.POST_json_json(sessionUrl, "{\"password\":\"" + password + "\"}")
            val jsonMap = jsonOpt.asMap
            resourceMap = Some(jsonMap.asKVStrings("resources", "name", "url"))
        } catch {
            case e: HttpFailureCodeException =>
                e.httpCode match {
                    case HttpStatus.SC_UNAUTHORIZED =>
                        // wrong password for this user
                        throw ScalatronException.NotAuthorized(e.reason)
                    case HttpStatus.SC_NOT_FOUND =>
                        // no user with this user name exists
                        throw ScalatronException.NotFound(e.reason)
                    case HttpStatus.SC_INTERNAL_SERVER_ERROR =>
                        // user configuration file could not be read
                        throw ScalatronException.InternalServerError(e.reason)
                    case _ =>
                        throw e // rethrow
                }
        }
    }

    def logOff(): Unit = {
        try {
            scalatron.connection.DELETE(sessionUrl)
            resourceMap = None
        } catch {
            case e: HttpFailureCodeException =>
                e.httpCode match {
                    case HttpStatus.SC_UNAUTHORIZED =>
                        // not logged on as this user
                        throw ScalatronException.NotAuthorized(e.reason)
                    case _ =>
                        throw e // rethrow
                }
        }
    }


    //----------------------------------------------------------------------------------------------
    // account management
    //----------------------------------------------------------------------------------------------

    def delete(): Unit = {
        try {
            scalatron.connection.DELETE(resourceUrl)
            resourceMap = None
        } catch {
            case e: HttpFailureCodeException =>
                e.httpCode match {
                    case HttpStatus.SC_FORBIDDEN =>
                        // tried to delete the Administrator account
                        throw ScalatronException.Forbidden(e.reason)
                    case HttpStatus.SC_UNAUTHORIZED =>
                        // not logged on as this user or as Administrator
                        throw ScalatronException.NotAuthorized(e.reason)
                    case HttpStatus.SC_NOT_FOUND =>
                        // this user does not exist on the server
                        throw ScalatronException.NotFound(e.reason)
                    case HttpStatus.SC_BAD_REQUEST =>
                        // user name contained invalid characters
                        throw ScalatronException.IllegalUserName(e.reason)
                    case HttpStatus.SC_INTERNAL_SERVER_ERROR =>
                        // this user's workspace files could not be deleted
                        throw ScalatronException.InternalServerError(e.reason)
                    case _ =>
                        throw e // rethrow
                }
        }
    }


    def updateAttributes(map: Map[String, String]): Unit = {
        try {
            val attributesJson =
                "{ \"attributes\" : [ " +
                    map.map(kv => {"{ \"name\" : \"" + kv._1 + "\", \"value\" : " + JSONFormat.defaultFormatter(kv._2) + " }"}).mkString(",\n") +
                    " ] }"
            scalatron.connection.PUT_json_nothing(resourceUrl, attributesJson)
        } catch {
            case e: HttpFailureCodeException => // TODO
                e.httpCode match {
                    case HttpStatus.SC_BAD_REQUEST =>
                        // user name contained invalid characters
                        throw ScalatronException.IllegalUserName(e.reason)
                    case HttpStatus.SC_UNAUTHORIZED =>
                        // not logged on as this user or as Administrator
                        throw ScalatronException.NotAuthorized(e.reason)
                    case HttpStatus.SC_NOT_FOUND =>
                        // this user does not exist on the server
                        throw ScalatronException.NotFound(e.reason)
                    case HttpStatus.SC_INTERNAL_SERVER_ERROR =>
                        // the user's attribute file could not be read or was invalid
                        throw ScalatronException.InternalServerError(e.reason)
                    case _ =>
                        throw e // rethrow
                }
        }
    }

    def getAttributeMap: Map[String, String] = {
        try {
            val jsonOpt = scalatron.connection.GET_json(resourceUrl)
            val jsonMap = jsonOpt.asMap
            val attributes = jsonMap.asKVStrings("attributes", "name", "value")
            // we ignore the resource list returned by this request
            attributes
        } catch {
            case e: HttpFailureCodeException =>
                e.httpCode match {
                    case HttpStatus.SC_BAD_REQUEST =>
                        // user name contained invalid characters
                        throw ScalatronException.IllegalUserName(e.reason)
                    case HttpStatus.SC_UNAUTHORIZED =>
                        // not logged on as this user or as Administrator
                        throw ScalatronException.NotAuthorized(e.reason)
                    case HttpStatus.SC_NOT_FOUND =>
                        // this user does not exist on the server
                        throw ScalatronException.NotFound(e.reason)
                    case HttpStatus.SC_INTERNAL_SERVER_ERROR =>
                        // the user's attribute file could not be read or was invalid
                        throw ScalatronException.InternalServerError(e.reason)
                    case _ =>
                        throw e // rethrow
                }
        }
    }


    //----------------------------------------------------------------------------------------------
    // source code & build management
    //----------------------------------------------------------------------------------------------

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
            val sourcesResource = resource("Sources")
            val jsonOpt = scalatron.connection.GET_json(sourcesResource)
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

    def updateSourceFiles(sourceFileCollection: SourceFileCollection): Unit = {
        try {
            val sourcesResource = resource("Sources")
            val sourceFilesJson =
                "{ \"files\" : [\n" +
                    sourceFileCollection.map(sf => {
                        val codeJson = JSONFormat.defaultFormatter(sf.code)
                        "{ \"filename\" : \"" + sf.filename + "\", \"code\" : " + codeJson + " }"
                    }).mkString(",\n") +
                    "]}"
            scalatron.connection.PUT_json_nothing(sourcesResource, sourceFilesJson)
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
                        // the user's source files could not be written
                        throw ScalatronException.InternalServerError(e.reason)
                    case _ =>
                        throw e // rethrow
                }
        }
    }

    def buildSources(): BuildResult = {
        try {
            val buildResource = resource("Build")
            val jsonOpt = scalatron.connection.PUT_nothing_json(buildResource)
            val jsonMap = jsonOpt.asMap
            val successful = jsonMap.asBoolean("successful")
            val duration = jsonMap.asInt("duration")
            val errorCount = jsonMap.asInt("errors")
            val warningCount = jsonMap.asInt("warnings")
            val messages = jsonMap.asList[Map[String, Any]]("messages")
            val buildMessages = messages.map(m => {
                val jsonMap = JSonMap(m, jsonOpt)
                BuildMessage(
                    jsonMap.asString("filename"),
                    (jsonMap.asInt("line"), jsonMap.asInt("column")),
                    jsonMap.asString("message"),
                    jsonMap.asInt("severity")
                )
            })
            BuildResult(successful, duration, errorCount, warningCount, buildMessages)
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
                        // the compile service could not be started, or disk error, etc.
                        throw ScalatronException.InternalServerError(e.reason)
                    case _ =>
                        throw e // rethrow
                }
        }
    }


    //----------------------------------------------------------------------------------------------
    // version control & sample bots
    //----------------------------------------------------------------------------------------------

    def versions: Iterable[Version] = {
        try {
            /*
            {
                "versions" :
                [
                    { "id" : "1499f03b85b29eac6bde835eed92f68336fb901b", "label" : "Label 1", "date" : "2012-04-13 08:22", "url" : "/api/users/{user}/versions/1499f03b85b29eac6bde835eed92f68336fb901b" },
                    { "id" : "56e43c370bfaaa9773f7cf1fb41b6cec494a3c43", "label" : "Label 2", "date" : "2012-04-13 08:23", "url" : "/api/users/{user}/versions/56e43c370bfaaa9773f7cf1fb41b6cec494a3c43" },
                    { "id" : "a1ae813f274b4a33bc61535e0e0de5345bb08d42", "label" : "Label 3", "date" : "2012-04-13 08:24", "url" : "/api/users/{user}/versions/a1ae813f274b4a33bc61535e0e0de5345bb08d42" }
                ]
            }
            */
            val versionsResource = resource("Versions")
            val jsonOpt = scalatron.connection.GET_json(versionsResource)
            val jsonMap = jsonOpt.asMap
            val versionList = jsonMap.asList[Map[String, Any]]("versions")
            versionList.map(m => {
                val jsonMap = JSonMap(m, jsonOpt)
                ScalatronVersion(
                    jsonMap.asString("id"),
                    jsonMap.asString("label"),
                    jsonMap.asString("date").toLong,
                    jsonMap.asString("url"),
                    this
                )
            })
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
                        // the user's version list could not be read from disk
                        throw ScalatronException.InternalServerError(e.reason)
                    case _ =>
                        throw e // rethrow
                }
        }
    }

    def version(id: String): Option[Version] = versions.find(_.id == id)

    def createVersion(label: String, sourceFileCollection: SourceFileCollection): ScalatronVersion = {
        try {
            /*
            {
                "label" : "Label 1",
                "files" :
                [
                    { "filename" : "Bot.scala", "code" : "class ControlFunctionFactory { ... }" },
                    { "filename" : "Util.scala", "code" : "class View { ... }" }
                ]
            }
            */
            val versionsResource = resource("Versions")
            val versionJSon =
                "{ " +
                    "\"label\" : \"" + label + "\", " +
                    "\"files\" : [\n" +
                    sourceFileCollection.map(sf => {
                        val codeJson = JSONFormat.defaultFormatter(sf.code)
                        "{ \"filename\" : \"" + sf.filename + "\", \"code\" : " + codeJson + " }"
                    }).mkString(",\n") +
                    "]}"
            val jsonOpt = scalatron.connection.POST_json_json(versionsResource, versionJSon)

            /*
                { "id" : "1", "label" : "Label 1", "date" : "2012-04-13 08:22", "url" : "/api/users/{user}/versions/1" },
             */
            val jsonMap = jsonOpt.asMap
            ScalatronVersion(
                jsonMap.asString("id"),
                jsonMap.asString("label"),
                jsonMap.asString("date").toLong,
                jsonMap.asString("url"),
                this
            )
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
                        // the user's version list could not be read from disk
                        throw ScalatronException.InternalServerError(e.reason)
                    case _ =>
                        throw e // rethrow
                }
        }
    }


    //----------------------------------------------------------------------------------------------
    // tournament management
    //----------------------------------------------------------------------------------------------

    def publish(): Unit = {
        try {
            val publishResource = resource("Publish")
            scalatron.connection.PUT_nothing_nothing(publishResource)
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
                        // the user's bot .jar file not be read/written on the server
                        throw ScalatronException.InternalServerError(e.reason)
                    case _ =>
                        throw e // rethrow
                }
        }
    }


    //----------------------------------------------------------------------------------------------
    // sandbox management
    //----------------------------------------------------------------------------------------------

    def createSandbox(argMap: Map[String, String] = Map.empty): ScalatronSandbox = {
        try {
            /*
            {
                "config" :
                [
                    { "name" : "-perimeter", "value" : "open" },
                    { "name" : "-walls",     "value" : "30" },
                    { "name" : "-snorgs",    "value" : "200" },
                ]
            }
            */
            val sandboxResource = resource("Sandboxes")
            val sandboxConfigJSon =
                "{ " +
                    "\"config\" : {\n" +
                    argMap.map(arg => {
                        val valueJson = JSONFormat.defaultFormatter(arg._2)
                        " \"" + arg._1 + "\" : " + valueJson
                    }).mkString(",\n") +
                    "} }"
            val jsonOpt = scalatron.connection.POST_json_json(sandboxResource, sandboxConfigJSon)
            val stateData = ScalatronSandboxState.StateData.fromJson(jsonOpt)
            ScalatronSandbox(stateData.id, stateData, this)
        } catch {
            case e: HttpFailureCodeException =>
                e.httpCode match {
                    case HttpStatus.SC_UNAUTHORIZED =>
                        // not logged on as this user or as Administrator
                        throw ScalatronException.NotAuthorized(e.reason)
                    case HttpStatus.SC_NOT_FOUND =>
                        // this user does not exist on the server
                        throw ScalatronException.NotFound(e.reason)
                    case _ =>
                        throw e // rethrow
                }
        }
    }
}