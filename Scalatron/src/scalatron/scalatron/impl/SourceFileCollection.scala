package scalatron.scalatron.impl

import scalatron.core.Scalatron._
import FileUtil.use
import java.io.{FileWriter, File}

/** Utility methods for working with collections of source files. */
object SourceFileCollection
{
    /** Returns an initial source file collection that incorporates the given user name.
      * @param userName the name of the user to generate an initial source file collection for.
      * @return a non-empty collection of SourceFile objects
      */
    def initial(userName: String): SourceFileCollection =
        Iterable(SourceFile(
            "Bot.scala",
            "// this is the source code for your bot - have fun!\n" +
                "\n" +
                "class ControlFunctionFactory {\n" +
                "    def create = new Bot().respond _\n" +
                "}\n" +
                "\n" +
                "class Bot {\n" +
                "    def respond(input: String) = \"Status(text=" + userName + ")\"\n" +
                "}\n"
        ))


    def areEqual(as: SourceFileCollection, bs: SourceFileCollection, verbose: Boolean): Boolean =
        (as.size == bs.size) && // same number of files?
            as.forall(a => {
                bs.find(_.filename == a.filename) match {
                    case None =>
                        if(verbose) println("  SourceFileCollection.areEqual: file exists in as but not bs: " + a.filename)
                        false // file present in a, but not in b => different

                    case Some(b) =>
                        if(verbose) {
                            // 2012-05-04 temp debugging information
                            println("  SourceFileCollection.areEqual: file still exists: " + a.filename)
                            if(b.code != a.code) {
                                println("   file contents different")

                                if(b.code.length != a.code.length) {
                                    println("   file sizes are different: a %d vs b %d".format(a.code.length, b.code.length))
                                }

                                val aLineCount = a.code.lines.length
                                val bLineCount = b.code.lines.length
                                if(bLineCount > aLineCount) {
                                    println("   file b contains more lines: b %d vs a %d".format(bLineCount, aLineCount))
                                    val bLines = b.code.lines
                                    val aLines = a.code.lines
                                    val linesWithIndex = bLines.zip(aLines).zipWithIndex.map(l => "%04d: %60s %60s".format(l._2, l._1._1, l._1._2))
                                    println(linesWithIndex.mkString("\n"))
                                } else
                                if(bLineCount < aLineCount) {
                                    println("   file a contains more lines: b %d vs a %d".format(bLineCount, aLineCount))
                                } else {
                                    println("   files a and b contain same number of lines")
                                }
                            }
                        }

                        a.code == b.code // file present in a and b, so compare code
                }
            })

    /** Returns a collection of SourceFile objects representing source code files residing in the given directory.
      * Excludes directories and files that have the same name as the config files ("config.txt").
      * If the directory does not exist, is empty or does not contain valid files, an empty collection is returned.
      * @param directoryPath the directory to scan for source files.
      * @param verbose if true, information about the files read is logged to the console.
      * @return a collection of SourceFile objects; may be empty
      * @throws IOError on IO errors encountered while loading source file contents from disk
      */
    def loadFrom(directoryPath: String, verbose: Boolean = false): SourceFileCollection = {
        val directory = new File(directoryPath)
        if(!directory.exists) {
            System.err.println("warning: directory expected to contain source files does not exist: %s".format(directoryPath))
            Iterable.empty
        }
        else {
            val sourceFiles = directory.listFiles()
            if(sourceFiles == null || sourceFiles.isEmpty) {
                // no source files there!
                Iterable.empty
            } else {
                // read whatever is on disk now
                sourceFiles
                .filter(file => file.isFile && file.getName != Constants.ConfigFilename)
                .map(file => {
                    val filename = file.getName
                    val filePath = file.getAbsolutePath
                    val code = FileUtil.loadTextFileContents(filePath)
                    if(verbose) println("loaded source code from file: '%s'".format(filePath))
                    SourceFile(filename, code)
                })
            }
        }
    }


    /** Writes the given collection of source files into the given directory, which must exist
      * and should be empty. Throws an exception on IO errors.
      * @param directoryPath the directory to write the source files to.
      * @param sourceFileCollection the collection of source files to write to disk.
      * @param verbose if true, information about the written files is logged to the console.
      * @throws IOError on IO errors encountered while writing source file contents to disk.
      */
    def writeTo(directoryPath: String, sourceFileCollection: SourceFileCollection, verbose: Boolean = false) {
        sourceFileCollection.foreach(sf => {
            val path = directoryPath + "/" + sf.filename
            use(new FileWriter(path)) {_.append(sf.code)}
            if(verbose) println("wrote source file: " + path)
        })
    }
}
