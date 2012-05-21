package scalatron.webServer.rest.resources

import scalatron.core.Scalatron
import javax.ws.rs._
import core.{Response, MediaType}
import org.eclipse.jetty.http.HttpStatus
import java.io.IOError
import java.text.DateFormat
import java.util.Date


@Produces(Array(MediaType.APPLICATION_JSON))
@Consumes(Array(MediaType.APPLICATION_JSON))
@Path("users/{user}/versions")
class VersionsResource extends ResourceWithUser {
    @GET
    def getVersions = {
        if(!userSession.isLoggedOnAsUserOrAdministrator(userName)) {
            Response.status(CustomStatusType(HttpStatus.UNAUTHORIZED_401, "must be logged on as '" + userName + "' or '" + Scalatron.Constants.AdminUserName + "'")).build()
        } else {
            try {
                scalatron.user(userName) match {
                    case Some(user) =>
                        val v = user.versions
                        val versionList = v.map(e => {
                            val url = "/api/users/%s/versions/%s".format(userName, e.id)
                            val dateString = e.date.toString // milliseconds since the epoch, as string
                            VersionsResource.Version(e.id, e.label, dateString, url)
                        }).toArray
                        VersionsResource.VersionList(versionList)
                    case None =>
                        Response.status(CustomStatusType(HttpStatus.NOT_FOUND_404, "user '" + userName + "' does not exist")).build()
                }
            } catch {
                case e: IOError =>
                    // source files could not be read
                    Response.status(CustomStatusType(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage)).build()
            }
        }
    }


    @POST
    def createVersion(sources: VersionsResource.CreateVersion) = {
        if(!userSession.isLoggedOnAsUserOrAdministrator(userName)) {
            Response.status(CustomStatusType(HttpStatus.UNAUTHORIZED_401, "must be logged on as '" + userName + "' or '" + Scalatron.Constants.AdminUserName + "'")).build()
        } else {
            try {
                scalatron.user(userName) match {
                    case Some(user) =>
                        // fetch the source files from the message
                        val label = sources.getLabel
                        val sourceFiles = sources.getFiles.map(sf => Scalatron.SourceFile(sf.getFilename, sf.getCode))

                        // 1 - as a precaution, back up the user's current workspace source files
                        user.createVersion("Secondary backup before '%s'" format label)

                        // 2 - write the given source files to disk
                        user.updateSourceFiles(sourceFiles)

                        // 3 - create a version from the updated source files
                        val versionOpt = user.createVersion(label)

                        // 4 - legacy: handle the case where no version was generated because nothing changed
                        val version = versionOpt.getOrElse(user.latestVersion.get)

                        // send information about the new version to the user
                        val url = "/api/users/%s/versions/%s".format(userName, version.id)
                        val dateString = version.date.toString // milliseconds since the epoch, as string
                        VersionsResource.Version(version.id, version.label, dateString, url)
                    case None =>
                        Response.status(CustomStatusType(HttpStatus.NOT_FOUND_404, "user '" + userName + "' does not exist")).build()
                }
            } catch {
                case e: IllegalStateException =>
                    // version (base) directory could not be created
                    Response.status(CustomStatusType(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage)).build()

                case e: IOError =>
                    // source files could not be written
                    Response.status(CustomStatusType(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage)).build()
            }
        }
    }


    @GET
    @Path("{id}")
    def restoreVersion(@PathParam("id") id: String) = {
        if(!userSession.isLoggedOnAsUserOrAdministrator(userName)) {
            Response.status(CustomStatusType(HttpStatus.UNAUTHORIZED_401, "must be logged on as '" + userName + "' or '" + Scalatron.Constants.AdminUserName + "'")).build()
        } else {
            try {
                scalatron.user(userName) match {
                    case Some(user) =>
                        user.version(id) match {
                            case None =>
                                Response.status(CustomStatusType(HttpStatus.NOT_FOUND_404, "version %s of user %s does not exist".format(id,userName))).build()
                            case Some(version) =>
                                // check source files out from version control into workspace, overwriting what was there
                                version.restore()

                                // then send the restored source files to the client
                                val sourceFiles = user.sourceFiles.map(sf => SourcesResource.SourceFile(sf.filename, sf.code)).toArray
                                SourcesResource.SourceFiles(sourceFiles)
                        }
                    case None =>
                        Response.status(CustomStatusType(HttpStatus.NOT_FOUND_404, "user '" + userName + "' does not exist")).build()
                }
            } catch {
                case e: IOError =>
                    // source files could not be read
                    Response.status(CustomStatusType(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage)).build()
            }
        }
    }
}


object VersionsResource {
    case class VersionList(versionlist: Array[Version]) {
        def getVersions = versionlist
    }

    case class Version(id: String, label: String, date: String, url: String) {
        def getId = id
        def getLabel = label
        def getDate = date
        def getUrl = url
    }



    case class CreateVersion(var label: String, var fileList: Array[SourceFile]) {
        def this() = this(null, null)
        def getLabel = label
        def setLabel(l: String) { this.label = l }
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


