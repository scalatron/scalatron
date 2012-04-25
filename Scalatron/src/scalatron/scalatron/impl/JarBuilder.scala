package scalatron.scalatron.impl

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */


import java.util.jar.{JarEntry, JarOutputStream}
import java.io.{FileOutputStream, FileInputStream, BufferedInputStream, File}


/** Utility class for building a .jar file from a directory of .class files.
  * Ported to Scala from:
  * http://stackoverflow.com/questions/1281229/how-to-use-jaroutputstream-to-create-a-jar-file
  *
  * CBB: turn this into a function that uses more of a classical Builder wrapping a JarOutputStream
  * to construct the .jar file.
  */
object JarBuilder {
    /** Given an input directory containing other directories which may include Java .class
      * files and a .jar output file name, this routine builds a jar file at the given location
      * by adding all the .class files into it and attaching a manifest.
      * The paths of the .class files within the .jar will be relative to the given input
      * directory path.
      * Example:
      *     given the inputDirectoryPath:
      *         /Scalatron/scalatron-0.9.3/Scalatron/bots/CompileTest/out
      *     containing the files
      *         /Scalatron/scalatron-0.9.3/Scalatron/bots/CompileTest/out/Bot.class
      *         /Scalatron/scalatron-0.9.3/Scalatron/bots/CompileTest/out/ControlFunctionFactory$$anonfun$create$1.class
      *         /Scalatron/scalatron-0.9.3/Scalatron/bots/CompileTest/out/ControlFunctionFactory.class
      *         /Scalatron/scalatron-0.9.3/Scalatron/bots/CompileTest/out/util/XY.class
      *     the function will generate a .jar file containing:
      *         Bot.class
      *         ControlFunctionFactory$$anonfun$create$1.class
      *         ControlFunctionFactory.class
      *         util/XY.class
      *
      * @param inputDirectoryPath the input directory from which to (recursively) read .class files
      * @param jarFilePath the output file name of the .jar to generate
      * @param verbose if true, prints verbose debug messages
      * @return true if the .jar file was successfully created, false if not
      */
    def apply(inputDirectoryPath: String, jarFilePath: String, verbose: Boolean): Boolean = {
        // ascertain that the target directory exists
        val rootDirectory = new File(inputDirectoryPath)
        if( !rootDirectory.exists ) {
            println("error: .class directory does not exist: " + inputDirectoryPath)
            false
        }
        else
        if( !rootDirectory.isDirectory ) {
            println("error: .class path is not a directory: " + inputDirectoryPath)
            false
        }
        else {
            val rootPath = rootDirectory.getAbsolutePath
            val canonicalRootPath = canonizeSlashesInPath(rootPath, true)

            val fileOutputStream =
                try {
                    new FileOutputStream(jarFilePath)
                } catch {
                    case e: Throwable =>
                        println("error: failed to open file output stream on: " + jarFilePath + ": " + e)
                        return false
                }

            val jarOutputStream =
                try {
                    val manifest = new java.util.jar.Manifest()
                    manifest.getMainAttributes.put(java.util.jar.Attributes.Name.MANIFEST_VERSION, "1.0")
                    new JarOutputStream(fileOutputStream, manifest)
                } catch {
                    case e: Throwable =>
                        fileOutputStream.close()
                        println("error: failed to open jar output stream: " + e)
                        return false
                }

            try {
                if( verbose ) println("      building jar file '" + jarFilePath + "' from class files in '" + inputDirectoryPath + "'...")

                // recursively add the files in the input directory
                add(rootDirectory, canonicalRootPath, jarOutputStream, verbose)

                if( verbose ) println("      ...jar file assembly completed.")
            } catch {
                case e: Throwable =>
                    System.err.println("error: failed to build jar file: " + jarFilePath + ": " + e)
                    return false
            } finally {
                jarOutputStream.close() // closes JarOutputStream and FileOutputStream
            }

            true
        }
    }


    /** Given a path that may use Windows (backward) slashes and that may or may not contain a
      * terminating slash, generates a path using Unix (forward) slashes which, if it represents
      * a directory, contains a terminating (forward) slash. This is required for the paths
      * inside .jar (and .zip) files based on their specification.
      */
    private def canonizeSlashesInPath(path: String, asDirectory: Boolean) = {
        val slashesFixed = path.replace("\\", "/")
        if( asDirectory ) {
            if( slashesFixed.endsWith("/") ) slashesFixed else slashesFixed + "/"
        } else {
            slashesFixed
        }
    }


    /** Recursively add a directory or file to the given jar output stream. */
    private def add(source: File, relativeTo: String, jarOutputStream: JarOutputStream, verbose: Boolean) {
        // .jar (and .zip) file specification calls for unix-style slashes. Convert if on Windows:
        val name = source.getPath.replace("\\", "/")
        val relativeName = name.drop(relativeTo.length)

        if( source.isDirectory ) {
            if( !relativeName.isEmpty ) {
                val adjustedRelativeName = if( relativeName.endsWith("/") ) relativeName else relativeName + "/"
                if( verbose ) println("        adding directory: " + adjustedRelativeName)
                val entry = new JarEntry(adjustedRelativeName)
                entry.setTime(source.lastModified())
                jarOutputStream.putNextEntry(entry)
                jarOutputStream.closeEntry()
            }

            val nestedFiles = source.listFiles()
            if( nestedFiles != null ) {
                nestedFiles.foreach(nestedFile => add(nestedFile, relativeTo, jarOutputStream, verbose))
            }
        }
        else {
            val relativeName = name.drop(relativeTo.length)
            if( verbose ) println("        adding file: " + relativeName)

            val entry = new JarEntry(relativeName)
            entry.setTime(source.lastModified())
            jarOutputStream.putNextEntry(entry)

            var in: BufferedInputStream = null
            try {
                in = new BufferedInputStream(new FileInputStream(source))

                val buffer = Array.ofDim[Byte](1024)
                var looping = true
                while( looping ) {
                    val count = in.read(buffer)
                    if( count == -1 )
                        looping = false
                    else
                        jarOutputStream.write(buffer, 0, count)
                }
            } finally {
                if( in != null ) in.close()
            }

            jarOutputStream.closeEntry()
        }
    }

}