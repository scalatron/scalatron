package scalatron.webServer.servelets

import scalatron.core.Scalatron


case class WebContext(
    scalatron: Scalatron, // Scalatron API entry point
    webUiBaseDirectoryPath: String, // e.g. "/Scalatron/webui"
    verbose: Boolean)