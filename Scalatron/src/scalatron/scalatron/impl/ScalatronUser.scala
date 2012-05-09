package scalatron.scalatron.impl

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */


import scalatron.botwar.{Config, PermanentConfig, BotWar}
import java.io._

import akka.util.duration._
import akka.pattern.ask
import akka.util.Timeout
import akka.dispatch.Await


import scalatron.util.FileUtil
import FileUtil.deleteRecursively
import FileUtil.copyFile
import ConfigFile.loadConfigFile
import ConfigFile.updateConfigFileMulti
import ScalatronUser.buildSourceFilesIntoJar
import scalatron.scalatron.api.Scalatron.Constants._
import java.util.Date
import scalatron.scalatron.api.Scalatron
import scalatron.scalatron.api.Scalatron._


case class ScalatronUser(name: String, scalatron: ScalatronImpl) extends Scalatron.User {
    require(scalatron.isUserNameValid(name))

    //----------------------------------------------------------------------------------------------
    // cached paths
    //----------------------------------------------------------------------------------------------

    val userDirectoryPath = scalatron.usersBaseDirectoryPath + "/" + name
    val userConfigFilePath = userDirectoryPath + "/" + Scalatron.Constants.ConfigFilename

    val sourceDirectoryPath = userDirectoryPath + "/" + UsersSourceDirectoryName
    val sourceFilePath = sourceDirectoryPath + "/" + UsersSourceFileName
    val patchedSourceDirectoryPath = userDirectoryPath + "/" + UsersPatchedSourceDirectoryName

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

    def sourceFiles = {
        val sourceFileCollection = SourceFileCollection.loadFrom(sourceDirectoryPath)
        if(sourceFileCollection.isEmpty) {
            System.err.println("error: user '" + name + "' has no source files in: '%s'".format(sourceDirectoryPath))
            throw new IllegalStateException("no source files found for user '%s'".format(name))
        }
        sourceFileCollection
    }


    def updateSourceFiles(transientSourceFiles: SourceFileCollection) {
        // delete existing content
        deleteRecursively(sourceDirectoryPath, scalatron.verbose)
        new File(sourceDirectoryPath).mkdirs()

        // write source files to disk
        SourceFileCollection.writeTo(sourceDirectoryPath, transientSourceFiles, scalatron.verbose)
    }


    def buildSourceFiles(transientSourceFiles: SourceFileCollection): BuildResult = {
        /** The compile service recycles its compiler state to accelerate compilation. This results in namespace
          * collisions if multiple users use the same fully qualified package names for their classes and submit
          * those files for compilation. So in order to make the compiler instance recycling feasible, we need a bit
          * of a hack: each user's classes must reside in their own namespace, which we can realize by using a
          * package statement with a unique package name for each source code file. The user name provides a
          * symbol that is guaranteed to be unique in this context, so we use that as the package name - verbatim.
          * The plug-in loader knows about this hack, too, and tries to load a fully qualified class name based on
          * the user name first. Case is significant.
          */
        val gameSpecificPackagePath = scalatron.game.pluginLoadSpec.gameSpecificPackagePath
        val packagePath = gameSpecificPackagePath + "." + name
        val packageStatementWithNewline = "package " + packagePath + "\n"
        val patchedSourceFiles = transientSourceFiles.map(sf => {
            val localCode = sf.code
            // CBB: warn the user about conflicts if she embeds her own package name
            // but if(localCode.contains("package")) ... is too dumb
            val patchedCode = packageStatementWithNewline + localCode
            if(scalatron.verbose) println("  patching '%s' with 'package %s'".format(sf.filename, packagePath))
            SourceFile(sf.filename, patchedCode)
        })
        val messageLineAdjustment = -1

        // OK, in theory, this should work:
        //   val compileJob = CompileJob.FromMemory(patchedSourceFiles, outputDirectoryPath)
        // but unfortunately, we're doing something wrong in setting up the virtual files to compile from,
        // so the compiler chokes while trying to sort its dependent files by rank, or something like that.

        // so, as a temporary work-around, we create temp files on disk:
        // TODO: this code shiould probably exist within writeSourceFiles() - refactor!
        val patchedSourceDirectory = new File(patchedSourceDirectoryPath)
        if(!patchedSourceDirectory.exists) {
            if(!patchedSourceDirectory.mkdirs()) {
                System.err.println("error: cannot create patched source directory at: " + patchedSourceDirectory)
                throw new IllegalStateException("error: cannot create patched source directory at: " + patchedSourceDirectory)
            }
        }
        SourceFileCollection.writeTo(patchedSourceDirectoryPath, patchedSourceFiles, scalatron.verbose)

        val patchesSourceFilePaths = patchedSourceFiles.map(psf => patchedSourceDirectoryPath + "/" + psf.filename)
        val compileJob = CompileJob.FromDisk(patchesSourceFilePaths, outputDirectoryPath)


        // compiles source -> out, then zips out -> jar, then deletes out & returns BuildResult
        buildSourceFilesIntoJar(
            scalatron,
            name,
            patchedSourceDirectoryPath,
            compileJob,
            localJarDirectoryPath,
            localJarFilePath,
            messageLineAdjustment
        )
    }



    def buildSources(): BuildResult = {
        val localSourceFiles = sourceFiles        // fetch the source files from disk
        buildSourceFiles(localSourceFiles)
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
            versionDirectories
            .filter(_.isDirectory)
            .map(dir => {
                val versionId = dir.getName.toInt
                val versionConfigFilename = dir.getAbsolutePath + "/" + ConfigFilename
                val paramMap = loadConfigFile(versionConfigFilename)
                val label = paramMap.getOrElse("label", "")
                val date = paramMap.getOrElse("date", "").toLong
                ScalatronVersion(versionId, label, date, this)
            })
            .toArray
            .sortBy(_.id)
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


    def createVersion(label: String, sourceFiles: SourceFileCollection): ScalatronVersion = {
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
        SourceFileCollection.writeTo(versionDirectoryPath, sourceFiles, scalatron.verbose)

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


    def createBackupVersion(policy: VersionPolicy, label: String, updatedSourceFiles: SourceFileCollection) =
        policy match {
            case VersionPolicy.IfDifferent =>
                val sourceFilesCurrentlyInWorkspace = sourceFiles

                val workspaceEqualsUpdated = SourceFileCollection.areEqual(sourceFilesCurrentlyInWorkspace, updatedSourceFiles, scalatron.verbose)

                val workspaceEqualsLatestVersion = latestVersion match {
                    case None =>
                        false
                    case Some(latestVersionInHistory) =>
                        val sourceFilesOfLatestVersion = latestVersionInHistory.sourceFiles
                        SourceFileCollection.areEqual(sourceFilesCurrentlyInWorkspace, sourceFilesOfLatestVersion, scalatron.verbose)
                }

                if(workspaceEqualsUpdated || workspaceEqualsLatestVersion) {
                    if(scalatron.verbose) println("VersionPolicy.IfDifferent, files are unchanged => not creating backup version")
                    None
                } else {
                    if(scalatron.verbose) println("VersionPolicy.IfDifferent, files are different => creating backup version")
                    Some(createVersion(label, sourceFilesCurrentlyInWorkspace))    // backup old files as a version
                }

            case VersionPolicy.Always =>
                if(scalatron.verbose) println("VersionPolicy.Always => creating backup version")
                val oldSourceFiles = sourceFiles
                Some(createVersion(label, oldSourceFiles))       // backup old files as a version

            case VersionPolicy.Never =>
                if(scalatron.verbose) println("VersionPolicy.Never => not creating backup version")
                None // OK - nothing to back up
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
                        name,
                        loadSpec.gameSpecificPackagePath,
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

        val initialSimState = BotWar.startHeadless(plugins, permanentConfig, gameConfig)(scalatron.executionContextForUntrustedCode)
        val sandboxId = nextSandboxId
        nextSandboxId += 1
        ScalatronSandbox(sandboxId, this, initialSimState)
    }
}


object ScalatronUser {
    /** Builds a .jar file using the given compile job (either from disk or from in-memory files), using the output
      * directory stored in the compile job for temporarily created .class files, storing the resulting .jar file
      * into the given path. The first compilation in a newly started server may take rather long
      * (parsing scala-language), so a longish timeout (60 seconds) is recommended for that case.
      * @param scalatron reference to the Scalatron instance (for verbosity and access to CompileActor)
      * @param sourceDirectoryPath the source directory path - no trailing slash!; used to make error messages relative
      * @param compileJob the collection of source file paths to compile
      * @param jarDirectoryPath the path of the directory where the zipped-up .jar file should reside; created if it does not exist
      * @param jarFilePath the path where the zipped-up .jar file should reside
      * @param messageLineAdjustment how much to add/remove from line number error message (because of patched-in package statement)
      * @param timeoutInSeconds the timeout for the compile job in seconds
      * @return a build result containing any compiler error messages that may have been generated
      * @throws IOError when building the .jar file encounters problems
      */
    private def buildSourceFilesIntoJar(
        scalatron: ScalatronImpl,
        userName: String,
        sourceDirectoryPath: String,
        compileJob: CompileJob,
        jarDirectoryPath: String,
        jarFilePath: String,
        messageLineAdjustment: Int,
        timeoutInSeconds: Int = 60
    ): BuildResult = {
        scalatron.compileActorRefOpt match {
            case None =>
                throw new IllegalStateException("compile actor not available")

            case Some(compileActorRef) =>
                // create the output directory if necessary
                val outputDirectoryPath = compileJob.outputDirectoryPath
                val outputDirectory = new File(outputDirectoryPath)
                if( outputDirectory.exists() ) {
                    // it should not exist before we start. If it does, we delete it
                    deleteRecursively(outputDirectoryPath, scalatron.verbose)
                }
                outputDirectory.mkdirs()


                // compile the source file, using an Akka Actor with a fixed time-out
                implicit val timeout = Timeout(timeoutInSeconds seconds)
                val future = compileActorRef ? compileJob
                val result = Await.result(future, timeout.duration)
                val compileResult = result.asInstanceOf[CompileResult]

                if( compileResult.compilationSuccessful ) {
                    // create the .jar directory, if necessary
                    val localJarDirectory = new File(jarDirectoryPath)
                    if( !localJarDirectory.exists() ) {
                        if( !localJarDirectory.mkdirs() ) {
                            throw new IllegalStateException("failed to create directory for .jar file: " + jarDirectoryPath)
                        }
                        if( scalatron.verbose ) println("created .jar directory at: " + jarDirectoryPath)
                    }

                    // build the .jar archive file
                    JarBuilder(outputDirectoryPath, jarFilePath, scalatron.verbose)

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
                            (msg.pos.line + messageLineAdjustment, msg.pos.column),
                            msg.msg,
                            msg.severity
                        )}
                    )
                )
        }
    }
}