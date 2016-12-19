package scalatron.webServer.rest

import javax.servlet.http.HttpSession
import javax.ws.rs.core.Response.ResponseBuilder
import javax.ws.rs.core.{NewCookie, Response}
import scalatron.webServer.rest.UserSession.UserAttributeKey
import scalatron.core.Scalatron

/** Scala wrapper for Java HttpSession. */
object UserSession {
  val UserAttributeKey = "user"
  val SandboxAttributeKey = "sandbox"
}

case class UserSession(session: HttpSession) {
  def usernameOpt: Option[String] = getStringOpt(UserAttributeKey)

  def init(userName: String): ResponseBuilder = {
    session.setAttribute(UserAttributeKey, userName)
    Response.ok().cookie(new NewCookie("scalatron-user", userName))
  }

  def destroy(): Unit = {
    session.removeAttribute(UserAttributeKey)
    session.invalidate()
  }

  def +=(kv: (String, Any)): Unit = { session.setAttribute(kv._1, kv._2) }

  def -=(key: String): Unit = { session.removeAttribute(key) }

  /** Returns the value of the given key as a Some(Any) instance if it is defined, or None if not. */
  def get(key: String): Option[Any] = {
    val value = session.getAttribute(key)
    Option(value)
  }

  /** Returns the value of the given key as a Some(String) instance if it is defined, or None
    * if not. Returns None if the item is not a string. */
  def getStringOpt(key: String): Option[String] = {
    val value = session.getAttribute(key)
    if (value == null) {
      None
    } else {
      value match {
        case s: String => Some(s)
        case _ =>
          throw new IllegalStateException(
            "expected string value for attribute: " + key)
      }
    }
  }

  def requireLoggedOnAsOwningUser(candidateUserName: String): Unit = {
    usernameOpt match {
      case None =>
        throw new SecurityException("Access denied: not logged in")
      case Some(sessionUserName) =>
        if (sessionUserName != candidateUserName)
          throw new SecurityException("Access denied")
    }
  }

  def requireLoggedOnAsOwningUserOrAdministrator(
      candidateUserName: String): Unit = {
    usernameOpt match {
      case None =>
        throw new SecurityException("Access denied: not logged in")
      case Some(sessionUserName) =>
        if (sessionUserName != Scalatron.Constants.AdminUserName && sessionUserName != candidateUserName)
          throw new SecurityException("Access denied")
    }
  }

  def requireLoggedOnAsAdministrator(): Unit = {
    usernameOpt match {
      case None =>
        throw new SecurityException("Access denied: not logged in")
      case Some(sessionUserName) =>
        if (sessionUserName != Scalatron.Constants.AdminUserName)
          throw new SecurityException("Access denied: Administrator only")
    }
  }

  def isLoggedOnAsAnyone = usernameOpt.isDefined

  def isLoggedOnAsAdministrator =
    usernameOpt.contains(Scalatron.Constants.AdminUserName)

  def isLoggedOnAsUser(candidateUserName: String) =
    usernameOpt.contains(candidateUserName)

  def isLoggedOnAsUserOrAdministrator(candidateUserName: String) =
    isLoggedOnAsAdministrator || isLoggedOnAsUser(candidateUserName)

}
