package scalatron.scalatron.impl

import scalatron.util.FileUtil
import FileUtil.deleteRecursively
import scalatron.scalatron.api.Scalatron
import Scalatron.SourceFileCollection


case class ScalatronVersion(id: Int, label: String, date: Long, user: ScalatronUser)
    extends Scalatron.Version {
    val versionDirectoryPath = user.versionBaseDirectoryPath + "/" + id
    def sourceFiles = SourceFileCollection.loadFrom(versionDirectoryPath, user.scalatron.verbose)
    def delete() { deleteRecursively(versionDirectoryPath, user.scalatron.verbose) }
}
