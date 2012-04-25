package org.fusesource.scalamd

import io.Source
import java.io.{FileOutputStream, FileInputStream, FileWriter, File}


/** http://scalamd.fusesource.org/maven/1.0/
 *
 */
object Main {
    def main(args: Array[String]) {

        val (verbose: Boolean, inPath: String, outPath: String) = args.length match {
            case 1 =>
                println("Syntax: java -jar ScalaMarkdown.jar [-verbose] inputPath outputPath")
                println("where inputPath is either a file or a directory and outputPath is a directory")
                System.exit(-1)

            case 2 =>
                (false, args(0), args(1))

            case 3 =>
                if( args(0) == "verbose" ) {
                    (true, args(1), args(2))
                } else {
                    System.err.println("unrecognized option: %s".format(args(0)))
                    System.exit(-1)
                }

            case _ =>
                System.err.println("invalid number of options.")
                println("Syntax: java -jar ScalaMarkdown.jar [-verbose] inputPath outputPath")
                println("where inputPath is either a file or a directory and outputPath is a directory")
                System.exit(-1)
        }

        val outFile = new File(outPath)
        if(!outFile.exists()) {
            if(!outFile.mkdirs()) {
                System.err.println("failed to create output path: " + outPath)
                System.exit(-1)
            }
        } else {
            if(!outFile.isDirectory) {
                System.err.println("output path is not a directory")
                System.exit(-1)
            }
        }
        val outputDirectoryPath = outFile.getAbsolutePath

        val inFile = new File(inPath)
        if(inFile.isDirectory) {
            // process directory
            val fileList = inFile.listFiles()
            if(fileList == null || fileList.isEmpty) {
                System.err.println("No files to process in: " + inPath)
            } else {
                fileList.foreach(file => processFile(file.getAbsolutePath, outputDirectoryPath, verbose))
            }
        } else {
            // process file
            processFile(inFile.getAbsolutePath, outputDirectoryPath, verbose)
        }

        if(verbose) println("...done" )
    }



    def processFile(inputFilePath: String, outputDirectoryPath: String, verbose: Boolean) {
        val inputFile = new File(inputFilePath)
        val inputFilename = inputFile.getName

        val outputFilename = computeOutputFilename(inputFilename)
        if(outputFilename.isEmpty) {
            if(verbose) println("Skipping " + inputFilename )
        } else {
            val outputFilePath = outputDirectoryPath + "/" + outputFilename
            if(outputFilename == inputFilename) {
                if(verbose) println("Copying " + inputFilename )
                copyFile(inputFilePath, outputFilePath)
            } else {
                if(verbose) println("Coverting " + inputFilename )
                convertFileFromMarkdownToHtml(inputFilePath, outputFilePath)
            }
        }
    }


    /** Given an input filename, computes an output filename. Returns:
      * - a valid output filename
      *     - which is different from the original if markdown processing is required
      *     - which is the same as the original if NO markdown processing is required
      * - an empty string if the file does NOT need to be processed.
      */
    def computeOutputFilename(inputFilename: String) =
        if(inputFilename.endsWith(".md")) {
            inputFilename.dropRight(3) + ".html"
        } else
        if(inputFilename.endsWith(".markdown")) {
            inputFilename.dropRight(9) + ".html"
        } else
        if(inputFilename.endsWith(".css") || inputFilename.endsWith(".scala")) {
            inputFilename
        } else {
            ""
        }


    def convertFileFromMarkdownToHtml(inPath: String, outPath: String) {
        val inputText = Source.fromFile(inPath).getLines().mkString("\n")
        val outputHtml = new MarkdownText(inputText).toHtml()
        val fw = new FileWriter(outPath)
        try {
            fw.append(outputHtml)
        } finally {
            fw.close()
        }
    }

    // code from http://stackoverflow.com/a/3028853
    def copyFile(from: String, to: String) {
        def use[T <: {def close()}](closable: T)(block: T => Unit) {
            try {
                block(closable)
            }
            finally {
                closable.close()
            }
        }

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
}