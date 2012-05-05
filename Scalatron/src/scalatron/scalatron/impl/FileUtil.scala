package scalatron.scalatron.impl

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
      * @throws IllegalStateException if there is a problem deleting a file or directory
      */
    def deleteRecursively(directoryPath: String, verbose: Boolean = false) {
        val directory = new File(directoryPath)
        if( directory.exists ) {
            // caller handles exceptions
            if( verbose ) println("  deleting contents of directory at: " + directoryPath)
            if( directory.isDirectory ) {
                val filesInsideUserDir = directory.listFiles()
                if( filesInsideUserDir != null ) {
                    filesInsideUserDir.foreach(file => deleteRecursively(file.getAbsolutePath, verbose))
                }
                if( verbose ) println("  deleting directory: " + directoryPath)
                if( !directory.delete() )
                    throw new IllegalStateException("failed to delete directory at: " + directoryPath)
            } else {
                if( !directory.delete() )
                    throw new IllegalStateException("failed to delete file: " + directory.getAbsolutePath)
            }
        }
    }
}
