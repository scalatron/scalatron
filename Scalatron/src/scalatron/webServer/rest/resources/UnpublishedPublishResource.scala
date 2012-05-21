package scalatron.webServer.rest.resources

import javax.ws.rs.{PUT, Path, Consumes, Produces}
import javax.ws.rs.core.{Response, MediaType}
import scalatron.core.Scalatron
import org.eclipse.jetty.http.HttpStatus
import java.io.IOError


@Produces(Array(MediaType.APPLICATION_JSON))
@Consumes(Array(MediaType.APPLICATION_JSON))
@Path("users/{user}/unpublished/publish")
class UnpublishedPublishResource extends ResourceWithUser
{
    @PUT
    def publish = {
        if(!userSession.isLoggedOnAsUserOrAdministrator(userName)) {
            Response.status(CustomStatusType(HttpStatus.UNAUTHORIZED_401, "must be logged on as '" + userName + "' or '" + Scalatron.Constants.AdminUserName + "'")).build()
        } else {
            try {
                scalatron.user(userName) match {
                    case Some(user) =>
                        user.publish()
                        Response.noContent().build();

                    case None =>
                        Response.status(CustomStatusType(HttpStatus.NOT_FOUND_404, "user '" + userName + "' does not exist")).build()
                }
            } catch {
                case e: IllegalStateException =>
                    // if the old plug-in file could not be backed up or the backup deleted
                    Response.status(CustomStatusType(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage)).build()

                case e: IOError =>
                    // if the unpublished plug-in file could not be copied
                    Response.status(CustomStatusType(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage)).build()
            }
        }
    }
}



