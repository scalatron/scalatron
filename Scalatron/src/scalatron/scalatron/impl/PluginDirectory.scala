package scalatron.scalatron.impl

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */

import java.io.File


case class PluginDirectory(pluginDirectory: File) {
  case class VersionedPlugin(version: Int, plugin: File)

  val botfile_r = """ScalatronBot_(\d+)\.jar""".r
  val botfile_f = "ScalatronBot_%d.jar"

  def mostRecentPlugin: Option[File] = {
    mostRecentPluginWithVersion.map(_.plugin)
  }

  def nextFilename: File = {
    val nextVersion = mostRecentPluginWithVersion.map(_.version).getOrElse(0) + 1
    val filePath = pluginDirectory.getCanonicalPath + "/" + botfile_f.format(nextVersion)
    new File(filePath)
  }

  private def mostRecentPluginWithVersion : Option[VersionedPlugin] = {
    val botnums = enumFiles

    if (botnums.isEmpty) {
      None
    } else {
      val most_recent = botnums.maxBy(_.version)
      Some(most_recent)
    }
  }

  private def enumFiles: Array[VersionedPlugin] = {
    val files = pluginDirectory.listFiles()
    val botnums = files.filter(file => botfile_r.findFirstIn(file.getName) match {
      case Some(num) => true
      case None => false
    }).map(file => botfile_r.findFirstIn(file.getName) match {
      case Some(num) => {
        val botfile_r(num2) = file.getName
        VersionedPlugin(num2.toInt, file)
      }
    })
    botnums
  }
}
