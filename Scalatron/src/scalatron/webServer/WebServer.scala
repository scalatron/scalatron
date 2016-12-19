package scalatron.webServer

import org.eclipse.jetty
import java.net.{UnknownHostException, InetAddress}
import org.eclipse.jetty.servlet.{ServletContextHandler, ServletHolder}
import javax.servlet.http.HttpServlet
import scalatron.webServer.rest.RestApplication
import scalatron.core.Scalatron
import com.sun.jersey.spi.container.servlet.ServletContainer
import scalatron.webServer.servelets.{
  AdminServlet,
  UserServlet,
  HomePageServlet,
  WebContext,
  GitServlet
}
import akka.actor.ActorSystem

/** Entry point for the web server that serves the Scalatron RESTful API and the browser UI ("Scalatron IDE").
  * Currently this uses Jetty, but it would be nice to migrate this to somthing based on Akka (e.g., Spray?)
  * at some point. That's why apply() receives a reference to the Akka actor system, which is not currently used.
  */
object WebServer {
  def cmdArgList = Iterable(
    "webui <dir>" -> "directory containing browser UI (default: ../webui)",
    "users <dir>" -> "directory containing browser UI workspaces (default: ../users)",
    "port <int>" -> "port to serve browser UI at (default: 8080)",
    "browser yes|no" -> "open a browser showing Scalatron IDE (default: yes)"
  )

  def apply(actorSystem: ActorSystem,
            scalatron: Scalatron,
            argMap: Map[String, String],
            verbose: Boolean) = {

    val webServerPort = argMap.get("-port").map(_.toInt).getOrElse(8080)
    if (verbose) println("Browser UI will be served on port: " + webServerPort)

    // for a help message, find out the hostname and IP address
    val (hostname, ipAddressString) =
      try {
        val addr = InetAddress.getLocalHost

        val hostname =
          try {
            addr.getHostName // Get hostname
          } catch {
            case e: UnknownHostException => "localhost"
          }

        val ipAddressString =
          try {
            addr.getHostAddress // get host IP address string (e.g. "123.123.200.120")
          } catch {
            case e: UnknownHostException => "127.0.0.1"
          }
        (hostname, ipAddressString)
      } catch {
        case t: Throwable => ("localhost", "127.0.0.1") // oh well
      }

    val browserUiUrl_Hostname = "http://" + hostname + ":" + webServerPort + "/"
    val browserUiUrl_IpAddress = "http://" + ipAddressString + ":" + webServerPort + "/"
    println(
      s"Players should point their browsers to '$browserUiUrl_Hostname' or '$browserUiUrl_IpAddress'")

    // extract the web UI base directory from the command line ("/webui")
    // construct the complete plug-in path and inform the user about it
    val webUiBaseDirectoryPathFallback = scalatron.installationDirectoryPath + "/" + "webui"
    val webUiBaseDirectoryPathArg =
      argMap.getOrElse("-webui", webUiBaseDirectoryPathFallback)
    val webUiBaseDirectoryPath =
      if (webUiBaseDirectoryPathArg.last == '/')
        webUiBaseDirectoryPathArg.dropRight(1)
      else webUiBaseDirectoryPathArg
    if (verbose)
      println("Will search for web UI files in: " + webUiBaseDirectoryPath)

    // extract the web user base directory from the command line ("/webuser")
    val webUserBaseDirectoryPathFallback = scalatron.installationDirectoryPath + "/" + "users"
    val webUserBaseDirectoryPathArg =
      argMap.getOrElse("-users", webUserBaseDirectoryPathFallback)
    val webUserBaseDirectoryPath =
      if (webUserBaseDirectoryPathArg.last == '/')
        webUserBaseDirectoryPathArg.dropRight(1)
      else webUserBaseDirectoryPathArg
    if (verbose)
      println("Will maintain web user content in: " + webUserBaseDirectoryPath)

    val webCtx = WebContext(scalatron, webUiBaseDirectoryPath, verbose)

    val jettyServer = new jetty.server.Server(webServerPort)

    val context = new ServletContextHandler(ServletContextHandler.SESSIONS)
    context.setContextPath("/")
    context.addServlet(holder(HomePageServlet(webCtx)), "/*")
    context.addServlet(holder(UserServlet(webCtx)), "/user/*")
    context.addServlet(holder(AdminServlet(webCtx)), "/admin/*")
    context.addServlet(holder(GitServlet(webCtx)), "/git/*")

    val jerseyServlet: ServletContainer = new ServletContainer(
      RestApplication(scalatron, verbose))

    context.addServlet(holder(jerseyServlet), "/api/*")

    jettyServer.setHandler(context)

    val webServer = new WebServer(jettyServer, verbose)

    // optionally: open a browser showing the browser UI
    val openBrowser = argMap.getOrElse("-browser", "yes") != "no"
    if (openBrowser && java.awt.Desktop.isDesktopSupported) {
      val desktop = java.awt.Desktop.getDesktop
      if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
        new Thread(new Runnable {
          def run(): Unit = {
            try {
              val waitTimeBeforeLaunchingBrowser = 3000 // give web server some time to start up
              Thread.sleep(waitTimeBeforeLaunchingBrowser)
              desktop.browse(new java.net.URI(browserUiUrl_IpAddress))
            } catch {
              case t: Throwable =>
                if (verbose)
                  System.err.println(
                    "warning: failed to open browser (for convenience only)")
            }
          }
        }).start()
      }
    }

    webServer
  }

  def holder(s: HttpServlet): ServletHolder = new ServletHolder(s)

}

class WebServer(server: jetty.server.Server, verbose: Boolean) {
  def start(): Unit = {
    if (verbose) println("Starting browser front-end...")
    try {
      server.start()
    } catch {
      case t: Throwable =>
        System.err.println("error: failed to start browser front-end: " + t)
    }
  }

  def stop(): Unit = {
    if (verbose) println("Stopping browser front-end...")
    server.stop()
  }

}
