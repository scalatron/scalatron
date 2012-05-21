package scalatron.scalatron.impl

import scalatron.core.Scalatron
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.treewalk.TreeWalk
import scalatron.core.Scalatron.{SourceFile, SourceFileCollection, User}
import org.eclipse.jgit.dircache.DirCacheCheckout
import org.eclipse.jgit.errors.{CorruptObjectException, NoWorkTreeException}
import java.io.{IOException, IOError}

case class ScalatronVersion(commit: RevCommit, user: ScalatronUser) extends Scalatron.Version {
    def id = commit.getId.name
    def label = commit.getShortMessage
    def date = commit.getCommitterIdent.getWhen.getTime

    /**
      * Restores the working directory to a given version.
      * This is roughly similar to running 'git checkout .', which isn't
      * properly supported by the JGit high-level API.
      * This was inspired by JGit's ResetCommand.checkoutIndex().
      * We don't want to reset because that will change the branch as well.
      * @throws IOError if the repository is in a corrupt state.
      */
    def restore() {
        try {
            val dc = user.gitRepository.lockDirCache()
            try {
                val checkout = new DirCacheCheckout(user.gitRepository, dc, commit.getTree)
                checkout.setFailOnConflict(false)
                checkout.checkout
            } finally {
                dc.unlock()
            }
        } catch {
            case e: NoWorkTreeException => throw new IOError(e)
            case e: CorruptObjectException => throw new IOError(e)
            case e: IOException => throw new IOError(e)
        }
    }
}
