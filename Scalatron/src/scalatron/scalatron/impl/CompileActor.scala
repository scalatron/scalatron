package scalatron.scalatron.impl

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */


import akka.actor.Actor
import scala.tools.nsc.{Global, Settings}
import tools.nsc.reporters.StoreReporter
import tools.nsc.util.Position


/** We use Akka to manage the compiler queue. A single Scala compiler instance is held by
  * a CompileActor, which processes CompileJob messages (containing the source files to
  * compile and the output directory) and returns CompileResult messages (containing the
  * error and warning counts and the individual error messages).
  */
case class CompileActor(verbose: Boolean) extends Actor {
    var compilerGlobalOpt: Option[Global] = None

    /** Returns a collection of one or more class paths to be appended to the classPath property of the
      * Scala compiler settings. It tries to deal with the following scenarios:
      * (a) running from unpackaged .class files:
      *     "/Users/dev/Scalatron/Scalatron/out/production/Scalatron/"
      * (b) running from a .jar produced by onejar:
      *     "file:/Users/dev/Scalatron/dist/bin/Scalatron.jar!/main/scalatron_2.9.1-0.1-SNAPSHOT.jar"
      * (c) running from a .jar produced by e.g. IDEA:
      *     "/Users/dev/Scalatron/bin/Scalatron.jar"
      *
      * @return a collection of class paths to append to the classPath property of the Scala compiler
      */
    private def detectScalaClassPaths : Iterable[String] = {
        val scalatronJarFilePath = classOf[CompileActor].getProtectionDomain.getCodeSource.getLocation.getPath
        val scalaCompilerClassPath = classOf[Global].getProtectionDomain.getCodeSource.getLocation.getPath
        val scalaLibraryClassPath = classOf[List[_]].getProtectionDomain.getCodeSource.getLocation.getPath
        if( verbose ) {
            println("  detected class path for Scalatron: " + scalatronJarFilePath)
            println("  detected class path for scala-compiler: " + scalaCompilerClassPath)
            println("  detected class path for scala-library: " + scalaLibraryClassPath)
        }

        // are we running from a .jar file produced by onejar?
        val classPaths =
            if(scalatronJarFilePath.toLowerCase.contains(".jar!/")) {
                // case (b) -- running from a .jar produced by onejar. We have a path like
                //             "file:/Users/dev/Scalatron/dist/bin/Scalatron.jar!/main/scalatron_2.9.1-0.1-SNAPSHOT.jar"
                //             TODO: deal with this case, if we decide to keep onejar
                System.err.println("warning: the way the .jar file is generated may result in the compile service failing")

                // the following hack for dealing with onejar was kindly invented by Charles O'Farrell (@charleso)
                // fortunately, we hopefully won't need it. Retained here for future reference.
                def extractJar(name: String) = {
                    var in = classOf[CompileActor].getResourceAsStream("/lib/" + name)
                    val file = java.io.File.createTempFile(name, ".jar")
                    if( verbose ) println("  detected class path of Scala %s to be: %s" format (name, file))
                    file.deleteOnExit()
                    file.getParentFile.mkdirs()
                    sys.process.BasicIO.transferFully(in, new java.io.FileOutputStream(file))
                    file.getAbsolutePath
                }

                Iterable(extractJar("scala-library.jar"), extractJar("scala-compiler.jar"))
            } else {
                // this is either a single .jar or a directory
                ScalatronImpl.cleanJarFilePath(scalatronJarFilePath) match {
                    case Left(filePath) =>
                        // case (c) -- running from a .jar produced by e.g. IDEA. We have a path like
                        //             "/Users/dev/Scalatron/bin/Scalatron.jar"
                        //             in theory, the scala-library.jar and scala-compiler.jar should be in here
                        if(scalaCompilerClassPath != scalatronJarFilePath)
                            println("info: unexpectedly, scala-compiler is not inside the Scalatron.jar: " + scalaCompilerClassPath + " vs. " + scalatronJarFilePath)
                        if(scalaLibraryClassPath != scalatronJarFilePath)
                            println("info: unexpectedly, scala-library is not inside the Scalatron.jar: " + scalaLibraryClassPath + " vs. " + scalatronJarFilePath)
                        Iterable(scalaCompilerClassPath, scalaLibraryClassPath)

                    case Right(dirPath) =>
                        // case (a): running from unpackaged .class files. We have a path like
                        //           "/Users/dev/Scalatron/Scalatron/out/production/Scalatron/"
                        //          in theory, the detected class paths for scala-library.jar and scala-compiler.jar
                        //          will now point into wherever they are installed on the system
                        Iterable(scalaCompilerClassPath, scalaLibraryClassPath)
                }
            }

        if(verbose) classPaths.foreach(cp => println("  adding class path to Scala compiler: " + cp))

        classPaths
    }

    override def preStart() {
        val detectedScalaClassPaths = detectScalaClassPaths

        // called in the event of a compilation error
        def error(message: String) {println(message)}

        val settings = new Settings(error)
        detectedScalaClassPaths.foreach(settings.classpath.append)
        settings.deprecation.value = true // enable detailed deprecation warnings
        settings.unchecked.value = true // enable detailed unchecked warnings
        settings.explaintypes.value = true // explain type errors
        settings.verbose.value = false // verbose output -- too verbose even for our own 'verbose' setting

        val reporter = new StoreReporter()
        val compilerGlobal = new Global(settings, reporter)

        compilerGlobalOpt = Some(compilerGlobal)
    }


    def receive = {
        case CompileJob(sourceFilePathList, outputDirectoryPath) =>
            val compileResult = compile(sourceFilePathList, outputDirectoryPath)
            sender ! compileResult
    }

    /** Compiles a given collection of source files into a given output directory.
      * @param sourceFilePathList the list of source files to compile
      * @param outputDirectoryPath the output directory path where the class files should go
      * @return (successful, errorCount, warningCount, compilerMessages)
      */
    def compile(sourceFilePathList: Iterable[String], outputDirectoryPath: String): CompileResult = {
        compilerGlobalOpt match {
            case None =>
                throw new IllegalStateException("compiler not initialized")

            case Some(compilerGlobal) =>
                if( verbose ) println("    starting compilation of {" + sourceFilePathList.mkString(", ") + "}...")
                val startTime = System.currentTimeMillis

                compilerGlobal.settings.outdir.value = outputDirectoryPath

                val run = new compilerGlobal.Run
                run.compile(sourceFilePathList.toList)

                val endTime = System.currentTimeMillis
                val elapsed = endTime - startTime
                if( verbose ) println("    ...compilation completed (" + elapsed + "ms)")

                val errorList = compilerGlobal.reporter.asInstanceOf[StoreReporter].infos.map(info => CompilerMessage(info.pos, info.msg, info.severity.id))
                val hasErrors = compilerGlobal.reporter.hasErrors
                val errorCount = compilerGlobal.reporter.ERROR.count
                val warningCount = compilerGlobal.reporter.WARNING.count

                compilerGlobal.reporter.reset() // clear all errors before next compilation


                // result: true if no errors
                CompileResult(!hasErrors, errorCount, warningCount, errorList)
        }
    }
}


/** Records one error or warning that was generated by the compiler. */
case class CompilerMessage(pos: Position, msg: String, severity: Int)


/** Messages passed to and from the compile actor. */
sealed trait CompileActorMessage

case class CompileJob(
    sourceFilePathList: Iterable[String],
    outputDirectoryPath: String)
    extends CompileActorMessage

case class CompileResult(
    compilationSuccessful: Boolean,
    errorCount: Int,
    warningCount: Int,
    compilerMessages: Iterable[CompilerMessage])
    extends CompileActorMessage

