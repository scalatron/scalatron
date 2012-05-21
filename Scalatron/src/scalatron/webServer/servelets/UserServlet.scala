package scalatron.webServer.servelets

import scalatron.core.Scalatron
import scalatron.core.Scalatron.SourceFile
import Scalatron.Constants._
import javax.servlet.http.{Cookie, HttpServletResponse, HttpServletRequest}


/** Servelet handling `user/<user-name>/task`.
  * Note that this should really all be done via the REST API and Ajax on the client now.
  */
case class UserServlet(context: WebContext) extends BaseServlet {
    override def doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        doGet(req, resp)
    }

    override def doPut(req: HttpServletRequest, resp: HttpServletResponse) {
        doGet(req, resp)
    }

    override def doGet(request: HttpServletRequest, response: HttpServletResponse) {

        val target = request.getRequestURI;

        if(!target.startsWith("/user/")) {
            serveErrorPage("unexpected user url: '" + target + "'", request, response)
            System.err.println("error: unexpected user url: '" + target + "'")
            return
        }

        val suffix = target.drop(6)
        val parts = suffix.split('/')
        if(parts.length != 2) {
            serveErrorPage("malformed user url: '" + target + "'", request, response)
            System.err.println("error: malformed user url: '" + target + "'")
            return
        }

        val userName = parts(0)
        val task = parts(1)

        task match {
            case "loginprompt" =>
                // "/user/name/loginprompt" -> ask for user credentials
                if(context.verbose) println("/user/name/loginprompt for user: " + userName)
                val userOpt = context.scalatron.user(userName)
                userOpt match {
                    case None =>
                        serveErrorPage("the user account for '" + userName + "' does not exist", "/admin/list", "return to administration main page", request, response)
                        System.err.println("error: the user account for '" + userName + "' does not exist")
                        return

                    case Some(user) =>
                        user.getPasswordOpt match {
                            case None => // failed to check password. error.
                                serveErrorPage("no password is configured for user '" + userName + "' - login disabled", request, response)
                                System.err.println("error: no password is configured for user: '" + userName + "' - login disabled")

                            case Some(password) =>
                                if(password.isEmpty) {
                                    // No passwortd needed - just init the session.
                                    setUserToSession(request, response, userName)

                                    if(context.verbose) println("user has empty password, skipping log-in: " + userName)
                                    if(user.isAdministrator) {
                                        response.sendRedirect("/admin/list")
                                    } else {
                                        response.sendRedirect("/user/" + userName + "/edit")
                                    }
                                } else {
                                    val result = loadRelTextFile("loginprompt.html").replace("$UserName$", userName)
                                    serveString(result, request, response)
                                }
                        }
                }

            case "login" =>
                handleUserLogin(userName, request, response)

            case "edit" =>
                handleUserEdit(userName, request, response)

            case _ =>
                serveErrorPage("unknown user task: '" + task + "'", request, response)
                System.err.println("error: unknown user task received from browser: '" + task + "'")
        }
    }


    // "/user/name/login" -> verify
    private def handleUserLogin(userName: String, request: HttpServletRequest, response: HttpServletResponse) {
        if(context.verbose) println("/user/name/login for user: " + userName)

        val candidatePassword = request.getParameter("password")
        if(candidatePassword == null) {
            serveErrorPage("login request with null password for user: " + userName, request, response)
            System.err.println("error: login request with null password for user: " + userName)
            return
        }

        // ensure that the password is correct
        val userOpt = context.scalatron.user(userName)
        userOpt match {
            case None =>
                serveErrorPage("the user account for '" + userName + "' does not exist", "/admin/list", "return to administration main page", request, response)
                System.err.println("error: the user account for '" + userName + "' does not exist")
                return

            case Some(user) =>
                user.getPasswordOpt match {
                    case None =>
                        serveErrorPage("no password is configured for user '" + userName + "' - login disabled", request, response)
                        System.err.println("error: no password is configured for user: '" + userName + "' - login disabled")
                        return
                    case Some(actualPassword) =>
                        if(!actualPassword.isEmpty && actualPassword != candidatePassword) {
                            serveErrorPage("sorry, wrong password for user '" + userName + "'. <a href=\"/user/" + userName + "/loginprompt\">Please try again.</a>", request, response)
                            System.err.println("warning: login attempt with invalid password for user: " + userName)
                            return
                        }
                }

                setUserToSession(request, response, userName);

                // TODO: this is totally retarded - we need to store the log-in state as session state
                if(context.verbose) println("user logged in: " + userName)
                if(user.isAdministrator) {
                    response.sendRedirect("/admin/list")
                } else {
                    response.sendRedirect("/user/" + userName + "/edit")
                }
        }
    }

    private def setUserToSession(request: HttpServletRequest, response: HttpServletResponse, userName: String) {
        // Set at least the user as session attribute - can be used by the web socket to identify the bot.
        request.getSession(true).setAttribute("user", userName);
        // val list = request.getCookies.toList
        //if(!list.exists(e => e.getName == "scalatron-user")) {
        val cookie = new Cookie("scalatron-user", userName);
        cookie.setMaxAge(24 * 60 * 60);
        response.addCookie(cookie);
        //}
    }

    // "/user/name/edit" -> open editor; before that, create bot if necessary
    private def handleUserEdit(userName: String, request: HttpServletRequest, response: HttpServletResponse) {
        // Make sure we have a user session - If not we will redirect to the the user prompt
        if(request.getSession(true).getAttribute("user") != userName) {
            response.sendRedirect("/user/%s/loginprompt".format(userName));
            return;
        }

        val userOpt = context.scalatron.user(userName)
        userOpt match {
            case None =>
                serveErrorPage("the user account for '" + userName + "' does not exist", "/admin/list", "return to administration main page", request, response)
                System.err.println("error: the user account for '" + userName + "' does not exist")
                return

            case Some(user) =>
                val botSources =
                    try {
                        user.sourceFiles
                    } catch {
                        case t: Throwable =>
                            serveErrorPage("could not retrieve sources for user '" + userName + "'", request, response)
                            System.err.println("error: failed to retrieve sources for '" + userName + "': " + t)
                            return
                    }


                val result =
                    loadRelTextFile("webclient.html")
                    .replace("$BotName$", userName)

                serveString(result, request, response)
        }
    }
}