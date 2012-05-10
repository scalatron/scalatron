package scalatronRemote.impl

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */

import scalatronRemote.api.ScalatronRemote


case class ScalatronSampleList(samples: Iterable[ScalatronSample], scalatron: ScalatronRemoteImpl) extends ScalatronRemote.SampleList {
    override def toString = samples.mkString(",")
    override def size = samples.size
    def iterator = samples.iterator
    def get(name: String): Option[ScalatronSample] = samples.find(_.name == name)
}