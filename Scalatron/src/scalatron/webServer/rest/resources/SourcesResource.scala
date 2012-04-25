package scalatron.webServer.rest.resources

import scalatron.scalatron.api.Scalatron
import javax.ws.rs._
import core.{Response, MediaType}
import org.eclipse.jetty.http.HttpStatus
import java.io.IOError


@Produces(Array(MediaType.APPLICATION_JSON))
@Consumes(Array(MediaType.APPLICATION_JSON))
@Path("users/{user}/sources")
class SourcesResource extends ResourceWithUser {

    @GET
    def getSourceFiles = {
        if(!userSession.isLoggedOnAsUserOrAdministrator(userName)) {
            Response.status(CustomStatusType(HttpStatus.UNAUTHORIZED_401, "must be logged on as '" + userName + "' or '" + Scalatron.Constants.AdminUserName + "'")).build()
        } else {
            try {
                scalatron.user(userName) match {
                    case Some(user) =>
                        val s = user.sourceFiles
                        val sourceFiles = s.map(sf => SourcesResource.SourceFile(sf.filename, sf.code)).toArray
                        SourcesResource.SourceFiles(sourceFiles)
                    case None =>
                        Response.status(CustomStatusType(HttpStatus.NOT_FOUND_404, "user '" + userName + "' does not exist")).build()
                }
            } catch {
                case e: IllegalStateException =>
                    // source directory does not exist
                    Response.status(CustomStatusType(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage)).build()

                case e: IOError =>
                    // source files could not be read
                    Response.status(CustomStatusType(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage)).build()
            }
        }
    }


    @PUT
    def updateSourceFiles(sources: SourcesResource.SourceFiles) = {
        if(!userSession.isLoggedOnAsUserOrAdministrator(userName)) {
            Response.status(CustomStatusType(HttpStatus.UNAUTHORIZED_401, "must be logged on as '" + userName + "' or '" + Scalatron.Constants.AdminUserName + "'")).build()
        } else {
            try {
                scalatron.user(userName) match {
                    case Some(user) =>
                        val sourceFiles = sources.getFiles.map(sf => Scalatron.SourceFile(sf.getFilename, sf.getCode))
                        user.updateSourceFiles(sourceFiles)
                        Response.noContent().build()
                    case None =>
                        Response.status(CustomStatusType(HttpStatus.NOT_FOUND_404, "user '" + userName + "' does not exist")).build()
                }
            } catch {
                case e: IOError =>
                    // source files could not be written
                    Response.status(CustomStatusType(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage)).build()
            }
        }
    }
}

object SourcesResource {
    case class SourceFiles(var fileList: Array[SourceFile]) {
        def this() = this(null)
        def getFiles = fileList
        def setFiles(fl: Array[SourceFile]) { this.fileList = fl }
    }

    case class SourceFile(var n: String, var c: String) {
        def this() = this(null, null)
        def getFilename = n
        def getCode = c
        def setFilename(name: String) { n = name }
        def setCode(co: String) { c = co }
    }
}

