/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatronRemote.impl

import org.apache.http.impl.client.DefaultHttpClient
import scalatronRemote.api.ScalatronRemote
import scalatronRemote.impl.Connection.HttpFailureCodeException
import org.apache.http.HttpStatus
import scalatronRemote.api.ScalatronRemote.{ScalatronException, ConnectionConfig, User}


object ScalatronRemoteImpl {
    def apply(connectionConfig : ConnectionConfig) : ScalatronRemoteImpl = {
        val httpClient = new DefaultHttpClient()

        val verbose = connectionConfig.verbose
        val hostname = connectionConfig.hostname
        val port = connectionConfig.port
        val apiEntryPoint = connectionConfig.apiEntryPoint

        val connection = Connection(httpClient, hostname, port, verbose)
        val (version, userResource) = GET_api(connection, apiEntryPoint, verbose)

        if(verbose) println("Connected to Scalatron server running " + version + " at http://" + hostname + ":" + port + apiEntryPoint)

        ScalatronRemoteImpl(connection, version, userResource, verbose)
    }


    /** Performs the initial connection to the server and fetches the /api JSON data.
      * @return (version, userResource)
      * @throws IllegalStateException on unexpected data
      */
    private def GET_api(connection : Connection, apiEntryPoint : String, verbose : Boolean) : (String, String) = {
        /*
        {
            "version" : "0.9.7",
            "resources" :
            [
                { "name" : "Users",         "url" : "/api/users"},
                { "name" : "Samples",       "url" : "/api/samples"},
                { "name" : "Tournament",    "url" : "/api/tournament"}
            ]
        }
         */
        val jsonOpt = connection.GET_json(apiEntryPoint)
        val jsonMap = jsonOpt.asMap

        val version = jsonMap.asString("version")
        val resources = jsonMap.asKVStrings("resources", "name", "url")

        if(verbose) {
            println("Discovered the following resources:")
            resources.foreach(r => println("   " + r._1 + " -> " + r._2))
        }

        val userResource = resources.get("Users") match {
            case Some(url) => url
            case _ => throw new IllegalStateException("invalid server response (no 'Users' resource): " + jsonOpt)
        }

        (version, userResource)
    }
}

case class ScalatronRemoteImpl(
                                  connection : Connection,
                                  version : String,
                                  userResource : String,
                                  verbose : Boolean) extends ScalatronRemote {
    def hostname = connection.hostname
    def port = connection.port

    def users() : ScalatronUserList = {
        /*
        {
            "users" :
            [
                { "name" : "Administrator", "session" : "/api/users/Administrator/session"}, "resource" : "/api/users/Administrator"},
                { "name" : "Frank",         "session" : "/api/users/Frank/session"},         "resource" : "/api/users/Frank"},
                { "name" : "Daniel",        "session" : "/api/users/Daniel/session"},        "resource" : "/api/users/Daniel"},
                { "name" : "{user}",        "session" : "/api/users/{user}/session"}         "resource" : "/api/users/{user}"}
            ]
        }
         */
        val jsonOpt = connection.GET_json(userResource)
        val jsonMap = jsonOpt.asMap // throws on format error
        val users = jsonMap.asKVVStrings("users", "name", "session", "resource")

        if(verbose) {
            println("Discovered the following users:")
            users.foreach(r => println("   " + r._1 + " -> " + r._2._1 + "; " + r._2._2))
        }

        val userList = users.map(u => ScalatronUser(u._1, u._2._1, u._2._2, this))

        ScalatronUserList(userList, this)
    }

    def createUser(name : String, password : String) : User = {
        try {
            val jsonOpt = connection.POST_json_json(userResource, "{ \"name\" : \"" + name + "\", \"password\" : \"" + password + "\"}")
            val jsonMap = jsonOpt.asMap // throws on format error
            val createdUserName = jsonMap.asString("name")
            val createdUserSessionUrl = jsonMap.asString("session")
            val createdUserResourceUrl = jsonMap.asString("resource")

            ScalatronUser(createdUserName, createdUserSessionUrl, createdUserResourceUrl, this)
        } catch {
            case e : HttpFailureCodeException =>
                e.httpCode match {
                    case HttpStatus.SC_UNAUTHORIZED =>
                        // not logged on as Administrator
                        throw ScalatronException.NotAuthorized(e.reason)
                    case HttpStatus.SC_BAD_REQUEST =>
                        // server rejected user name
                        throw ScalatronException.IllegalUserName(e.reason)
                    case HttpStatus.SC_FORBIDDEN =>
                        // user already exists
                        throw ScalatronException.Exists(e.reason)
                    case HttpStatus.SC_INTERNAL_SERVER_ERROR =>
                        // user workspace could not be created
                        throw ScalatronException.CreationFailed(e.reason)
                    case _ =>
                        throw e // rethrow
                }
        }
    }
}