/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */

package scalatronRemote.impl

import scalatronRemote.api.ScalatronRemote


case class ScalatronVersion(
    id: Int,
    label: String,
    date: Long,
    resourceUrl: String, // e.g. "/api/users/{user}/versions/{id}"
    user: ScalatronUser)
    extends ScalatronRemote.Version
{
    override def toString = id + " -> " + resourceUrl

    def sourceFiles = throw new UnsupportedOperationException
    def delete() { throw new UnsupportedOperationException }
}