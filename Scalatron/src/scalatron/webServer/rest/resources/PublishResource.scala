package scalatron.webServer.rest.resources

import javax.ws.rs.core.{Response, MediaType}
import javax.ws.rs.{POST, Produces, Consumes, Path}


/*
### Publish Unpublished Bot

Publishes the unpublished bot version residing in the user's workspace into the tournament by
copying the bot plug-in .jar file into the tournament bot directory appropriate for the user.

Note: you may need to update the user's bot beforehand, either by uploading a .jar file built
client-side via POST to /api/users/{user}/unpublished or by building from sources (potentially
also to be uploaded first) via PUT to /api/users/{user}/unpublished

* URL:              /api/users/{user}/published
* Method:           POST
* Request Body:     JSON
* Returns:
    * 201 Created & Location
    * 401 Unauthorized (if not logged on as that user)
    * 415 Unsupported Media Type (malformed request, or other problem)
* Authentication:   must be logged on as that user
 */

@Produces(Array(MediaType.APPLICATION_JSON))
@Consumes(Array(MediaType.APPLICATION_JSON))
@Path("users/{user}/published")
class PublishResource extends ResourceWithUser {

/*
    @POST
    def publish = {
        requireLoggedInAsOwningUserOrAdministrator();
        val user = scalatron.user(userName).get
        user.publish()
        Response.created(uriInfo.getAbsolutePath).build();
    }
*/
}