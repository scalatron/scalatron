package scalatron.webServer.rest.resources

import scalatron.core.Scalatron
import javax.ws.rs._
import core.{Response, MediaType}
import org.eclipse.jetty.http.HttpStatus
import java.io.IOError
import Scalatron.VersionPolicy


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
    def updateSourceFiles(sourceFileUpdate: SourcesResource.SourceFileUpdate) = {
        if(!userSession.isLoggedOnAsUserOrAdministrator(userName)) {
            Response.status(CustomStatusType(HttpStatus.UNAUTHORIZED_401, "must be logged on as '" + userName + "' or '" + Scalatron.Constants.AdminUserName + "'")).build()
        } else {
            try {
                scalatron.user(userName) match {
                    case Some(user) =>
                        // before overwriting the existing source files, create an optional backup version
                        val versionLabel = if(sourceFileUpdate.versionLabel == null) "" else sourceFileUpdate.versionLabel
                        user.createVersion(versionLabel)

                        val updatedSourceFiles = sourceFileUpdate.getFiles.map(sf => Scalatron.SourceFile(sf.getFilename, sf.getCode))
                        user.updateSourceFiles(updatedSourceFiles)

                        // CBB: return information about the optionally created version to the caller as JSON (see 'create version' result)

                        Response.noContent().build()
                    case None =>
                        Response.status(CustomStatusType(HttpStatus.NOT_FOUND_404, "user '" + userName + "' does not exist")).build()
                }
            } catch {
                case e: IllegalStateException =>
                    // e.g. version creation failed
                    Response.status(CustomStatusType(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage)).build()

                case e: IOError =>
                    // source files could not be written
                    Response.status(CustomStatusType(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage)).build()
            }
        }
    }
}

object SourcesResource {
    object Constants {
        val IfDifferent = "ifDifferent"
        val Always = "always"
        val Never = "never"
    }

    def versionPolicyFrom(s: String) =
        if(s == null || s.compareToIgnoreCase(SourcesResource.Constants.IfDifferent)==0 ) {
            VersionPolicy.IfDifferent
        } else
        if(s.compareToIgnoreCase(SourcesResource.Constants.Always)==0 ) {
            VersionPolicy.Always
        } else
        if(s.compareToIgnoreCase(SourcesResource.Constants.Never)==0 ) {
            VersionPolicy.Never
        } else {
            throw new IllegalArgumentException("invalid version policy: '%s'".format(s) )
        }


    /** Used for server -> client transfers. */
    case class SourceFiles(var fileList: Array[SourceFile]) {
        def this() = this(null)
        def getFiles = fileList
        def setFiles(fl: Array[SourceFile]) { this.fileList = fl }
    }

    /** Used for client -> server transfers, includes optional versioning policy and label. */
    case class SourceFileUpdate(var fileList: Array[SourceFile], var versionPolicy: String, var versionLabel: String) {
        def this() = this(null,null,null)
        def getFiles = fileList
        def setFiles(fl: Array[SourceFile]) { this.fileList = fl }
        
        def getVersionPolicy = versionPolicy
        def setVersionPolicy(versionPolicy: String) { this.versionPolicy = versionPolicy }
        
        def getVersionLabel = versionLabel
        def setVersionLabel(versionLabel: String) { this.versionLabel = versionLabel }
    }

    case class SourceFile(var n: String, var c: String) {
        def this() = this(null, null)
        def getFilename = n
        def getCode = c
        def setFilename(name: String) { n = name }
        def setCode(co: String) { c = co }
    }
}

