package scalatron.webServer.rest.resources

import java.util.concurrent.TimeoutException
import scalatron.core.Scalatron
import javax.ws.rs._
import core.{Response, MediaType}
import org.eclipse.jetty.http.HttpStatus
import java.io.IOError

@Produces(Array(MediaType.APPLICATION_JSON))
@Consumes(Array(MediaType.APPLICATION_JSON))
@Path("users/{user}/sources/build")
class SourcesBuildResource extends ResourceWithUser {
    @PUT
    def buildSourceFiles() = {
        if(!userSession.isLoggedOnAsUserOrAdministrator(userName)) {
            Response.status(CustomStatusType(HttpStatus.UNAUTHORIZED_401, "must be logged on as '" + userName + "' or '" + Scalatron.Constants.AdminUserName + "'")).build()
        } else {
            try {
                scalatron.user(userName) match {
                    case Some(user) =>
                        val buildResult = user.buildSources()

                        val messages =
                            buildResult.messages
                            .map(e => new SourcesBuildResource.BuildMessage(e.sourceFile, e.lineAndColumn._1, e.lineAndColumn._2, e.multiLineMessage, e.severity))
                            .toArray

                        SourcesBuildResource.BuildResult(
                            buildResult.successful,
                            buildResult.duration,
                            buildResult.errorCount,
                            buildResult.warningCount,
                            messages)

                    case None =>
                        Response.status(CustomStatusType(HttpStatus.NOT_FOUND_404, "user '" + userName + "' does not exist")).build()
                }
            } catch {
                case e: IllegalStateException =>
                    // if compilation service is unavailable, sources don't exist etc.
                    Response.status(CustomStatusType(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage)).build()

                case e: IOError =>
                    // if source files cannot be read from disk, etc.
                    Response.status(CustomStatusType(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage)).build()
            }
        }
    }
}

object SourcesBuildResource {
    case class BuildResult(successful: Boolean, duration: Int, errorCount: Int, warningCount: Int, mesgs: Array[BuildMessage]) {
        def getSuccessful = successful
        def getDuration = duration
        def getErrors = errorCount
        def getWarnings = warningCount
        def getMessages = mesgs
    }

    class BuildMessage(f: String, l: Int, c: Int, m: String, s: Int) {
        def getFilename = f
        def getLine = l
        def getColumn = c
        def getMessage = m
        def getSeverity = s
    }
}