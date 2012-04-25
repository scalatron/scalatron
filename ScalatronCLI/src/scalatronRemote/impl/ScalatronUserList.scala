/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatronRemote.impl

import scalatronRemote.api.ScalatronRemote
import scalatronRemote.api.ScalatronRemote.User


case class ScalatronUserList(users: Iterable[User], scalatron: ScalatronRemoteImpl) extends ScalatronRemote.UserList {
    override def toString = users.mkString(",")

    def iterator = users.iterator

    def user(name: String): Option[User] = users.find(_.name == name)

    def adminUser =
        user(ScalatronRemote.Constants.AdminUserName) match {
            case Some(user) => user
            case None => throw new IllegalStateException("server does not contain an Administrator user account")
        }
}