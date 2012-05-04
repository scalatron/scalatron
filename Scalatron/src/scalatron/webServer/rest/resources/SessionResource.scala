package scalatron.webServer.rest.resources

import javax.ws.rs._
import core.{NewCookie, Response, MediaType}
import SessionResource.resourceList
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
    def logoff(@PathParam("user") userName: String) {
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
                SessionResource.ResourceLink("Session", "/api/users/%s/session".format(userName)),
                SessionResource.ResourceLink("Sources", "/api/users/%s/sources".format(userName)),
                SessionResource.ResourceLink("Build", "/api/users/%s/sources/build".format(userName)),
                SessionResource.ResourceLink("Sandboxes", "/api/users/%s/sandboxes".format(userName)),
                SessionResource.ResourceLink("Publish", "/api/users/%s/unpublished/publish".format(userName)),
                SessionResource.ResourceLink("Published", "/api/users/%s/published".format(userName)),
                SessionResource.ResourceLink("Unpublished", "/api/users/%s/unpublished".format(userName)),
                SessionResource.ResourceLink("Versions", "/api/users/%s/versions".format(userName))
            )

    def resourceList(userName: String) =
        SessionResource.ResourceList(resources(userName))

    case class Password(var p: String) {
        def this() = this(null)
        def getPassword = p
        def setPassword(p: String) {this.p = p}
    }

    case class ResourceList(r: Array[ResourceLink]) {
        def getResources = r
    }

    case class ResourceLink(n: String, u: String) {
        def getName = n
        def getUrl = u
    }
}




