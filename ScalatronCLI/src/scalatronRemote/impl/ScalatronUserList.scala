/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatronRemote.impl

import scalatronRemote.api.ScalatronRemote


case class ScalatronUserList(users: Iterable[ScalatronUser], scalatron: ScalatronRemoteImpl) extends ScalatronRemote.UserList {
    override def toString = users.mkString(",")
    override def size = users.size
    def iterator = users.iterator

    def get(name: String): Option[ScalatronUser] = users.find(_.name == name)

    def adminUser =
        get(ScalatronRemote.Constants.AdminUserName) match {
            case Some(user) => user
            case None => throw new IllegalStateException("server does not contain an Administrator user account")
        }
}