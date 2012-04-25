package scalatron.webServer.rest.resources

import javax.ws.rs.core.Response.{Status, StatusType}
import javax.ws.rs.core.Response.Status.Family


case class CustomStatusType(statusCode: Int, reasonPhrase: String) extends StatusType {
    def getReasonPhrase = reasonPhrase
    def getStatusCode = statusCode
    def getFamily = (statusCode/100) match {
        case 1 => Family.INFORMATIONAL
        case 2 => Family.SUCCESSFUL
        case 3 => Family.REDIRECTION
        case 4 => Family.CLIENT_ERROR
        case 5 => Family.SERVER_ERROR
        case _ => Family.OTHER
    }
}