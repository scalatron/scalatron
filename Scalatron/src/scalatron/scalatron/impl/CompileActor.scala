package scalatron.scalatron.impl

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */


import akka.actor.Actor
import scala.tools.nsc.{Global, Settings}
import tools.nsc.reporters.StoreReporter
import tools.nsc.util.Position
import scalatron.main.Main


/** We use Akka to manage the compiler queue. A single Scala compiler instance is held by
  * a CompileActor, which processes CompileJob messages (containing the source files to
  * compile and the output directory) and returns CompileResult messages (containing the
  * error and warning counts and the individual error messages).
  */
case class CompileActor(verbose: Boolean) extends Actor {
    var compilerGlobalOpt: Option[Global] = None

    override def preStart() {
        // before introducing onejar to package Scalatron.jar, we used this code:
        // val scalaCompilerClassPath = classOf[Global].getProtectionDomain.getCodeSource.getLocation.getPath
        // val scalaLibraryClassPath = classOf[List[_]].getProtectionDomain.getCodeSource.getLocation.getPath
        // if( verbose ) {
        //     println("Preparing Scala compiler...")
        //     println("  detected class path of Scala compiler to be: " + scalaCompilerClassPath)
        //     println("  detected class path of Scala library to be: " + scalaLibraryClassPath)
        // }
        // settings.classpath.append(scalaCompilerClassPath)
        // settings.classpath.append(scalaLibraryClassPath)

        // now that we introduced onejar to package Scalatron.jar, we need to used this code:
        // determine the path of the scala-compiler.jar, e.g. "/Applications/typesafe-stack/lib/scala-compiler.jar"
        val scalaCompilerClassPath = ScalatronImpl.detectScalatronJarFileDirectory(verbose)
        val scalaCompilerClassPathOld = classOf[Global].getProtectionDomain.getCodeSource.getLocation.getPath

        if( verbose ) {
            println("Preparing Scala compiler...")
            println("  detected class path of Scala compiler to be: " + scalaCompilerClassPath)
            println("  detected class path of Scala compiler B to be: " + scalaCompilerClassPathOld)
        }

        // called in the event of a compilation error
        def error(message: String) {println(message)}

        val settings = new Settings(error)
        settings.classpath.append(scalaCompilerClassPath)
        settings.classpath.append(scalaCompilerClassPathOld)
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

