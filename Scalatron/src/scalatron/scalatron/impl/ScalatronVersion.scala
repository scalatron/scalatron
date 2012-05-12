package scalatron.scalatron.impl

import scalatron.scalatron.api.Scalatron
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.treewalk.TreeWalk
import scalatron.scalatron.api.Scalatron.{SourceFile, SourceFileCollection, User}

case class ScalatronVersion(commit: RevCommit, user: ScalatronUser)
    extends Scalatron.Version {

  def id = commit.getId.name
  def label = commit.getShortMessage
  def date = commit.getCommitterIdent.getWhen.getTime
}
