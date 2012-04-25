package scalatron.webServer.servelets

import scalatron.scalatron.api.Scalatron


case class WebContext(
    scalatron: Scalatron, // Scalatron API entry point
    webUiBaseDirectoryPath: String, // e.g. "/Scalatron/webui"
    verbose: Boolean)