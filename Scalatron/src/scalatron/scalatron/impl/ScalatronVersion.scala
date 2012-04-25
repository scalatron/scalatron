package scalatron.scalatron.impl

import scalatron.scalatron.api.Scalatron.SourceFile
import ScalatronUser.loadSourceFiles
import ScalatronUser.deleteRecursively
import scalatron.scalatron.api.Scalatron


case class ScalatronVersion(id: Int, label: String, date: Long, user: ScalatronUser)
    extends Scalatron.Version {
    val versionDirectoryPath = user.versionBaseDirectoryPath + "/" + id

    def sourceFiles: Iterable[SourceFile] = loadSourceFiles(versionDirectoryPath)

    def delete() {deleteRecursively(versionDirectoryPath, user.scalatron.verbose)}
}
