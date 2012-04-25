package scalatron.scalatron.impl

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */


import scalatron.botwar.{Config, PermanentConfig, BotWar}
import io.Source
import java.io._

import akka.util.duration._
import akka.pattern.ask
import akka.util.Timeout
import akka.dispatch.Await


import ScalatronUser.deleteRecursively
import ScalatronUser.loadConfigFile
import ScalatronUser.updateConfigFileMulti
import ScalatronUser.copyFile
import ScalatronUser.writeSourceFiles
import scalatron.scalatron.api.Scalatron.Constants._
import java.util.Date
import scalatron.scalatron.api.Scalatron
import scalatron.scalatron.api.Scalatron.{ScalatronException, BuildResult, Version, SourceFile}


case class ScalatronUser(name: String, scalatron: ScalatronImpl) extends Scalatron.User {
    require(scalatron.isUserNameValid(name))

    //----------------------------------------------------------------------------------------------
    // cached paths
    //----------------------------------------------------------------------------------------------

    val userDirectoryPath = scalatron.usersBaseDirectoryPath + "/" + name
    val userConfigFilePath = userDirectoryPath + "/" + Scalatron.Constants.ConfigFilename

    val sourceDirectoryPath = userDirectoryPath + "/" + UsersSourceDirectoryName
    val sourceFilePath = sourceDirectoryPath + "/" + UsersSourceFileName

    val versionBaseDirectoryPath = userDirectoryPath + "/" + UsersVersionsDirectoryName

    val outputDirectoryPath = userDirectoryPath + "/" + UsersOutputDirectoryName

    val localJarDirectoryPath = userDirectoryPath + "/" + UsersBotDirectoryName
    val localJarFilePath = localJarDirectoryPath + "/" + JarFilename

    val userPluginDirectoryPath = scalatron.pluginBaseDirectoryPath + "/" + name
    val publishedJarFilePath = userPluginDirectoryPath + "/" + JarFilename
    val backupJarFilePath = userPluginDirectoryPath + "/" + BackupJarFilename


    //----------------------------------------------------------------------------------------------
    // interface
    //----------------------------------------------------------------------------------------------

    def isAdministrator = ( name == AdminUserName )


    def delete() {
        if( isAdministrator ) {
            throw ScalatronException.Forbidden("deleting '" + Scalatron.Constants.AdminUserName + "' account is not permitted")
        } else {
            // caller must handle IOError exceptions
            deleteRecursively(userDirectoryPath, scalatron.verbose)
            deleteRecursively(userPluginDirectoryPath, scalatron.verbose)
        }
    }


    def updateAttributes(map: Map[String, String]) {
        // update the password setting in the user config file
        updateConfigFileMulti(userConfigFilePath, map)
        if( scalatron.verbose ) println("updated configuration attributes (" + map.keys.mkString(",") + ") in config file: " + userConfigFilePath)
    }


    def getAttributeMapOpt =
        try {
            Some(loadConfigFile(userConfigFilePath))
        } catch {
            case t: Throwable =>
                System.err.println("error: unable to load configuration attribute map for: " + name + ": " + t)
                None
        }




    //----------------------------------------------------------------------------------------------
    // source code & build management
    //----------------------------------------------------------------------------------------------

    def sourceFiles: Iterable[SourceFile] = {
        val sourceDirectory = new File(sourceDirectoryPath)
        if( !sourceDirectory.exists ) {
            System.err.println("error: user '" + name + "' has no /src directory at: " + sourceDirectoryPath)
            throw new IllegalStateException("no source directory found for user '" + name + "'")
        }

        // read whatever is on disk now
        val currentSourceFiles = sourceDirectory.listFiles()
        if(currentSourceFiles==null) {
            Iterable.empty
        } else {
            currentSourceFiles.filter(_.isFile).map(file => {
                val filename = file.getName
                val code = Source.fromFile(file).getLines().mkString("\n")
                Scalatron.SourceFile(filename, code)
            })
        }
    }


    def updateSourceFiles(sourceFiles: Iterable[SourceFile]) {
        // delete existing content
        deleteRecursively(sourceDirectoryPath, true)
        new File(sourceDirectoryPath).mkdirs()

        // write source files to disk
        writeSourceFiles(sourceDirectoryPath, sourceFiles, scalatron.verbose)
    }


    def buildSources(): BuildResult = {
        scalatron.compileActorRefOpt match {
            case None =>
                throw new IllegalStateException("compile actor not available")

            case Some(compileActorRef) =>
                // scan for source files to compile
                val sourceDirectory = new File(sourceDirectoryPath)
                if( !sourceDirectory.exists() ) {
                    throw new IllegalStateException("source directory does not exist: " + sourceDirectoryPath)
                }
                val sourceFiles = sourceDirectory.listFiles()
                if( sourceFiles == null || sourceFiles.isEmpty ) {
                    throw new IllegalStateException("source directory is empty: " + sourceDirectoryPath)
                }
                val sourceFileCollection = sourceFiles.map(_.getAbsolutePath)


                // create the output directory
                val outputDirectory = new File(outputDirectoryPath)
                if( outputDirectory.exists() ) {
                    // it should not exist before we start. If it does, we delete it
                    deleteRecursively(outputDirectoryPath, scalatron.verbose)
                }
                outputDirectory.mkdirs()


                // compile the source file, using an Akka Actor with a fixed time-out
                implicit val timeout = Timeout(60 seconds)
                val compileJob = CompileJob(sourceFileCollection, outputDirectoryPath)
                val future = compileActorRef ? compileJob
                val result = Await.result(future, timeout.duration)
                val compileResult = result.asInstanceOf[CompileResult]

                if( compileResult.compilationSuccessful ) {
                    // create the .jar directory, if necessary
                    val localJarDirectory = new File(localJarDirectoryPath)
                    if( !localJarDirectory.exists() ) {
                        if( !localJarDirectory.mkdirs() ) {
                            throw new IllegalStateException("failed to create directory for .jar file: " + localJarDirectoryPath)
                        }
                        if( scalatron.verbose ) println("created .jar directory for '" + name + "' at: " + localJarDirectoryPath)
                    }

                    // build the .jar archive file
                    JarBuilder(outputDirectoryPath, localJarFilePath, scalatron.verbose)

                    // delete the output directory - it is no longer needed
                    deleteRecursively(outputDirectoryPath, scalatron.verbose)
                }

                // transform compiler output into the BuildResult format expected by the Scalatron API
                val sourceDirectoryPrefix = sourceDirectoryPath + "/"
                BuildResult(
                    compileResult.compilationSuccessful,
                    compileResult.errorCount,
                    compileResult.warningCount,
                    compileResult.compilerMessages.map(msg => {
                        val absoluteSourceFilePath = msg.pos.source.path
                        val relativeSourceFilePath =
                            if(absoluteSourceFilePath.startsWith(sourceDirectoryPrefix)) {
                                absoluteSourceFilePath.drop(sourceDirectoryPrefix.length)
                            } else {
                                absoluteSourceFilePath
                            }
                        BuildResult.BuildMessage(
                            relativeSourceFilePath,
                            (msg.pos.line,msg.pos.column),
                            msg.msg,
                            msg.severity
                        )}
                    )
                )
        }
    }

    def unpublishedBotPluginPath = localJarFilePath




    //----------------------------------------------------------------------------------------------
    // version control & sample bots
    //----------------------------------------------------------------------------------------------

    def versions: Iterable[ScalatronVersion] = {
        val versionBaseDirectory = new File(versionBaseDirectoryPath)

        // enumerate the version directories below '.../versions'
        val versionDirectories = versionBaseDirectory.listFiles()
        if( versionDirectories == null || versionDirectories.isEmpty ) {
            // no versions there yet!
            Iterable.empty
        } else {
            versionDirectories.filter(_.isDirectory).map(dir => {
                val versionId = dir.getName.toInt
                val versionConfigFilename = dir.getAbsolutePath + "/" + ConfigFilename
                val paramMap = loadConfigFile(versionConfigFilename)
                val label = paramMap.getOrElse("label", "")
                val date = paramMap.getOrElse("date", "").toLong
                ScalatronVersion(versionId, label, date, this)
            })
        }
    }


    def version(id: Int): Option[Version] = {
        val versionDirectoryPath = versionBaseDirectoryPath + "/" + id
        val versionDirectory = new File(versionDirectoryPath)
        if( !versionDirectory.exists() ) {
            None
        } else {
            val versionConfigFilename = versionDirectoryPath + "/" + ConfigFilename
            val paramMap = loadConfigFile(versionConfigFilename)
            val label = paramMap.getOrElse("label", "")
            val date = paramMap.getOrElse("date", "").toLong
            Some(ScalatronVersion(id, label, date, this))
        }
    }


    def createVersion(label: String, sourceFiles: Iterable[SourceFile]): ScalatronVersion = {
        val versionBaseDirectory = new File(versionBaseDirectoryPath)
        if( !versionBaseDirectory.exists ) {
            if( !versionBaseDirectory.mkdirs() ) {
                System.err.println("error: failed to create user /versions directory for '" + name + "'")
                throw new IllegalStateException("could not create user /versions directory for '" + name + "'")
            }
            if( scalatron.verbose ) println("created user /versions directory '" + versionBaseDirectoryPath + "'")
        }

        // enumerate the existing versions
        val versionList = versions
        val versionId =
            if( versionList.isEmpty ) {
                0 // no versions there yet!
            } else {
                val maxExistingVersion = versionList.map(_.id).max
                maxExistingVersion + 1
            }

        // create a new version directory
        val versionDirectoryPath = versionBaseDirectoryPath + "/" + versionId
        if( !new File(versionDirectoryPath).mkdirs() ) {
            System.err.println("error: failed to create new version directory for '" + name + "': " + versionDirectoryPath)
            throw new IllegalStateException("could not create version directory for '" + name + "': " + versionDirectoryPath)
        }

        // create a new version config file
        val versionConfigFilePath = versionDirectoryPath + "/" + ConfigFilename
        val date = new Date().getTime
        val dateString = date.toString
        updateConfigFileMulti(versionConfigFilePath, Map("label" -> label, "date" -> dateString))

        // write the given source files into the version directory
        writeSourceFiles(versionDirectoryPath, sourceFiles, scalatron.verbose)

/*
        // 2012-04-16 was: copy the current contents of the source directory into the version directory
        val sourceDirectory = new File(sourceDirectoryPath)
        val sourceFiles = sourceDirectory.listFiles()
        if( sourceFiles == null || sourceFiles.isEmpty ) {
            // no source files there yet! -> nothing to do
        } else {
            sourceFiles.foreach(sourceFile => {
                val destPath = versionDirectoryPath + "/" + sourceFile.getName
                copyFile(sourceFile.getAbsolutePath, destPath)
                if( scalatron.verbose ) println("copied user source file for '" + name + "' to version: " + destPath)
            })
        }
*/

        ScalatronVersion(versionId, label, date, this)
    }


    //----------------------------------------------------------------------------------------------
    // tournament management
    //----------------------------------------------------------------------------------------------

    def publish() {
        // delete the old backup.jar file
        val backupJarFile = new File(backupJarFilePath)
        if( backupJarFile.exists ) {
            if( scalatron.verbose ) println("      deleting backup .jar file: " + backupJarFilePath)
            if( !backupJarFile.delete() ) throw new IllegalStateException("failed to delete backup .jar file at: " + backupJarFilePath)
        }

        // then move away the current .jar file
        val publishedJarFile = new File(publishedJarFilePath)
        if( publishedJarFile.exists ) {
            if( scalatron.verbose ) println("      backing up current .jar file: " + publishedJarFilePath + " => " + backupJarFilePath)
            if( !publishedJarFile.renameTo(backupJarFile) )
                throw new IllegalStateException("failed to rename .jar file to backup: " + backupJarFilePath)
        }


        // then copy the local .jar file
        val localJarFile = new File(localJarFilePath)
        if( localJarFile.exists ) {
            if( scalatron.verbose ) println("      activating new .jar file: " + localJarFilePath + " => " + publishedJarFilePath)

            val userPluginDirectory = new File(userPluginDirectoryPath)
            if( !userPluginDirectory.exists() ) {
                if( !userPluginDirectory.mkdirs() ) {
                    throw new IllegalStateException("failed to create user plug-in directory: " + userPluginDirectoryPath)
                }
                if( scalatron.verbose ) println("created user plug-in directory for '" + name + "' at: " + userPluginDirectoryPath)
            }
            try {
                copyFile(localJarFilePath, publishedJarFilePath)
            } catch {
                case t: Throwable =>
                    throw new IllegalStateException("failed to copy .jar file '" + localJarFilePath + "' to '" + publishedJarFile + "': " + t)
            }
        }
    }


    //----------------------------------------------------------------------------------------------
    // sandbox management
    //----------------------------------------------------------------------------------------------

    var nextSandboxId = 0

    def createSandbox(argMap: Map[String, String]) = {
        // determine the location of the user's local bot
        val localJarFile = new File(localJarFilePath)
        val plugins: Iterable[Plugin.External] =
            if( localJarFile.exists() ) {
                // attempt to load the plug-in
                val loadSpec = scalatron.game.pluginLoadSpec
                val eitherFactoryOrException =
                    Plugin.loadFrom(
                        localJarFile,
                        loadSpec.factoryClassPackage,
                        loadSpec.factoryClassName,
                        scalatron.verbose)

                eitherFactoryOrException match {
                    case Left(controlFunctionFactory) =>
                        val fileTime = localJarFile.lastModified()
                        val externalPlugin = Plugin.External(localJarDirectoryPath, localJarFilePath, fileTime, name, controlFunctionFactory)
                        println("plugin loaded for sandbox for user '" + name + "': " + externalPlugin)
                        Iterable(externalPlugin)
                    case Right(exception) =>
                        // plugin loading failed - we create the sandbox anyway
                        System.err.append("plugin load failure for sandbox for user '" + name + "': " + exception + "\n")
                        Iterable.empty
                }
            } else {
                // plugin does not exist - we create the sandbox anyway
                Iterable.empty
            }

        // TODO: allow the user to merge in other plug-ins, either from the tournament /bots directory,..
        // TODO: ..or from some repository, such as /tutorial/bots

        // determine the permanent configuration for the game - in particular, that it should run forever
        val permanentConfig = PermanentConfig(stepsPerRound = Int.MaxValue, internalPlugins = Iterable.empty)

        // determine the per-round configuration for the game
        val roundIndex = 0
        val gameConfig = Config.create(permanentConfig, roundIndex, plugins, argMap)

        val initialSimState = BotWar.startHeadless(plugins, permanentConfig, gameConfig)
        val sandboxId = nextSandboxId
        nextSandboxId += 1
        ScalatronSandbox(sandboxId, this, initialSimState)
    }
}


