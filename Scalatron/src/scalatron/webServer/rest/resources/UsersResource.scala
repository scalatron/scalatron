package scalatron.webServer.rest.resources

import javax.ws.rs._
import core.{Response, MediaType}
import scalatron.core.Scalatron
import scalatron.core.Scalatron.ScalatronException
import org.eclipse.jetty.http.HttpStatus
import java.io.IOError
import scalatron.scalatron.impl.SourceFileCollection


@Produces(Array(MediaType.APPLICATION_JSON))
@Consumes(Array(MediaType.APPLICATION_JSON))
@Path("users")
class UsersResource extends Resource {
    // cannot extends from BaseResource because some methods do not have a userName injection.

    @GET
    def listUsers = {
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
        UsersResource.UserList(
            scalatron.users().map(u =>
                UsersResource.User(
                    u.name,
                    "/api/users/%s/session".format(u.name),
                    "/api/users/%s".format(u.name)
                )
            ).toArray
        )
    }

    @POST
    def createUser(param: UsersResource.UserPassword) = {
        if(!userSession.isLoggedOnAsAdministrator) {
            Response.status(CustomStatusType(HttpStatus.UNAUTHORIZED_401, "must be logged on as '" + Scalatron.Constants.AdminUserName + "'")).build()
        } else {
            try {
                val user = scalatron.createUser(param.getName, param.getPassword, SourceFileCollection.initial(param.getName))

                /*
               {
                   "name" : "{user}",
                   "session" : "/api/users/{user}/session",
                   "resource" : "/api/users/{user}"
               }
                */
                UsersResource.User(
                    param.getName,
                    "/api/users/%s/session".format(user.name),
                    "/api/users/%s".format(user.name)
                )
            } catch {
                case e: ScalatronException.IllegalUserName =>
                    Response.status(CustomStatusType(HttpStatus.BAD_REQUEST_400, e.getMessage)).build()
                case e: ScalatronException.Exists =>
                    Response.status(CustomStatusType(HttpStatus.FORBIDDEN_403, e.getMessage)).build()
                case e: ScalatronException.CreationFailed =>
                    Response.status(CustomStatusType(HttpStatus.INTERNAL_SERVER_ERROR_500,e.getMessage)).build()
                case e: IOError =>
                    Response.status(CustomStatusType(HttpStatus.INTERNAL_SERVER_ERROR_500,e.getMessage)).build()
            }
        }
    }


    @GET
    @Path("{user}")
    def getUserAttributesAndResources(@PathParam("user") userName: String) = {
        if(!userSession.isLoggedOnAsUserOrAdministrator(userName)) {
            Response.status(CustomStatusType(HttpStatus.UNAUTHORIZED_401, "must be logged on as '" + userName + "' or '" + Scalatron.Constants.AdminUserName + "'")).build()
        } else {
            try {
                scalatron.user(userName) match {
                    case Some(user) =>
                        user.getAttributeMapOpt match {
                            case None =>
                                // we map None to INTERNAL_SERVER_ERROR_500
                                Response.status(CustomStatusType(HttpStatus.INTERNAL_SERVER_ERROR_500, "configuration attributes of user '" + userName + "' are invalid")).build()

                            case Some(map) =>
                                val attributes =
                                    map
                                    .filter(entry => entry._1 != Scalatron.Constants.PasswordKey)   // don't serve the password
                                    .map(entry => new UsersResource.Attribute(entry._1, entry._2)).toArray

                                UsersResource.AttributesAndResources(
                                    attributes,
                                    SessionResource.resources(userName)
                                )
                        }

                    case None =>
                        Response.status(CustomStatusType(HttpStatus.NOT_FOUND_404, "user '" + userName + "' does not exist")).build()
                }
            } catch {
                case e: ScalatronException.IllegalUserName =>
                    Response.status(CustomStatusType(HttpStatus.BAD_REQUEST_400, e.getMessage)).build()
            }
        }
    }

    @PUT
    @Path("{user}")
    def updateUserAttributes(attributeList: UsersResource.Attributes, @PathParam("user") userName: String) = {
        if(!userSession.isLoggedOnAsUserOrAdministrator(userName)) {
            Response.status(CustomStatusType(HttpStatus.UNAUTHORIZED_401, "must be logged on as '" + userName + "' or '" + Scalatron.Constants.AdminUserName + "'")).build()
        } else {
            try {
                scalatron.user(userName) match {
                    case Some(user) =>
                        user.updateAttributes(attributeList.getAttributes.map(a => (a.getName, a.getValue)).toMap)
                        Response.noContent().build()

                    case None =>
                        Response.status(CustomStatusType(HttpStatus.NOT_FOUND_404, "user '" + userName + "' does not exist")).build()
                }
            } catch {
                case e: ScalatronException.IllegalUserName =>
                    Response.status(CustomStatusType(HttpStatus.BAD_REQUEST_400, e.getMessage)).build()
            }
        }
    }

    @DELETE
    @Path("{user}")
    def deleteUser(@PathParam("user") userName: String) = {
        if(!userSession.isLoggedOnAsUserOrAdministrator(userName)) {
            Response.status(CustomStatusType(HttpStatus.UNAUTHORIZED_401, "must be logged on as '" + userName + "' or '" + Scalatron.Constants.AdminUserName + "'")).build()
        } else {
            try {
                scalatron.user(userName) match {
                    case Some(user) =>
                        user.delete()
                        Response.noContent().build()

                    case None =>
                        Response.status(CustomStatusType(HttpStatus.NOT_FOUND_404, "user '" + userName + "' does not exist")).build()
                }
            } catch {
                case e: ScalatronException.Forbidden =>
                    Response.status(CustomStatusType(HttpStatus.FORBIDDEN_403, e.getMessage)).build()
                case e: ScalatronException.IllegalUserName =>
                    Response.status(CustomStatusType(HttpStatus.BAD_REQUEST_400, e.getMessage)).build()
                case e: IOError =>
                    Response.status(CustomStatusType(HttpStatus.INTERNAL_SERVER_ERROR_500,e.getMessage)).build()
            }
        }
    }
}


object UsersResource {
    class Attributes(var attributeList: Array[Attribute]) {
        def this() = this(null)
        def getAttributes = attributeList
        def setAttributes(al: Array[Attribute]) { this.attributeList = al }
    }

    class Attribute(var k: String, var v: String) {
        def this() = this(null, null)
        def getName = k
        def setName(k: String) { this.k = k }
        def getValue = v
        def setValue(v: String) { this.v = v }
    }

    case class AttributesAndResources(a: Array[Attribute], r: Array[SessionResource.ResourceLink]) {
        def getAttributes = a
        def getResources = r
    }

    class Password(var p: String) {
        def this() = this(null)
        def getPassword = p
        def setPassword(p: String) {this.p = p}
    }

    class UserPassword(var n: String, var p: String) {
        def this() = this(null, null)
        def getPassword = p
        def setPassword(p: String) {this.p = p}
        def getName = n
        def setName(n: String) {this.n = n}
    }

    case class UserList(list: Array[User]) {
        def getUsers = list
    }

    case class User(n: String, s: String, r: String) {
        def getName = n
        def getSession = s
        def getResource = r
    }
}
