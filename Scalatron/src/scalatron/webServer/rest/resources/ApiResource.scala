package scalatron.webServer.rest.resources

import javax.ws.rs.core.MediaType
import javax.ws.rs.{GET, Produces, Consumes, Path}
import scalatron.Version
import ApiResource.JSON


/*
## Entry Point

### Get Available Resources

Returns urls pointing to the primary resources exposed by the web API, such as `users`,
`samples` and `tournament`.

* URL:              /api
* Method:           GET
* Returns:          200 OK & JSON
* Authentication:   no need to be logged in

Response JSON example:

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

@Produces(Array(MediaType.APPLICATION_JSON))
@Consumes(Array(MediaType.APPLICATION_JSON))
@Path("/")
class ApiResource extends ResourceWithUser {
    @GET
    def urls =
        JSON.Api(
            Array(
                JSON.ResourceLink("Users", "/api/users"),
                JSON.ResourceLink("Samples", "/api/samples"),
                JSON.ResourceLink("Tournament", "/api/tournament")
            )
        )
}


object ApiResource {
    object JSON {
        case class Api(r: Array[ResourceLink]) {
            def getVersion = Version.VersionString
            def getResources = r
        }
        case class ResourceLink(n: String, u: String) {
            def getName = n
            def getUrl = u
        }
    }
}


