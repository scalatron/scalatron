package org.fusesource.scalamd

import io.Source
import java.io.{FileOutputStream, FileInputStream, FileWriter, File}


/** Based on: http://scalamd.fusesource.org/maven/1.0/
  *
  * If a directory is passed, it assumes the following layout on disk (inspired by Jekyll):
  *  /directory
  *     *.md            -- the markdown files
  *     /_layouts       -- optional: template files
  *     /images         -- optional: image directory
  *     /stylesheets    -- optional: stylesheet directory
  *     /javascripts    -- optional: JavaScript directory
  */
object Main {
    val LiquidMarker = "---"
    val ContentMarker = "{{ content }}"
    val TitleMarker = "{{ page.title }}"
    val SubTitleMarker = "{{ page.subtitle }}"

    val LayoutDirectoryName = "_layouts"

    val CopyableExtensions = Iterable(".css", ".html", ".js", ".scala", ".java", ".png", ".gif", ".jpg")


    def main(args: Array[String]) {

        val (verbose: Boolean, inPath: String, outPath: String) = args.length match {
            case 1 =>
                println("Syntax: java -jar ScalaMarkdown.jar [-verbose] inputPath outputPath")
                println("where:")
                println("  - 'inputPath' is either a file or a directory containing markdown")
                println("  - 'outputPath' is a directory to receive HTML")
                System.exit(-1)

            case 2 =>
                (false, args(0), args(1))

            case 3 =>
                if( args(0) == "-verbose" ) {
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
            val inputBaseDirectoryPath = inFile.getAbsolutePath
            processDirectory(inputBaseDirectoryPath, outputDirectoryPath, inputBaseDirectoryPath, outputDirectoryPath, verbose)
        } else {
            // process file
            val inputBaseDirectoryPath = inFile.getParentFile.getAbsolutePath
            processFile(inFile.getAbsolutePath, outputDirectoryPath, inputBaseDirectoryPath, outputDirectoryPath, verbose)
        }

        if(verbose) println("...done" )
    }


    def loadOptionalTemplateFile(templateFilePath: String) : Option[String] =
        if(new File(templateFilePath).exists) {
            val templateHtml = Source.fromFile(templateFilePath).mkString
            Some(templateHtml)
        } else {
            None
        }


    /** Processes a directory full of files and sub-directories.
      * @param inputDirectoryPath the input directory to read from, may be a subdirectory during recursion
      * @param outputDirectoryPath the corresponding output directory to write to, may be a subdirectory during recursion
      * @param inputBaseDirectoryPath the topmost input directory, stays constant during recursion
      * @param outputBaseDirectoryPath the topmost output directory, stays constant during recursion
      * @param verbose if true, log verbose output
      */
    def processDirectory(
        inputDirectoryPath: String,
        outputDirectoryPath: String,
        inputBaseDirectoryPath: String,
        outputBaseDirectoryPath: String,
        verbose: Boolean)
    {
        val inputDirectory = new File(inputDirectoryPath)
        if(inputDirectory.getName.startsWith("_")) {
            // skip
        } else {
            val fileList = inputDirectory.listFiles()
            if(fileList == null || fileList.isEmpty) {
                System.err.println("warning: no files to process in: " + inputDirectoryPath)
            } else {
                val (directoryCollection, fileCollection) = fileList.partition(_.isDirectory)
                directoryCollection.foreach(dir => processDirectory(dir.getAbsolutePath, outputDirectoryPath + "/" + dir.getName, inputBaseDirectoryPath, outputBaseDirectoryPath, verbose))
                fileCollection.foreach(file => processFile(file.getAbsolutePath, outputDirectoryPath, inputBaseDirectoryPath, outputBaseDirectoryPath, verbose))
            }
        }
    }


    /** Processes a single file, either copying it or markdown-converting it.
      * @param inputFilePath the path of the input file to read from, may be in a subdirectory during recursion
      * @param outputDirectoryPath the corresponding output directory to write to, may be a subdirectory during recursion
      * @param inputBaseDirectoryPath the topmost input directory, stays constant during recursion
      * @param outputBaseDirectoryPath the topmost output directory, stays constant during recursion
      * @param verbose if true, log verbose output
      */
    def processFile(
        inputFilePath: String,
        outputDirectoryPath: String,
        inputBaseDirectoryPath: String,
        outputBaseDirectoryPath: String,
        verbose: Boolean) {
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
                convertFileFromMarkdownToHtml(inputFilePath, outputFilePath, inputBaseDirectoryPath, outputBaseDirectoryPath)
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
        if(CopyableExtensions.find(inputFilename.endsWith).isDefined) {
            inputFilename
        } else {
            ""
        }


    /** Convert a single file from Markdown to HTML.
      * @param inputFilePath the path of the input file to read from
      * @param outputFilePath the corresponding output file to write to
      * @param inputBaseDirectoryPath the topmost input directory, stays constant during recursion
      * @param outputBaseDirectoryPath the topmost output directory, stays constant during recursion
      */
    def convertFileFromMarkdownToHtml(
        inputFilePath: String,
        outputFilePath: String,
        inputBaseDirectoryPath: String,
        outputBaseDirectoryPath: String)
    {
        // load
        val inputLines = Source.fromFile(inputFilePath).getLines().toIterable // load lines

        // deal with Liquid-like headers
        val (liquidParamMap, inputText) : (Map[String,String], String) =
            if(inputLines.head.startsWith(LiquidMarker)) {
                var linesTaken = 0
                val map =
                    inputLines
                    .tail   // drop starting "---"
                    .takeWhile(line => { linesTaken += 1; (line.contains(':') && !line.startsWith(LiquidMarker)) })       // between Liquid markers ("---")
                    .map(line => line.split(':'))                                                   // "key: value" => Array("key", " value")
                    .map(array => if(array.length==2) Some((array(0).trim,array(1).trim)) else None)// Array => Option[Tuple2]
                    .flatten                                                                        // remove None values
                    .toMap
                val remainingLines = inputLines.drop(linesTaken + 1) // start marker, lines, end marker
                (map, remainingLines.mkString("\n"))
            } else {
                (Map.empty, inputLines.mkString("\n"))
            }

        val convertedHtml = new MarkdownText(inputText).toHtml() // convert

        /*
                // TODO: check for the presence of a template file
                val templateFilePath = inputFilePath.getAbsolutePath + "/" + HtmlTemplateFileName
                val templateHtmlOpt = loadOptionalTemplateFile(templateFilePath)
        */


        val templateProcessedHtml =
            if(liquidParamMap.isEmpty) {
                convertedHtml
            } else {
                val pageTitle =
                    liquidParamMap.get("title") match {
                        case Some(title) => title
                        case None => new File(inputFilePath).getName.takeWhile(_ != '.')
                    }

                val pageSubTitle =
                    liquidParamMap.get("subtitle") match {
                        case Some(subtitle) => subtitle
                        case None => ""
                    }

                val templateFilenameOpt = liquidParamMap.get("layout").map( _ + ".html")

                val templateContentOpt = templateFilenameOpt match {
                    case Some(templateFilename) =>
                        val templateFilePath = inputBaseDirectoryPath + "/" + LayoutDirectoryName + "/" + templateFilename
                        try {
                            Some(Source.fromFile(templateFilePath).mkString)
                        } catch {
                            case t: Throwable =>
                                System.err.println("error: template file not found: %s".format(templateFilePath))
                                None
                        }
                    case None => None
                }

                val templatedContent = templateContentOpt match {
                    case Some(templateContent) => templateContent.replace(ContentMarker, convertedHtml)
                    case None => convertedHtml
                }

                templatedContent
                .replace(TitleMarker, pageTitle)
                .replace(SubTitleMarker, pageSubTitle)
            }

        use(new FileWriter(outputFilePath)) { _.append(templateProcessedHtml) }
    }

    // code from http://stackoverflow.com/a/3028853
    def use[T <: {def close()}](closable: T)(block: T => Unit) {
        try {
            block(closable)
        }
        finally {
            closable.close()
        }
    }


    def copyFile(from: String, to: String) {
        new File(to).getParentFile.mkdirs()
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