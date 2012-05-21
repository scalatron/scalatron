package scalatron.webServer.rest.resources

import javax.ws.rs._
import core.{Response, MediaType}
import scalatron.core.Scalatron
import org.eclipse.jetty.http.HttpStatus
import scalatron.core.Scalatron.ScalatronException
import java.io.IOError
import java.net.{URI, URLDecoder, URLEncoder}

@Produces(Array(MediaType.APPLICATION_JSON))
@Consumes(Array(MediaType.APPLICATION_JSON))
@Path("samples")
class SamplesResource extends Resource
{
    @GET
    def listSamples = {
        if(!userSession.isLoggedOnAsAnyone) {
            Response.status(CustomStatusType(HttpStatus.UNAUTHORIZED_401, "must be logged on")).build()
        } else {
                /*
               {
                   "samples" :
                   [
                       { "name" : "Tutorial Bot 01", "url" : "/api/samples/Tutorial%20Bot%2001"},
                       { "name" : "Tutorial Bot 02", "url" : "/api/samples/Tutorial%20Bot%2002"},
                       { "name" : "Tutorial Bot 03", "url" : "/api/samples/Tutorial%20Bot%2003"},
                   ]
               }
                */
            SamplesResource.SampleList(
                scalatron.samples.map(s => {
                    val sampleName = s.name
                    val encodedSampleName = URLEncoder.encode(sampleName, "UTF-8")
                    val sampleURL = "/api/samples/%s" format encodedSampleName
                    SamplesResource.Sample(sampleName, sampleURL)
                }).toArray
            )
        }
    }

    @POST
    def createSample(sources: SamplesResource.CreateSample) = {
        if(!userSession.isLoggedOnAsAnyone) {
            Response.status(CustomStatusType(HttpStatus.UNAUTHORIZED_401, "must be logged on")).build()
        } else {
            try {
                val sampleName = sources.getName
                val sourceFiles = sources.getFiles.map(sf => Scalatron.SourceFile(sf.getFilename, sf.getCode))
                /*val sample =*/ scalatron.createSample(sampleName, sourceFiles)
                val encodedSampleName = URLEncoder.encode(sampleName, "UTF-8")

                Response
                .created(URI.create(uriInfo.getAbsolutePath + "/" + encodedSampleName))
                .build()
            } catch {
                case e: ScalatronException.IllegalUserName =>
                    Response.status(CustomStatusType(HttpStatus.BAD_REQUEST_400, e.getMessage)).build()
                case e: ScalatronException.Exists =>
                    Response.status(CustomStatusType(HttpStatus.FORBIDDEN_403, e.getMessage)).build()
                case e: ScalatronException.CreationFailed =>
                    Response.status(CustomStatusType(HttpStatus.INTERNAL_SERVER_ERROR_500,e.getMessage)).build()
                case e: IOError =>
                    Response.status(CustomStatusType(HttpStatus.INTERNAL_SERVER_ERROR_500,e.getMessage)).build()
            }
        }
    }

    @GET
    @Path("{sample}")
    def getSourceFiles(@PathParam("sample") sampleName: String) = {
        if(!userSession.isLoggedOnAsAnyone) {
            Response.status(CustomStatusType(HttpStatus.UNAUTHORIZED_401, "must be logged on")).build()
        } else {
            try {
                val decodedSampleName = URLDecoder.decode(sampleName, "UTF-8")
                scalatron.sample(decodedSampleName) match {
                    case Some(sample) =>
                        val s = sample.sourceFiles
                        val sourceFiles = s.map(sf => SourcesResource.SourceFile(sf.filename, sf.code)).toArray
                        SourcesResource.SourceFiles(sourceFiles)
                    case None =>
                        Response.status(CustomStatusType(HttpStatus.NOT_FOUND_404, "sample '%s' (%s) does not exist" format(decodedSampleName, sampleName))).build()
                }
            } catch {
                case e: IllegalStateException =>
                    // sample source directory does not exist
                    Response.status(CustomStatusType(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage)).build()

                case e: IOError =>
                    // sample source files could not be read
                    Response.status(CustomStatusType(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage)).build()
            }
        }
    }


    @DELETE
    @Path("{sample}")
    def deleteSample(@PathParam("sample") sampleName: String) = {
        if(!userSession.isLoggedOnAsAdministrator) {
            Response.status(CustomStatusType(HttpStatus.UNAUTHORIZED_401, "must be logged on as '%s'" format Scalatron.Constants.AdminUserName)).build()
        } else {
            try {
                val decodedSampleName = URLDecoder.decode(sampleName, "UTF-8")
                scalatron.sample(decodedSampleName) match {
                    case Some(sample) =>
                        sample.delete()
                        Response.noContent().build()

                    case None =>
                        Response.status(CustomStatusType(HttpStatus.NOT_FOUND_404, "sample '%s' (%s) does not exist" format(decodedSampleName, sampleName))).build()
                }
            } catch {
                case e: ScalatronException.Forbidden =>
                    Response.status(CustomStatusType(HttpStatus.FORBIDDEN_403, e.getMessage)).build()
                case e: IOError =>
                    Response.status(CustomStatusType(HttpStatus.INTERNAL_SERVER_ERROR_500,e.getMessage)).build()
            }
        }
    }

}


object SamplesResource {
    case class SampleList(list: Array[Sample]) {
        def getSamples = list
    }

    case class Sample(n: String, r: String) {
        def getName = n
        def getUrl = r
    }


    case class CreateSample(var name: String, var fileList: Array[SourceFile]) {
        def this() = this(null, null)
        def getName = name
        def setName(l: String) { this.name = l }
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
