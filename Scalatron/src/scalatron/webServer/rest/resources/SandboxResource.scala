package scalatron.webServer.rest.resources

import javax.ws.rs._
import core.{Response, MediaType}
import scalatron.scalatron.api.Scalatron.SandboxState
import collection.JavaConversions
import collection.JavaConversions.JMapWrapper
import scalatron.botwar.CommandParser
import scalatron.webServer.rest.UserSession
import UserSession.SandboxAttributeKey
import org.eclipse.jetty.http.HttpStatus
import scalatron.scalatron.api.Scalatron


@Produces(Array(MediaType.APPLICATION_JSON))
@Consumes(Array(MediaType.APPLICATION_JSON))
@Path("users/{user}/sandbox")
class SandboxResource extends ResourceWithUser {
    @POST
    def create(startConfig: SandboxResource.StartConfig) = {
        if(!userSession.isLoggedOnAsUserOrAdministrator(userName)) {
            Response.status(CustomStatusType(HttpStatus.UNAUTHORIZED_401, "must be logged on as '" + userName + "' or '" + Scalatron.Constants.AdminUserName + "'")).build()
        } else {
            try {
                scalatron.user(userName) match {
                    case Some(user) =>
                        delete()

                        // extract the arguments, like Map("-x" -> 50, "-y" -> 50)
                        val argMap = JMapWrapper(startConfig.getConfig).toMap
                        val state = user.createSandbox(argMap)
                        userSession += SandboxAttributeKey -> state

                        Response.ok(SandboxResource.createSandboxResult(userName, state)).build()

                    case None =>
                        Response.status(CustomStatusType(HttpStatus.NOT_FOUND_404, "user '" + userName + "' does not exist")).build()
                }
            } catch {
                case e: IllegalStateException =>
                    // source directory does not exist
                    Response.status(CustomStatusType(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage)).build()
            }
        }
    }

    @DELETE
    def delete() {
        if(!userSession.isLoggedOnAsUserOrAdministrator(userName)) {
            Response.status(CustomStatusType(HttpStatus.UNAUTHORIZED_401, "must be logged on as '" + userName + "' or '" + Scalatron.Constants.AdminUserName + "'")).build()
        } else {
            scalatron.user(userName) match {
                case Some(user) =>
                    userSession -= SandboxAttributeKey
                    Response.noContent().build()

                case None =>
                    Response.status(CustomStatusType(HttpStatus.NOT_FOUND_404, "user '" + userName + "' does not exist")).build()
            }
        }
    }

    @GET
    @Path("{id}/{time}")
    def getForIdAndTime(@PathParam("id") id: Int, @PathParam("time") time: Int): Response = {
        if(!userSession.isLoggedOnAsUserOrAdministrator(userName)) {
            Response.status(CustomStatusType(HttpStatus.UNAUTHORIZED_401, "must be logged on as '" + userName + "' or '" + Scalatron.Constants.AdminUserName + "'")).build()
        } else {
            scalatron.user(userName) match {
                case Some(user) =>
                    userSession.get(SandboxAttributeKey) match {
                        case None =>
                            // Ok no sandbox found - client must create one.
                            Response.status(CustomStatusType(HttpStatus.NOT_FOUND_404, "sandbox id=%d at time=%d for user '%s' does not exist".format(id, time, userName))).build()

                        case Some(currentSandboxState: SandboxState) =>
                            val currentId = currentSandboxState.id
                            if(currentId != id) {
                                Response.status(CustomStatusType(HttpStatus.NOT_FOUND_404, "sandbox with id=%d does not exist for user '%s'".format(id, userName))).build()
                            } else {
                                val currentTime = currentSandboxState.time
                                val timeDelta = time - currentTime
                                if(timeDelta == 0) {
                                    Response.ok(SandboxResource.createSandboxResult(userName, currentSandboxState)).build()
                                } else
                                if(timeDelta < 0) {
                                    Response.status(CustomStatusType(HttpStatus.NOT_FOUND_404, "cannot step sandbox with id=%d for user '%s' backwards in time".format(id, userName, timeDelta))).build()
                                } else {
                                    val updatedSandboxState = currentSandboxState.step(timeDelta)
                                    userSession += SandboxAttributeKey -> updatedSandboxState
                                    Response.ok(SandboxResource.createSandboxResult(userName, updatedSandboxState)).build()
                                }
                            }
                    }

                case None =>
                    Response.status(CustomStatusType(HttpStatus.NOT_FOUND_404, "user '" + userName + "' does not exist")).build()
            }
        }
    }


