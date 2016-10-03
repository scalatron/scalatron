package scalatron.webServer.rest.resources

import javax.ws.rs._
import javax.ws.rs.core.{NewCookie, Response, MediaType}
import scalatron.webServer.rest.resources.SessionResource.resourceList
import org.eclipse.jetty.http.HttpStatus


@Produces(Array(MediaType.APPLICATION_JSON))
@Consumes(Array(MediaType.APPLICATION_JSON))
@Path("users/{user}/session")
class SessionResource extends ResourceWithUser {
    @POST
    def logon(@PathParam("user") userName: String, password: SessionResource.Password) = {
        scalatron.user(userName) match {
            case Some(user) =>
                user.getPasswordOpt match {
                    case Some(pwd) =>
                        if(pwd == password.getPassword) {
                            userSession.init(userName).build()

                            if(verbose) println("User '" + userName + "' logged on")

                            Response
                            .created(uriInfo.getAbsolutePath)
                            .cookie(new NewCookie("scalatron-user", userName))
                            .entity(resourceList(userName))
                            .build()
                        } else {
                            System.err.println("Refused logon for user '" + userName + "': wrong password")
                            Response.status(CustomStatusType(HttpStatus.UNAUTHORIZED_401, "wrong password for user '" + userName + "'")).build()
                        }
                    case None =>
                        System.err.println("Refused logon for user '" + userName + "': password configuration invalid")
                        Response.status(CustomStatusType(HttpStatus.INTERNAL_SERVER_ERROR_500, "password configuration invalid for user '" + userName + "'")).build()
                }
            case None =>
                System.err.println("Refused logon for user '" + userName + "': unknown account")
                Response.status(CustomStatusType(HttpStatus.NOT_FOUND_404, "user '" + userName + "' does not exist")).build()
        }
    }

    @DELETE
    def logoff(@PathParam("user") userName: String): Unit = {
        if(!userSession.isLoggedOnAsUser(userName)) {
            System.err.println("Refused logout: not logged on as user '" + userName + "'")
            Response.status(CustomStatusType(HttpStatus.UNAUTHORIZED_401, "not logged on as user '" + userName + "'")).build()
        } else {
            userSession.destroy()
            if(verbose) println("User '" + userName + "' logged off")
        }
    }
}

object SessionResource {
    def resources(userName: String) =
            Array(
                SessionResource.ResourceLink("Session", s"/api/users/$userName/session"),
                SessionResource.ResourceLink("Sources", s"/api/users/$userName/sources"),
                SessionResource.ResourceLink("Build", s"/api/users/$userName/sources/build"),
                SessionResource.ResourceLink("Sandboxes", s"/api/users/$userName/sandboxes"),
                SessionResource.ResourceLink("Publish", s"/api/users/$userName/unpublished/publish"),
                SessionResource.ResourceLink("Published", s"/api/users/$userName/published"),
                SessionResource.ResourceLink("Unpublished", s"/api/users/$userName/unpublished"),
                SessionResource.ResourceLink("Versions", s"/api/users/$userName/versions")
            )

    def resourceList(userName: String) =
        SessionResource.ResourceList(resources(userName))

    case class Password(var p: String) {
        def this() = this(null)
        def getPassword = p
        def setPassword(p: String): Unit = {this.p = p}
    }

    case class ResourceList(r: Array[ResourceLink]) {
        def getResources = r
    }

    case class ResourceLink(n: String, u: String) {
        def getName = n
        def getUrl = u
    }
}




