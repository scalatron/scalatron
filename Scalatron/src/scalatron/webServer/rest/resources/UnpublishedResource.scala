package scalatron.webServer.rest.resources

import javax.ws.rs.{PUT, Path, Consumes, Produces}
import collection.mutable.ArrayBuffer
import java.util.concurrent.TimeoutException
import javax.ws.rs.core.{Response, MediaType}
import org.eclipse.jetty.http.HttpStatus
import java.io.IOError
import scalatron.core.Scalatron


@Produces(Array(MediaType.APPLICATION_JSON))
@Consumes(Array(MediaType.APPLICATION_JSON))
@Path("users/{user}/unpublished")
class UnpublishedResource extends ResourceWithUser {


/*
    @PUT
    def build() = {
        if(!userSession.isLoggedOnAsUserOrAdministrator(userName)) {
            Response.status(CustomStatusType(HttpStatus.UNAUTHORIZED_401, "must be logged on as '" + userName + "' or '" + Scalatron.Constants.AdminUserName + "'")).build()
        } else {
            try {
                scalatron.user(userName) match {
                    case Some(user) =>
                        val res = user.buildSources()

                        val buildResult = res.messages.map(e => {
                            var line: Int = 0
                            try {
                                line = e.lineAndColumn._1
                            } catch {
                                case e: Exception => // Nothing to do
                            }

                            buf += new UnpublishedResource.BuildMessage(line, e.multiLineMessage, e.severity)
                        }
                        )

                        val buf = new ArrayBuffer[UnpublishedResource.BuildMessage]();
                        // Enumeration of messages doesn't work!
                        val messages = res.messages
                        messages.foreach(e => {
                            var line: Int = 0
                            try {
                                line = e.lineAndColumn._1
                            } catch {
                                case e: Exception => // Nothing to do
                            }

                            buf += new UnpublishedResource.BuildMessage(line, e.multiLineMessage, e.severity)
                        });

                        Response
                            .created(uriInfo.getAbsolutePath)
                            .entity(new UnpublishedResource.BuildResult(res.successful, res.errorCount, res.warningCount, buf.toArray))
                            .build()

                    case None =>
                        Response.status(CustomStatusType(HttpStatus.NOT_FOUND_404, "user '" + userName + "' does not exist")).build()
                }
            } catch {
                case e: TimeoutException =>
                    new UnpublishedResource.BuildResult(
                        false, 1, 0,
                        Array(new UnpublishedResource.BuildMessage(0, "Compilation has timed out. Please try again.", 0))
                    )
                case e: IOError =>
                    // source files could not be written
                    Response.status(CustomStatusType(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage)).build()

                case t: Throwable =>
                    System.err.println("error while building: " + t.toString)
                    throw t
            }
        }
    }
*/
}

object UnpublishedResource {
    case class BuildResultDto(success: Boolean, errorCount: Int, warningCount: Int, mesgs: Array[MessageDto]) {
        def getSuccess = success
        def getErrorCount = errorCount
        def getWarningCount = warningCount
        def getMessages = mesgs
    }


    class MessageDto(l: Int, m: String, s: Int) {
        def getLine = l
        def getMessage = m
        def getSeverity = s
    }
}

