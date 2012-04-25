package scalatron.webServer.rest.resources

import javax.ws.rs.PathParam


/** Resource extending this trait will receive the injected name of the current user.
  * Please note that only stateless resources should extend from this trait.
  */
trait ResourceWithUser extends Resource {
    /** Injected by each request. The user only needs to check the permissions. */
    @PathParam("user") var userName: String = null

    def requireLoggedInAsOwningUser() {userSession.requireLoggedOnAsOwningUser(userName)}

    def requireLoggedInAsOwningUserOrAdministrator() {userSession.requireLoggedOnAsOwningUserOrAdministrator(userName)}
}
