package scalatron.webServer.servelets

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import io.Source
import java.net.URL
import java.io.{InputStream, BufferedOutputStream, BufferedInputStream}
import scalatron.scalatron.impl.FileUtil
import scalatron.scalatron.impl.FileUtil


trait BaseServlet extends HttpServlet {

    def context: WebContext


    override def doGet(req: HttpServletRequest, resp: HttpServletResponse) {}

    def loadRelTextFile(relativePath: String) = FileUtil.loadTextFileContents(relativeToAbsolutePath(relativePath))

    def relativeToAbsolutePath(relativePath: String) = {
        if(relativePath.contains(".."))
            throw new IllegalAccessError("error: illegal access attempt via relative path: " + relativePath)
        if(relativePath.startsWith("/"))
            context.webUiBaseDirectoryPath + relativePath
        else
            context.webUiBaseDirectoryPath + "/" + relativePath
    }


    def serveString(string: String, request: HttpServletRequest, response: HttpServletResponse) {
        response.setContentType("text/html")
        response.setStatus(HttpServletResponse.SC_OK)
        response.getWriter.append(string)
        //request.setHandled(true)
    }

    /** Replacement strings: $Error$, $ReturnUrl$, $ReturnUrlText$,
      */
    def serveErrorPage(
        errorString: String,
        returnUrl: String,
        returnUrlText: String,
        request: HttpServletRequest,
        response: HttpServletResponse) {
        serveString(
            loadRelTextFile("error.html")
            .replace("$Error$", errorString)
            .replace("$ReturnUrl$", returnUrl)
            .replace("$ReturnUrlText$", returnUrlText),
            request, response)
    }

    def serveErrorPage(errorString: String, request: HttpServletRequest, response: HttpServletResponse) {
        serveErrorPage(errorString, "/", "return to welcome page", request, response)
    }

    // from http://www.devx.com/getHelpOn/Article/11698/1954
    def streamFile(absolutePath: String, mimeType: String, request: HttpServletRequest, response: HttpServletResponse) {
        if(context.verbose) println("streaming file: " + absolutePath)
        response.setContentType(mimeType)
        val outputStream = response.getOutputStream

        var bis: BufferedInputStream = null
        var bos: BufferedOutputStream = null
        try {
            val url = new URL("file", "localhost", absolutePath)
            val urlc = url.openConnection()
            val length = urlc.getContentLength
            response.setContentLength(length)

            // Use Buffered Stream for reading/writing.
            val in: InputStream = urlc.getInputStream
            bis = new BufferedInputStream(in)
            bos = new BufferedOutputStream(outputStream)
            val buff = Array.ofDim[Byte](length)

            // Simple read/write loop.
            var looping = true
            while(looping) {
                val bytesRead = bis.read(buff, 0, buff.length)
                if(bytesRead == -1)
                    looping = false
                else
                    bos.write(buff, 0, bytesRead)
            }
        } catch {
            case t: Throwable =>
                System.err.println("error: failed to serve file '" + absolutePath + "': " + t)
                response.setStatus(HttpServletResponse.SC_NOT_FOUND)
        } finally {
            if(bis != null) {
                bis.close()
            }
            if(bos != null) {
                bos.close()
            }
            if(outputStream != null) {
                outputStream.flush();
                outputStream.close();
            }
        }
    }
}