    @PUT
    def next(step: Steps): Response = {
        requireLoggedInAsOwningUserOrAdministrator()

        userSession.get(SandboxAttributeKey) match {
            case None =>
                // Ok no sandbox found - client must create one.
                Response.noContent().build();

            case Some(sandbox: SandboxState) =>
                val sandboxState = sandbox.step(step.getSteps)
                val entities = sandboxState.entities

                userSession += SandboxAttributeKey -> sandboxState

                val mappedEntities = entities.map(e => {
                    val inputString = e.mostRecentControlFunctionInput
                    val (opcode, params) =
                        if(inputString.isEmpty) {
                            ("", Map.empty[String, String])
                        } else {
                            CommandParser.splitCommandIntoOpcodeAndParameters(inputString)
                        }

                    SandboxResource.EntityDto(
                        e.id,
                        e.name,
                        e.isMaster,
                        SandboxResource.InputCommand(opcode, JavaConversions.asJavaMap(params)),
                        SandboxResource.extractOutput(e.mostRecentControlFunctionOutput),
                        e.debugOutput)
                }).toArray

                Response.ok(new SandboxResult(mappedEntities)).build();
        }
    }

}

object SandboxResource {
    private def createSandboxResult(userName: String, state: Scalatron.SandboxState) : SandboxResource.SandboxCreationResult = {
        val sandboxId = state.id
        val sandboxTime = state.time

        val timePlus0 = "/api/users/%s/sandbox/%d/%d".format(userName, sandboxId, sandboxTime)
        val timePlus1 = "/api/users/%s/sandbox/%d/%d".format(userName, sandboxId, sandboxTime+1)
        val timePlus2 = "/api/users/%s/sandbox/%d/%d".format(userName, sandboxId, sandboxTime+2)
        val timePlus10 = "/api/users/%s/sandbox/%d/%d".format(userName, sandboxId, sandboxTime+10)

        val mappedEntities = state.entities.map(e => {
            val inputString = e.mostRecentControlFunctionInput
            val (opcode, params) =
                if(inputString.isEmpty) {
                    ("", Map.empty[String, String])
                } else {
                    CommandParser.splitCommandIntoOpcodeAndParameters(inputString)
                }

            SandboxResource.EntityDto(
                e.id,
                e.name,
                e.isMaster,
                SandboxResource.InputCommand(opcode, JavaConversions.asJavaMap(params)),
                extractOutput(e.mostRecentControlFunctionOutput),
                e.debugOutput)
        }).toArray

        SandboxResource.SandboxCreationResult(
            sandboxId,
            timePlus0, timePlus1, timePlus2, timePlus10,
            state.time,
            mappedEntities
        )
    }

    private def extractOutput(in: Iterable[(String, Iterable[(String, String)])]): Array[SandboxResource.InputCommand] =
        in.map(e => {
            val op = e._1
            val params = e._2
            SandboxResource.InputCommand(op, JavaConversions.asJavaMap(params.toMap))
        }).toArray



    // incoming to PUT
    case class StartConfig(var config: java.util.HashMap[String, String]) {
        def this() = this(null)
        def getConfig = config
        def setConfig(config: java.util.HashMap[String, String]) { this.config = config }
    }

    // outgoing from PUT
    case class SandboxCreationResult(
        id: Int,
        timePlus0: String, timePlus1: String, timePlus2: String, timePlus10: String,
        time: Int,
        entities: Array[EntityDto]
    )
    {
        def getId = id

        def getUrl = timePlus0
        def getUrlPlus1 = timePlus1
        def getUrlPlus2 = timePlus2
        def getUrlPlus10 = timePlus10

        def getTime = time

        def getEntities = entities
    }

    case class EntityDto(id: Int, name: String, master: Boolean, input: InputCommand, output: Array[InputCommand], debugOut: String) {
        def getId = id
        def getDebugOutput = debugOut
        def getName = name
        def isMaster = master
        def getOutput = output
        def getInput = input
    }

    case class InputCommand(oc: String, p: java.util.Map[String, String]) {
        def getOpcode = oc
        def getParams = p
    }
}



case class SandboxResult(entities: Array[SandboxResource.EntityDto]) {
    def getEntities = entities
}


class Steps(var i: Int) {
    def this() = this(0)

    def getSteps = i

    def setSteps(s: Int) {
        i = s
    }
}