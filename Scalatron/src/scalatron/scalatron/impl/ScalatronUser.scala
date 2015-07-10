package scalatron.scalatron.impl

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */


import java.io._
import scala.collection.JavaConverters._

import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.{Await, Future, ExecutionContext}
import scala.concurrent.duration._

import scalatron.scalatron.impl.FileUtil._
import ConfigFile.loadConfigFile
import ConfigFile.updateConfigFileMulti
import ScalatronUser.buildSourceFilesIntoJar
import scalatron.core.Scalatron.Constants._
import scalatron.core.Scalatron._
import java.util.concurrent.TimeoutException
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors._
import org.eclipse.jgit.errors._
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.lib.RepositoryCache.FileKey
import org.eclipse.jgit.util.FS
import scalatron.core._


case class ScalatronUser(name: String, scalatron: ScalatronImpl) extends Scalatron.User {
    require(scalatron.isUserNameValid(name))

    //----------------------------------------------------------------------------------------------
    // cached paths
    //----------------------------------------------------------------------------------------------

    val userDirectoryPath = scalatron.computeUserDirectoryPath(name)
    val userConfigFilePath = userDirectoryPath + "/" + Scalatron.Constants.ConfigFilename

    val sourceDirectoryPath = userDirectoryPath + "/" + UsersSourceDirectoryName
    val sourceFilePath = sourceDirectoryPath + "/" + UsersSourceFileName
    val patchedSourceDirectoryPath = userDirectoryPath + "/" + UsersPatchedSourceDirectoryName

    val outputDirectoryPath = userDirectoryPath + "/" + UsersOutputDirectoryName

    val localJarDirectoryPath = userDirectoryPath + "/" + UsersBotDirectoryName
    val localJarFilePath = localJarDirectoryPath + "/" + Plugin.JarFilename

    val userPluginDirectoryPath = scalatron.pluginBaseDirectoryPath + "/" + name
    val publishedJarFilePath = userPluginDirectoryPath + "/" + Plugin.JarFilename
    val backupJarFilePath = userPluginDirectoryPath + "/" + Plugin.BackupJarFilename

    val gitBaseDirectoryPath = sourceDirectoryPath + "/" + gitDirectoryName

    val gitRepository = RepositoryCache.open(FileKey.exact(new File(gitBaseDirectoryPath), FS.DETECTED), false)
    // Always ensure Git repository is initialised.
    if (!gitRepository.getDirectory.exists()) gitRepository.create()
    val git = new Git(gitRepository)


    /** Releases the cached git repository instance. */
    def release() {
        gitRepository.close()
    }


    //----------------------------------------------------------------------------------------------
    // account management
    //----------------------------------------------------------------------------------------------

    def isAdministrator = ( name == AdminUserName )


    def delete() {
        if( isAdministrator ) {
            throw ScalatronException.Forbidden("deleting '" + Scalatron.Constants.AdminUserName + "' account is not permitted")
        } else {
            // caller must handle IOError exceptions
            deleteRecursively(userDirectoryPath, atThisLevel = true, verbose = scalatron.verbose)
            deleteRecursively(userPluginDirectoryPath, atThisLevel = true, verbose = scalatron.verbose)

            // remove from cache
            release()
            scalatron.userCache.remove(name)
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
        val gameSpecificPackagePath = scalatron.game.gameSpecificPackagePath
        val packagePath = gameSpecificPackagePath + "." + name
        val packageStatement = "package " + packagePath
        val patchedSourceFiles = transientSourceFiles.map(sf => {
            val localCode = sf.code
            // CBB: warn the user about conflicts if she embeds her own package name
            // but if(localCode.contains("package")) ... is too dumb
            val patchedCode =
                if(sf.filename.endsWith(".java")) {
                    packageStatement + ";\n" + localCode
                } else {
                    packageStatement + "\n" + localCode
                }
            if(scalatron.verbose) println("  patching '%s' with 'package %s'".format(sf.filename, packagePath))
            SourceFile(sf.filename, patchedCode)
        })
        val messageLineAdjustment = -1

        // OK, in theory, this should work:
        //   val compileJob = CompileJob.FromMemory(patchedSourceFiles, outputDirectoryPath)
        // but unfortunately, we're doing something wrong in setting up the virtual files to compile from,
        // so the compiler chokes while trying to sort its dependent files by rank, or something like that.

        // so, as a temporary work-around, we create temp files on disk:
        // TODO: this code should probably exist within writeSourceFiles() - refactor!
        val patchedSourceDirectory = new File(patchedSourceDirectoryPath)
        if(patchedSourceDirectory.exists) {
            deleteRecursively(patchedSourceDirectoryPath, atThisLevel = false, verbose = scalatron.verbose)
        } else {
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
        try {
            git.log().call().asScala.map(ScalatronVersion(_, this))
        } catch {
            // Git isn't initialised - just ignore
            case _: NoHeadException => Iterable()
            case e: JGitInternalException => throw new IOError(e)
        }
    }


    def version(id: String): Option[Version] = try {
        // Get the
        git.log().add(ObjectId.fromString(id)).setMaxCount(1).call().asScala.map(ScalatronVersion(_, this)).headOption
    } catch {
        // Git isn't initialised - just ignore
        case _: NoHeadException => None
        // This matches previous behaviour
        case _: MissingObjectException => None
        case _: IncorrectObjectTypeException => None
        case e: JGitInternalException => throw new IOError(e)
    }

    def createVersion(label: String): Option[ScalatronVersion] = {
        // Add all new and modified files to Git
        git.add().addFilepattern(".").call
        // Remove any deleted files from Git
        git.add().addFilepattern(".").setUpdate(true).call

        try {
            // Check to see if anything has changed
            // Unlike Git, JGit will allow empty commits by default
            if(git.status().call().isClean) {
               None
            } else {
                try {
                    // Create Git commit from changes
                    val commit = git.commit().setCommitter(name, name + "@scalatron.github.com").setMessage(label).call
                    Some(ScalatronVersion(commit, this))
                } catch {
                    case e: NoHeadException => throw new IllegalStateException(e)
                    // This should never happen as we are calling setMessage above
                    case e: NoMessageException => throw new IllegalStateException(e)
                    case e: UnmergedPathException => throw new IllegalStateException(e)
                    case e: ConcurrentRefUpdateException => throw new IllegalStateException(e)
                    case e: WrongRepositoryStateException => throw new IllegalStateException(e)
                    case e: JGitInternalException => throw new IOError(e)
                }
            }
        } catch {
            case e: IOException => throw new IOError(e)
        }
    }


    /**
     * Returns the SourceFileCollection from Git for a given version.
     */
    def getSourceFiles(version: ScalatronVersion): SourceFileCollection = {
        val walk = new TreeWalk(gitRepository)
        // Walk the Git tree for _this_ version
        walk.addTree(version.commit.getTree)
        // Recurse subdirectories
        walk.setRecursive(true)
        val reader = walk.getObjectReader
        val list = collection.mutable.ListBuffer[SourceFile]()
        // Iterate over the TreeWalk for each file in the tree
        while (walk.next()) {
            // TODO Encoding?!?
            val code = new String(reader.open(walk.getObjectId(0)).getCachedBytes)
            list += SourceFile(walk.getPathString, code)
        }
        list
    }





    //----------------------------------------------------------------------------------------------
    // tournament management
    //----------------------------------------------------------------------------------------------

    def publish() {
        // delete the old backup.jar file
        val backupJarFile = new File(backupJarFilePath)
        if( backupJarFile.exists ) {
            if( scalatron.verbose ) println("      deleting backup .jar file: " + backupJarFilePath)
            if( !backupJarFile.delete() ) {
                System.err.println("failed to delete backup .jar file at: %s" format backupJarFilePath)
                throw new IllegalStateException("failed to delete backup .jar file at: %s" format backupJarFilePath)
            }
        }

        // then move away the current .jar file
        val publishedJarFile = new File(publishedJarFilePath)
        if( publishedJarFile.exists ) {
            if( scalatron.verbose ) println("      backing up current .jar file: " + publishedJarFilePath + " => " + backupJarFilePath)
            if( !publishedJarFile.renameTo(backupJarFile) ) {
                System.err.println("failed to rename .jar file to backup: %s" format backupJarFilePath)
                throw new IllegalStateException("failed to rename .jar file to backup: %s" format backupJarFilePath)
            }
        }


        // then copy the local .jar file
        val localJarFile = new File(localJarFilePath)
        if( localJarFile.exists ) {
            if( scalatron.verbose ) println("      activating new .jar file: " + localJarFilePath + " => " + publishedJarFilePath)

            val userPluginDirectory = new File(userPluginDirectoryPath)
            if( !userPluginDirectory.exists() ) {
                if( !userPluginDirectory.mkdirs() ) {
                    System.err.println("failed to create user plug-in directory: %s" format userPluginDirectoryPath)
                    throw new IllegalStateException("failed to create user plug-in directory: %s" format userPluginDirectoryPath)
                }
                if( scalatron.verbose ) println("created user plug-in directory for '" + name + "' at: " + userPluginDirectoryPath)
            }
            try {
                copyFile(localJarFilePath, publishedJarFilePath)
            } catch {
                case t: Throwable =>
                    System.err.println("failed to copy .jar file '%s' to '%s': %s" format(localJarFilePath, publishedJarFilePath, t.toString))
                    throw new IllegalStateException("failed to copy .jar file '%s' to '%s': %s" format(localJarFilePath, publishedJarFilePath, t.toString))
            }
        } else {
            System.err.println(".jar file intended for publication does not exist: %s" format localJarFilePath)
            throw new IllegalStateException(".jar file intended for publication does not exist: %s" format localJarFilePath)
        }
    }


    //----------------------------------------------------------------------------------------------
    // sandbox management
    //----------------------------------------------------------------------------------------------

    var nextSandboxId = 0

    def createSandbox(argMap: Map[String, String]) = {
        // determine the location of the user's local bot
        val localJarFile = new File(localJarFilePath)
        val plugins: Iterable[Plugin] =
            if( localJarFile.exists() ) {
                // attempt to load the plug-in
                val eitherFactoryOrException =
                    Plugin.loadBotControlFunctionFrom(
                        localJarFile,
                        name,
                        scalatron.game.gameSpecificPackagePath,
                        Plugin.ControlFunctionFactoryClassName,
                        scalatron.verbose)

                eitherFactoryOrException match {
                    case Left(controlFunctionFactory) =>
                        val fileTime = localJarFile.lastModified()
                        val externalPlugin = Plugin.FromJarFile(localJarDirectoryPath, localJarFilePath, fileTime, name, controlFunctionFactory)
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

        val entityControllers = EntityControllerImpl.fromPlugins(plugins)(scalatron.executionContextForUntrustedCode)

        // determine the permanent configuration for the game - in particular, that it should run forever
        val permanentConfig = PermanentConfig(secureMode = scalatron.secureMode, stepsPerRound = Int.MaxValue)

        // determine the per-round configuration for the game
        val roundIndex = 0
        val roundConfig = RoundConfig(permanentConfig, argMap, roundIndex)

        val initialSimState = scalatron.game.startHeadless(entityControllers, roundConfig, scalatron.executionContextForUntrustedCode)
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
        messageLineAdjustment: Int
    ): BuildResult = {
        scalatron.compileWorkerRouterOpt match {
            case None =>
                throw new IllegalStateException("compile actor not available")

            case Some(compileWorkerRouter) =>
                // create the output directory if necessary
                val outputDirectoryPath = compileJob.outputDirectoryPath
                val outputDirectory = new File(outputDirectoryPath)
                if( outputDirectory.exists() ) {
                    // it should be empty before we start. If it exists, we clear it
                    deleteRecursively(outputDirectoryPath, atThisLevel = false, verbose = scalatron.verbose)
                } else {
                    outputDirectory.mkdirs()
                }


                // compile the source file, using an Akka Actor with a fixed time-out
                try {
                    val timeoutInSeconds = 200      // Note: UI has a 60-second timeout
                    implicit val timeout = Timeout(timeoutInSeconds seconds)
                    val future = compileWorkerRouter ? compileJob
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

                        // we DO NOT delete the contents of the output directory, even though they are no longer needed
                        // the directory will be cleared before the next compilation starts, and leaving the files is
                        // useful when diagnosing problems.
                        // deleteRecursively(outputDirectoryPath, atThisLevel = false, verbose = scalatron.verbose)
                    }

                    // transform compiler output into the BuildResult format expected by the Scalatron API
                    val sourceDirectoryPrefix = sourceDirectoryPath + "/"
                    BuildResult(
                        compileResult.compilationSuccessful,
                        compileResult.duration,
                        compileResult.errorCount,
                        compileResult.warningCount,
                        compileResult.compilerMessages.map(msg => {
                            val absoluteSourceFilePath = msg.pos.sourceFilePath
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
                } catch {
                    case t: TimeoutException =>
                        BuildResult(
                            successful = false,
                            duration = 0,
                            errorCount = 1,
                            warningCount = 0,
                            messages = Iterable(BuildResult.BuildMessage("", (0,0), "Compile job timed out while waiting on the server. The server may be too busy. You could try it again in a minute.", 0))
                        )
                }
        }
    }
}