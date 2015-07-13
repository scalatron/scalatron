package scalatron.scalatron.impl

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */


import scala.tools.nsc.{Global, Settings}
import tools.nsc.reporters.StoreReporter
import scalatron.core.Scalatron
import scala.tools.nsc.util.{BatchSourceFile, Position}
import akka.actor.Actor
import java.util.Locale


/** Each compile actor holds a Scala compiler instance and uses it to process CompileJob messages
  * (containing the source files to compile and the output directory) and returns CompileResult messages
  * (containing the error and warning counts and the individual error messages).
  */
object CompileActor {
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
    private def detectScalaClassPaths(verbose: Boolean) : Iterable[String] = {
        val scalatronJarFilePath = ScalatronImpl.getClassPath(classOf[CompileActor])
        val scalaCompilerClassPath = ScalatronImpl.getClassPath(classOf[Global])
        val scalaLibraryClassPath = ScalatronImpl.getClassPath(classOf[List[_]])
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


    // constants
    object Constants {
        val ScalaCompilerInfo = 0       // see scala.tools.nsc.reporters.Reporter.INFO
        val ScalaCompilerWarning = 1    // see scala.tools.nsc.reporters.Reporter.WARNING
        val ScalaCompilerError = 2      // see scala.tools.nsc.reporters.Reporter.ERROR
    }
}

case class CompileActor(verbose: Boolean) extends Actor {
    var compilerGlobalOpt: Option[Global] = None

    override def preStart() {
        val detectedScalaClassPaths = CompileActor.detectScalaClassPaths(verbose)

        // called in the event of a compilation error
        def error(message: String) {println(message)}

        val settings = new Settings(error)
        detectedScalaClassPaths.foreach(settings.classpath.append)
        detectedScalaClassPaths.foreach(settings.bootclasspath.append)
        settings.deprecation.value = true // enable detailed deprecation warnings
        settings.unchecked.value = true // enable detailed unchecked warnings
        settings.explaintypes.value = true // explain type errors
        settings.verbose.value = false // verbose output -- too verbose even for our own 'verbose' setting

        val reporter = new StoreReporter()
        val compilerGlobal = new Global(settings, reporter)

        compilerGlobalOpt = Some(compilerGlobal)
    }


    def receive = {
        case CompileJob.FromDisk(sourceFilePathList, outputDirectoryPath) =>
            val compileResult = compileFromFiles(sourceFilePathList, outputDirectoryPath)
            sender ! compileResult

        case CompileJob.FromMemory(sourceFiles, outputDirectoryPath) =>
            val compileResult = compileFromMemory(sourceFiles, outputDirectoryPath)
            sender ! compileResult
    }

    /** Compiles a given collection of source files residing on disk into a given output directory.
      * @param sourceFilePathList the collection of source files to compile
      * @param outputDirectoryPath the output directory path where the class files should go
      */
    private def compileFromFiles(sourceFilePathList: Iterable[String], outputDirectoryPath: String): CompileResult = {
        // feed all source files to the Scala compiler, including *.java files
        // it will parse the Java files to resolve dependencies, but will not generate code for them
        val scalaCompilationResult = compileScalaCode(
            (run: Global#Run) => { run.compile(sourceFilePathList.toList) },
            outputDirectoryPath,
            sourceFilePathList.mkString(", ")
        )

        // Test: are there *.java sources among the source files?
        val javaFilePathList = sourceFilePathList.filter(_.endsWith(".java"))
        if(javaFilePathList.isEmpty) {
            // no *.java files, just *.scala
            scalaCompilationResult
        } else {
            // there are some *.java files => compile them and merge the error messages!
            try {
                import javax.tools._

                var javaCompilerErrors = 0
                var javaCompilerWarnings = 0
                val javaCompilerMessageBuilder = Vector.newBuilder[CompilerMessage]
                val diagnosticListener = new DiagnosticListener[Any] {
                    def report(diagnostic: Diagnostic[_]) {
                        val sourceFilePath = diagnostic.getSource match {
                            case t: Any => t.toString // TODO
                        }

                        import CompileActor.Constants._
                        val severity = diagnostic.getKind match {
                            case Diagnostic.Kind.ERROR => javaCompilerErrors += 1; ScalaCompilerError
                            case Diagnostic.Kind.WARNING => javaCompilerWarnings += 1; ScalaCompilerWarning
                            case Diagnostic.Kind.MANDATORY_WARNING => javaCompilerWarnings += 1; ScalaCompilerWarning
                            case _ => ScalaCompilerInfo
                        }

                        javaCompilerMessageBuilder +=
                            CompilerMessage(
                                CompilerMessagePosition(sourceFilePath, diagnostic.getLineNumber.toInt, diagnostic.getColumnNumber.toInt),
                                msg = diagnostic.getMessage(Locale.ENGLISH),
                                severity = severity
                            )
                    }
                }

                // Prepare the compilation options to be used during Java compilation
                // We are asking the compiler to place the output files under the /out folder.
                val compileOptions = scala.collection.JavaConversions.asJavaIterable(Iterable("-d", outputDirectoryPath))

                val compiler = ToolProvider.getSystemJavaCompiler
                if(compiler==null) throw new IllegalStateException("Java Compiler not available (on this platform)")

                val fileManager  = compiler.getStandardFileManager(diagnosticListener, null, null)
                val fileObjects = fileManager.getJavaFileObjectsFromStrings(scala.collection.JavaConversions.asJavaIterable(javaFilePathList))
                val task = compiler.getTask(null, fileManager, diagnosticListener, compileOptions, null, fileObjects)
                val javaCompilationSuccessful = task.call()

                try {
                    fileManager.close()
                } catch {
                    case t: Throwable =>
                        System.err.println("error: while closing Java compiler standard file manager: " + t)
                }

                val javaCompilationDuration = 0 // TODO: milliseconds
                val javaCompilerMessages = javaCompilerMessageBuilder.result()
                CompileResult(
                    javaCompilationSuccessful && scalaCompilationResult.compilationSuccessful,
                    javaCompilationDuration + scalaCompilationResult.duration,
                    scalaCompilationResult.errorCount,
                    scalaCompilationResult.warningCount,
                    scalaCompilationResult.compilerMessages ++ javaCompilerMessages
                )
            } catch {
                case t: Throwable =>
                    // Uh, something went wrong with the Java compilation - maybe not working on this system?
                System.err.println("error: exception during attempt to compile Java files: " + t)
                CompileResult(
                    false,
                    scalaCompilationResult.duration,
                    scalaCompilationResult.errorCount,
                    scalaCompilationResult.warningCount,
                    scalaCompilationResult.compilerMessages ++
                        Iterable(CompilerMessage(
                            CompilerMessagePosition(javaFilePathList.head, 0, 0),
                            msg = "exception during attempt to compile Java files: " + t,
                            severity = CompileActor.Constants.ScalaCompilerError
                        ))
                )
            }
        }
    }

    /** Compiles a given collection of source files residing in memory into a given output directory.
      * @param sourceFiles a collection of Scalatron SourceFile instances (filename + code) to compile
      * @return (successful, errorCount, warningCount, compilerMessages)
      */
    private def compileFromMemory(sourceFiles: Iterable[Scalatron.SourceFile], outputDirectoryPath: String): CompileResult = {
        /* Note: for the moment, compiling purely from memory does not work yet:
            java.lang.UnsupportedOperationException
                at scala.tools.nsc.io.AbstractFile.unsupported(AbstractFile.scala:249)
                at scala.tools.nsc.io.AbstractFile.unsupported(AbstractFile.scala:248)
                at scala.tools.nsc.io.VirtualFile.container(VirtualFile.scala:57)
                at scala.tools.nsc.Global$Run.rank$1(Global.scala:1121)
                at scala.tools.nsc.Global$Run$$anonfun$coreClassesFirst$1.apply(Global.scala:1131)
                at scala.tools.nsc.Global$Run$$anonfun$coreClassesFirst$1.apply(Global.scala:1131)
                at scala.math.Ordering$$anon$3.compare(Ordering.scala:81)
                at java.util.Arrays.mergeSort(Arrays.java:1270)
                at java.util.Arrays.sort(Arrays.java:1210)
                at scala.collection.SeqLike$class.sorted(SeqLike.scala:634)
                at scala.collection.immutable.List.sorted(List.scala:45)
                at scala.collection.SeqLike$class.sortBy(SeqLike.scala:613)
                at scala.collection.immutable.List.sortBy(List.scala:45)
                at scala.tools.nsc.Global$Run.coreClassesFirst(Global.scala:1131)
                at scala.tools.nsc.Global$Run.compileSources(Global.scala:916)
                at scalatron.scalatron.impl.CompileActor$$anonfun$scalatron$scalatron$impl$CompileActor$$compileFromMemory$1.apply(CompileActor.scala:143)
         */
        compileScalaCode(
            (run: Global#Run) => {
                val batchSourceFileList = sourceFiles.map(sf => {
                    new BatchSourceFile(sf.filename, sf.code.toCharArray)
                }).toList
                run.compileSources(batchSourceFileList)
            },
            outputDirectoryPath,
            sourceFiles.map(_.filename).mkString(", ")
        )
    }

    /** Uses the retained compiler global state to compile a list of source files.
      * 'compilerInvocation' is a closure that does something like this:
      * <code>(run: Global#Run) => { run.compileSources(sourceFileList)}</code> or
      * <code>(run: Global#Run) => { run.compile(sourceFilePathList)}</code>.
      * @param compilerInvocation a closure that feeds the source file list into the compiler run
      * @param outputDirectoryPath the output directory path where intermediate class files generated by the compiler should reside
      * @param runDescription a description for verbose output, e.g. a list of filenames
      * @return a CompileResult instance holding any compiler messages
      */
    private def compileScalaCode(compilerInvocation: (Global#Run) => Unit, outputDirectoryPath: String, runDescription: String): CompileResult = {
        compilerGlobalOpt match {
            case None =>
                throw new IllegalStateException("compiler not initialized")

            case Some(compilerGlobal) =>
                if( verbose ) println("    starting compilation (%s)...".format(runDescription))
                val startTime = System.currentTimeMillis

                // set the output directory (probably to a temporary directory somewhere)
                compilerGlobal.settings.outdir.value = outputDirectoryPath

                val run = new compilerGlobal.Run
                compilerInvocation(run)

                val endTime = System.currentTimeMillis
                val elapsed = (endTime - startTime).toInt
                if( verbose ) println("    ...compilation completed (" + elapsed + "ms)")

                val errorList =
                    compilerGlobal.reporter.asInstanceOf[StoreReporter].infos
                    .map(info => CompilerMessage(
                        CompilerMessagePosition(info.pos.source.path, info.pos.line, info.pos.column),
                        info.msg,
                        info.severity.id))
                val hasErrors = compilerGlobal.reporter.hasErrors
                val errorCount = compilerGlobal.reporter.ERROR.count
                val warningCount = compilerGlobal.reporter.WARNING.count

                compilerGlobal.reporter.reset() // clear all errors before next compilation

                CompileResult(!hasErrors, elapsed, errorCount, warningCount, errorList)
        }
    }
}


/** Records one error or warning that was generated by the compiler. */
case class CompilerMessagePosition(sourceFilePath: String, line: Int, column: Int)
case class CompilerMessage(pos: CompilerMessagePosition, msg: String, severity: Int)


/** Messages passed to and from the compile actor. */
sealed trait CompileActorMessage

trait CompileJob extends CompileActorMessage { def outputDirectoryPath: String }
object CompileJob {
    case class FromDisk(sourceFilePathList: Iterable[String], outputDirectoryPath: String) extends CompileJob
    case class FromMemory(sourceFiles: Iterable[Scalatron.SourceFile], outputDirectoryPath: String) extends CompileJob
}


case class CompileResult(
    compilationSuccessful: Boolean,
    duration: Int,  // milliseconds of actual build time (excluding build queue wait time)
    errorCount: Int,
    warningCount: Int,
    compilerMessages: Iterable[CompilerMessage])
    extends CompileActorMessage

