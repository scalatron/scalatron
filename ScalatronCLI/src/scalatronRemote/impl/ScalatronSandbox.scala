package scalatronRemote.impl

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */

import scalatronRemote.api.ScalatronRemote
import scalatronRemote.impl.Connection.HttpFailureCodeException
import org.apache.http.HttpStatus
import scalatronRemote.api.ScalatronRemote.ScalatronException


case class ScalatronSandbox(
    id: Int,
    initialStateData: ScalatronSandboxState.StateData,    // cached, since server sends it anyway
    user: ScalatronUser)
    extends ScalatronRemote.Sandbox
{
    def initialState = ScalatronSandboxState(initialStateData, this)

    def delete() {
        try {
            // hack: we ignore the resource URLs to construct one that matches the exact count
            val resourceUrl =  "/api/users/%s/sandboxes/%d".format(user.name, id)
            user.scalatron.connection.DELETE(resourceUrl)

            // CBB: would be nice to mark this resource as unusable
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
