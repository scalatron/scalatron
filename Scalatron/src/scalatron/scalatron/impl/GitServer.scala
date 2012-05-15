package scalatron.scalatron.impl

import java.io.File
import org.eclipse.jgit.lib.{Repository, RepositoryCache}
import org.eclipse.jgit.lib.RepositoryCache.FileKey
import org.eclipse.jgit.util.FS
import scalatron.scalatron.api.Scalatron

/**
 * Used to cache User repositories, and ensure they are closed before shutdown.
 */
case class GitServer() {

    val cache = collection.mutable.Map[String, Repository]()

    def get(user: Scalatron.User) = cache.get(user.name)

    def get(user: ScalatronUser) = cache.getOrElseUpdate(user.name,
        RepositoryCache.open(FileKey.exact(new File(user.gitBaseDirectoryPath), FS.DETECTED), false)
    )

    def shutdown() = cache.values.map(_.close)
}
