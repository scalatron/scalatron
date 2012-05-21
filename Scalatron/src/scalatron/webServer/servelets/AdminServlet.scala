package scalatron.webServer.servelets

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import scalatron.core.Scalatron.Constants._
import scalatron.scalatron.impl.SourceFileCollection


case class AdminServlet(context: WebContext) extends BaseServlet {
    override def doGet(request: HttpServletRequest, response: HttpServletResponse) {
        val target = request.getRequestURI
        if(!target.startsWith("/admin/")) {
            serveErrorPage("unexpected admin url: '" + target + "'", request, response)
            System.err.println("error: unexpected admin url: '" + target + "'")
            return
        }


        // Make sure that our user is an admin. If not redirect to the login prompt.
        if(request.getSession.getAttribute("user") != AdminUserName) {
            response.sendRedirect("/user/%s/loginprompt".format(AdminUserName));
            return
        }

        val task = target.drop(7)
        task match {
            case "list" =>
                handleAdminListPage(request, response)
            case "createuser" =>
                handleAdminCreateUserPage(request, response)
            case "setpassword" =>
                handleAdminSetPasswordPage(request, response) // asks for new password
            case "updatepassword" =>
                handleAdminUpdatePasswordPage(request, response) // updates new password
            case "deleteuser" =>
                handleAdminDeleteUserPage(request, response)
            case _ =>
                serveErrorPage("malformed admin url: '" + target + "'", request, response)
                System.err.println("error: malformed admin url: '" + target + "'")
        }
    }

    // "/admin/list"
    private def handleAdminListPage(request: HttpServletRequest, response: HttpServletResponse) {
        val webUsers = context.scalatron.users()
        val webUserListHtml = {
            "<table>" + // <tr><td width=180px><strong>user name</strong></td><td><strong>options</strong></td></tr>" +
                (if(webUsers.isEmpty) {
                    "<tr><td colspan='2'>there are no accounts yet</td></tr>"
                } else {
                    webUsers.map(user => {
                        val userName = user.name
                        val optionsHtml =
                            if(user.isAdministrator) {
                                "<a href=\"/admin/setpassword?username=" + userName + "\">set password</a> "
                            } else {
                                "<a href=\"/admin/setpassword?username=" + userName + "\">set password</a> " +
                                    "<a href=\"/admin/deleteuser?username=" + userName + "\">delete</a> "
                            }
                        "<tr>" +
                            "<td width=180px>" + userName + "</td>" +
                            "<td>" + optionsHtml + "</td>" +
                            "</tr>"
                    }).mkString("\n")
                }) +
                "</table>"
        }

        val result = loadRelTextFile("admin.html").replace("$WebUserList$", webUserListHtml)
        serveString(result, request, response)
    }


    // "/admin/createUser?username=xxx"
    private def handleAdminCreateUserPage(request: HttpServletRequest, response: HttpServletResponse) {
        val userName = request.getParameter("username")
        if(userName == null || userName.isEmpty) {
            serveErrorPage("user name must not be empty", "/admin/list", "return to administration main page", request, response)
            System.err.println("error: received empty user name")
            return
        }
        if(!context.scalatron.isUserNameValid(userName)) {
            serveErrorPage("invalid user name: '" + userName + "': must not contain special characters", "/admin/list", "return to administration main page", request, response)
            System.err.println("error: received invalid user name: '" + userName + "'")
            return
        }

        // verify the password field
        val password1 = request.getParameter("password1")
        val password2 = request.getParameter("password2")
        if(password1 == null || password2 == null) {
            serveErrorPage("invalid password field", "/admin/list", "return to administration main page", request, response)
            System.err.println("error: received invalid password field")
            return
        }

        if(password1 != password2) {
            serveErrorPage("passwords fields do not match", "/admin/list", "return to administration main page", request, response)
            System.err.println("error: passwords fields do not match")
            return
        }

        // does that user already exist?
        try {
            val userOpt = context.scalatron.user(userName)
            userOpt match {
                case Some(user) =>
                    serveErrorPage("user already exists: '" + userName + "'", "/admin/list", "return to administration main page", request, response)
                    System.err.println("error: user already exists: '" + userName + "'")
                    return
                case None => // OK -- user does not yet exist
            }
        } catch {
            case t: Throwable =>
                serveErrorPage("unable to verify user directory for '" + userName + "'", "/admin/list", "return to administration main page", request, response)
                System.err.println("error: failed to check whether user exists: '" + userName + "': " + t)
                return
        }


        // create the user password config file
        try {
            context.scalatron.createUser(userName, password1, SourceFileCollection.initial(userName))
        } catch {
            case t: Throwable =>
                serveErrorPage("unable to write user configuration file for '" + userName + "'", "/admin/list", "return to administration main page", request, response)
                System.err.println("error: unable to create user account for: '" + userName + "': " + t)
                return
        }

        response.sendRedirect("/admin/list")
    }


    // "/admin/deleteUser?username=xxx"
    private def handleAdminDeleteUserPage(request: HttpServletRequest, response: HttpServletResponse) {
        val userName = request.getParameter("username")
        if(userName == null || userName.isEmpty || !context.scalatron.isUserNameValid(userName)) {
            serveErrorPage("invalid user name: '" + userName + "'", "/admin/list", "return to administration main page", request, response)
            System.err.println("error: received invalid user name: '" + userName + "'")
            return
        }

        val userOpt = context.scalatron.user(userName)
        userOpt match {
            case None =>
                serveErrorPage("the user account for '" + userName + "' does not exist", "/admin/list", "return to administration main page", request, response)
                System.err.println("error: the user account for '" + userName + "' does not exist")
                return

            case Some(user) =>
                if(user.isAdministrator) {
                    serveErrorPage("deleting the 'Administrator' account is not permitted", "/admin/list", "return to administration main page", request, response)
                    System.err.println("error: deleting the 'Administrator' account is not permitted")
                    return
                }

                try {
                    user.delete()
                } catch {
                    case t: Throwable =>
                        serveErrorPage(
                            "Failed to delete user directory for '" + userName + "'.<br>" +
                                "This may be because compilation is in progress. <br>" +
                                "Retry in a few seconds.", "/admin/list", "return to administration main page", request, response)
                        System.err.println("error: failed to delete user: '" + userName + "': " + t)
                        return
                }

                response.sendRedirect("/admin/list")
        }
    }


    // "/admin/setPassword?username=xxx"
    private def handleAdminSetPasswordPage(request: HttpServletRequest, response: HttpServletResponse) {
        val userName = request.getParameter("username")
        if(userName == null || userName.isEmpty || !context.scalatron.isUserNameValid(userName)) {
            serveErrorPage("invalid user name: '" + userName + "'", "/admin/list", "return to administration main page", request, response)
            System.err.println("error: received invalid user name: '" + userName + "'")
            return
        }

        val result = loadRelTextFile("setpassword.html").replace("$UserName$", userName)
        serveString(result, request, response)
    }


    // "/admin/updatePassword?username=xxx&password1=xxx&password2=xxx"
    private def handleAdminUpdatePasswordPage(request: HttpServletRequest, response: HttpServletResponse) {
        val userName = request.getParameter("username")
        if(userName == null || userName.isEmpty || !context.scalatron.isUserNameValid(userName)) {
            serveErrorPage("invalid user name: '" + userName + "'", "/admin/list", "return to administration main page", request, response)
            System.err.println("error: received invalid user name: '" + userName + "'")
            return
        }


        // verify the password
        val password1 = request.getParameter("password1")
        val password2 = request.getParameter("password2")
        if(password1 == null || password2 == null) {
            serveErrorPage("invalid password field", "/admin/list", "return to administration main page", request, response)
            System.err.println("error: received invalid password field")
            return
        }

        if(password1 != password2) {
            serveErrorPage("passwords fields do not match", "/admin/list", "return to administration main page", request, response)
            System.err.println("error: passwords fields do not match")
            return
        }


        // update the password setting in the user config file
        try {
            val userOpt = context.scalatron.user(userName)
            userOpt match {
                case None =>
                    serveErrorPage("the user account for '" + userName + "' does not exist", "/admin/list", "return to administration main page", request, response)
                    System.err.println("error: the user account for '" + userName + "' does not exist")
                    return

                case Some(user) =>
                    user.setPassword(password1)
            }
        } catch {
            case t: Throwable =>
                serveErrorPage("unable to write user configuration file for '" + userName + "'", "/admin/list", "return to administration main page", request, response)
                System.err.println("error: unable to update user password for: '" + userName + "': " + t)
                return
        }

        response.sendRedirect("/admin/list")
    }
}