package scalatron.scalatron.impl

import scalatron.scalatron.impl.FileUtil.deleteRecursively
import scalatron.core.Scalatron
import scalatron.core.Scalatron.SourceFile
import scalatron.core.Scalatron.SourceFileCollection

case class ScalatronSample(name: String, scalatron: ScalatronImpl)
    extends Scalatron.Sample {
  val sampleDirectoryPath = scalatron.samplesBaseDirectoryPath + "/" + name
  val sampleSourceDirectoryPath = sampleDirectoryPath + "/" + Scalatron.Constants.SamplesSourceDirectoryName

  def sourceFiles: Iterable[SourceFile] =
    SourceFileCollection.loadFrom(sampleSourceDirectoryPath)

  def delete(): Unit = {
    deleteRecursively(sampleDirectoryPath,
                      atThisLevel = true,
                      verbose = scalatron.verbose)
  }
}
