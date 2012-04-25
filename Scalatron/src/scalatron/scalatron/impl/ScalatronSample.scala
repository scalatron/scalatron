package scalatron.scalatron.impl

import scalatron.scalatron.api.Scalatron.SourceFile
import ScalatronUser.loadSourceFiles
import ScalatronUser.deleteRecursively
import scalatron.scalatron.api.Scalatron


case class ScalatronSample(name: String, scalatron: ScalatronImpl) extends Scalatron.Sample {
    val sampleDirectoryPath = scalatron.samplesBaseDirectoryPath + "/" + name
    val sampleSourceDirectoryPath = sampleDirectoryPath + "/" + Scalatron.Constants.SamplesSourceDirectoryName

    def sourceFiles: Iterable[SourceFile] = loadSourceFiles(sampleSourceDirectoryPath)

    def delete() { deleteRecursively(sampleDirectoryPath, scalatron.verbose) }
}
