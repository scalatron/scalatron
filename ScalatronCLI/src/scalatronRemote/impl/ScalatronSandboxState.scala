package scalatronRemote.impl

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */

import scalatronRemote.api.ScalatronRemote
import scalatronRemote.api.ScalatronRemote._
import scalatronRemote.impl.Connection.HttpFailureCodeException
import org.apache.http.HttpStatus
import ScalatronSandboxState.StateData


case class ScalatronSandboxState(stateData: StateData, sandbox: ScalatronSandbox) extends ScalatronRemote.SandboxState
{
    override def toString = sandbox.id + " -> " + stateData.resourceUrl

    def time = stateData.time

    def entities = stateData.entities

    def step(count: Int) = {
        try {
            // hack: we ignore the resource URLs to construct one that matches the exact count
            val desiredTime = time + count
            val user = sandbox.user
            val nextResourceUrl =  "/api/users/%s/sandboxes/%d/%d".format(user.name, sandbox.id, desiredTime)
            val jsonOpt = user.scalatron.connection.GET_json(nextResourceUrl)
            val nextStateData = ScalatronSandboxState.StateData.fromJson(jsonOpt)
            if(nextStateData.id != stateData.id)
                throw new IllegalStateException("stepping from state of sandbox id=%d returned state in sandbox with id=%d".format(sandbox.id, nextStateData.id))

            ScalatronSandboxState(nextStateData, sandbox)
        } catch {
            case e: HttpFailureCodeException =>
                e.httpCode match {
                    case HttpStatus.SC_UNAUTHORIZED =>
                        // not logged on as this user or as Administrator
                        throw ScalatronException.NotAuthorized(e.reason)
                    case HttpStatus.SC_NOT_FOUND =>
                        // this user does not exist on the server
                        throw ScalatronException.NotFound(e.reason)
                    case _ =>
                        throw e // rethrow
                }
        }
    }
}

object ScalatronSandboxState {
    case class StateData(
        id: Int,
        time: Int,
        resourceUrl: String, // e.g. "/api/users/{user}/sandboxes/{id}/{time}"
        urlPlus1: String,   // e.g. "/api/users/{user}/sandboxes/{id}/{time+1}"
        urlPlus2: String, // e.g. "/api/users/{user}/sandboxes/{id}/{time+2}"
        urlPlus10: String, // e.g. "/api/users/{user}/sandboxes/{id}/{time+10}"
        entities: Iterable[ScalatronRemote.SandboxEntity]) // e.g. "/api/users/{user}/sandboxes/{id}/{time+10}"

    object StateData {
        def fromJson(jsonOpt: JSonOpt) = {
            val jsonMap = jsonOpt.asMap

            val entityList = jsonMap.asList[Map[String,Any]]("entities")

            val entities = entityList.map(e => {
                val map = JSonMap(e, jsonOpt)

                val icMap = JSonMap(map.map("input").asInstanceOf[Map[String,Any]], jsonOpt)
                val inputCommand = Command(
                    icMap.asString("opcode"),
                    icMap.asStringMap("params"))

                val outputCommandList = map.asList[Map[String,Any]]("output")
                val outputCommands = outputCommandList.map(c => {
                    val cMap = JSonMap(c, jsonOpt)
                    Command(
                        cMap.asString("opcode"),
                        cMap.asStringMap("params"))
                })

                ScalatronSandboxEntity(
                    map.asInt("id"),
                    map.asString("name"),
                    map.asBoolean("master"),
                    inputCommand,
                    outputCommands,
                    map.asString("debugOutput")
                )
            })

            StateData(
                jsonMap.asInt("id"),
                jsonMap.asInt("time"),
                jsonMap.asString("url"),
                jsonMap.asString("urlPlus1"),
                jsonMap.asString("urlPlus2"),
                jsonMap.asString("urlPlus10"),
                entities
            )
        }
    }
}