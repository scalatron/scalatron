package scalatron.webServer.servelets

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import java.io.File


case class HomePageServlet(context: WebContext) extends BaseServlet {
    override def doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        if(req.getRequestURI == "" || req.getRequestURI == "/") {
            // enumerate web user account details as collection of (name, path) tuples
            val webUsers = context.scalatron.users()

            val webUserListHtml = {
                "<ul>" +
                    (if(webUsers.isEmpty) {
                        "<li>there are no accounts yet</li>"
                    } else {
                        webUsers.map(user => {
                            "<li><a href=\"/user/" + user.name + "/loginprompt\">" + user.name + "</a></li>"
                        }).mkString("\n")
                    }) +
                    "</ul>"
            }

            val result = loadRelTextFile("index.html").replace("$WebUserList$", webUserListHtml)
            serveString(result, req, resp)
        } else {
            doGetOther(req, resp)
        }
    }

    def doGetOther(request: HttpServletRequest, response: HttpServletResponse) {
        val absolutePath = relativeToAbsolutePath(request.getRequestURI)
        val fileName = new File(absolutePath).getName

        if(fileName.endsWith(".html")) streamFile(absolutePath, "text/html", request, response)
        else if(fileName.endsWith(".css")) streamFile(absolutePath, "text/css", request, response)
        else if(fileName.endsWith(".ico")) streamFile(absolutePath, "image/ico", request, response)
        else if(fileName.endsWith(".js")) streamFile(absolutePath, "text/javascript" /*"text/js"*/ , request, response)
        else if(fileName.endsWith(".pde")) streamFile(absolutePath, "application/processing", request, response)
        else if(fileName.endsWith(".gif")) streamFile(absolutePath, "image/gif", request, response)
        else if(fileName.endsWith(".scala")) streamFile(absolutePath, "text/scala", request, response)
        else {
            System.err.println("error: unknown file format: '" + fileName + "'")
        }
    }
}