package scalatron.scalatron.impl

import java.io.{File, FileWriter}
import scala.io.Source
import FileUtil.use


object ConfigFile
{
    /** Loads and parses a file with one key/val pair per line to Map[String,String].
      * Throws an exception if an error occurs. */
    def loadConfigFile(absolutePath: String) = {
        val source = Source.fromFile(absolutePath)
        try {
            source
            .getLines()
            .map(_.split('='))
            .map(a => if( a.length == 2 ) (a(0), a(1)) else (a(0), ""))
            .toMap
        } finally {
            source.close()
        }
    }


    /** Loads, parses, updates and writes back a file with one key/val pair per line. */
    def updateConfigFile(absolutePath: String, key: String, value: String) {
        updateConfigFileMulti(absolutePath, Map(( key -> value )))
    }

    /** Loads, parses, updates and writes back a file with one key/val pair per line. */
    def updateConfigFileMulti(absolutePath: String, kvMap: Map[String, String]) {
        val updatedMap =
            if( new File(absolutePath).exists() ) {
                val paramMap = loadConfigFile(absolutePath)
                paramMap ++ kvMap
            } else {
                kvMap
            }

        use(new FileWriter(absolutePath)) {
            fileWriter =>
                updatedMap.foreach(entry =>
                    fileWriter.append(entry._1 + "=" + entry._2 + "\n"))
        }
    }
}