object ScalatronUser {
    /** Loads and parses a file with one key/val pair per line to Map[String,String].
      * Throws an exception if an error occurs. */
    def loadConfigFile(absolutePath: String) =
        Source
        .fromFile(absolutePath).getLines()
        .map(_.split('='))
        .map(a => if( a.length == 2 ) (a(0), a(1)) else (a(0), ""))
        .toMap


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

        val sourceFile = new FileWriter(absolutePath)
        updatedMap.foreach(entry => {
            sourceFile.append(entry._1 + "=" + entry._2 + "\n")
        })
        sourceFile.close()
    }

    /** Recursively deletes the given directory and all of its contents (CAUTION!)
      * @throws IllegalStateException if there is a problem deleting a file or directory
      */
    def deleteRecursively(directoryPath: String, verbose: Boolean) {
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

    /** code from http://stackoverflow.com/a/3028853
      * @throws IOError if there is a problem reading/writing the files
      */
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


    def loadSourceFiles(directoryPath: String): Iterable[SourceFile] = {
        val directory = new File(directoryPath)

        val sourceFiles = directory.listFiles()
        if( sourceFiles == null || sourceFiles.isEmpty ) {
            // no source files there!
            Iterable.empty
        } else {
            // read whatever is on disk now
            sourceFiles
            .filter(file => file.isFile && file.getName != ConfigFilename)
            .map(file => {
                val filename = file.getName
                val code = Source.fromFile(file).getLines().mkString("\n")
                Scalatron.SourceFile(filename, code)
            })
        }
    }

    /** Writes the given collection of source files into the given directory, which must exist
      * and should be empty. Throws an exception on IO errors. */
    def writeSourceFiles(targetDirectoryPath: String, sourceCode: Iterable[SourceFile], verbose: Boolean) {
        sourceCode.foreach(sf => {
            val path = targetDirectoryPath + "/" + sf.filename
            val sourceFile = new FileWriter(path)
            sourceFile.append(sf.code)
            sourceFile.close()
            if( verbose ) println("wrote source file: " + path)
        })
    }
}