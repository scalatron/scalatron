package scalatron.webServer.rest.resources

import scalatron.core.Scalatron
import javax.ws.rs.ext.ContextResolver
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.{UriInfo, Context}
import scalatron.webServer.rest.{Verbosity, UserSession}


trait Resource {
    /** Injected by JAX-RS */
    @Context protected[this] var s: ContextResolver[Scalatron] = null
    @Context protected[this] var v: ContextResolver[Verbosity] = null
    @Context protected[this] var req: HttpServletRequest = null
    @Context var uriInfo: UriInfo = null

    /** Get scalatron from the application context. */
    def scalatron = s.getContext(classOf[Scalatron])

    /** Get scalatron from the application context. */
    def verbose = v.getContext(classOf[Verbosity]).verbose

    /** Get the scala user session (wrapper for HttpSession) from the application context. */
    def userSession = new UserSession(req.getSession(true))
}