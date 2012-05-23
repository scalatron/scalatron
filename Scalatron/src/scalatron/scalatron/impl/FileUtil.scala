package scalatron.scalatron.impl

import scala.io.Source
import java.io.{FileInputStream, FileOutputStream, File}


object FileUtil
{
    /** Manages a closeable resource by applying the given closure to it, then closing the resource.
      * @param closable the closeable resource.
      * @param block the closure to execute with the resource.
      * @tparam T the resource type.
      */
    def use[T <: {def close()}](closable: T)(block: T => Unit) {
        try {
            block(closable)
        }
        finally {
            closable.close()
        }
    }


    /** code from http://stackoverflow.com/a/3028853
      * Using a later Java version (1.7?) would make this neater.
      * @throws IOError if there is a problem reading/writing the files
      */
    def copyFile(from: String, to: String) {
        use(new FileInputStream(from)) {
            in =>
                use(new FileOutputStream(to)) {
                    out =>
                        val buffer = new Array[Byte](1024)
                        Iterator.continually(in.read(buffer))
                        .takeWhile(_ != -1)
                        .foreach {out.write(buffer, 0, _)}
                }
        }
    }


    /** Recursively deletes the given directory and all of its contents (CAUTION!)
      * @param path the path of the directory to begin recursive deletion at
      * @param atThisLevel if true: the directory at `path` is deleted; if false: only its children
      * @param verbose if true, log verbosely to the console
      * @throws IllegalStateException if there is a problem deleting a file or directory
      */
    def deleteRecursively(path: String, atThisLevel: Boolean, verbose: Boolean) {
        val itemAtPath = new File(path)
        if(itemAtPath.exists) {
            // caller handles exceptions
            if(itemAtPath.isDirectory) {
                if(verbose) println("  deleting contents of directory at: " + path)
                val filesInsideUserDir = itemAtPath.listFiles()
                if(filesInsideUserDir != null) {
                    filesInsideUserDir.foreach(file => deleteRecursively(file.getAbsolutePath, true, verbose))
                }
                if(atThisLevel) {
                    if(verbose) println("  deleting directory: " + path)
                    if(!itemAtPath.delete()) {
                        System.err.println("error: failed to delete directory: %s".format(itemAtPath.getAbsolutePath))
                        throw new IllegalStateException("failed to delete directory at: " + itemAtPath.getAbsolutePath)
                    }
                }
            } else {
                if(atThisLevel) {
                    if(verbose) println("  deleting file at: " + path)
                    if(!itemAtPath.delete()) {
                        System.err.println("error: failed to delete file: %s".format(itemAtPath.getAbsolutePath))
                        throw new IllegalStateException("failed to delete file: " + itemAtPath.getAbsolutePath)
                    }
                }
            }
        }
    }


    /** Loads the contents from a text file using scala.io.Source.
      * Closes the source when it's done.
      * @param path the path of the file whose contents are to be read.
      * @return the contents of the file
      * @throws IOError if an IO error occurs while reading the file contents.
      */
    def loadTextFileContents(path: String): String = {
        val source = Source.fromFile(path)
        try {
            source.mkString
        } finally {
            source.close()
        }
    }
}
