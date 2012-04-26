package scalatron.scalatron.impl

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */


import java.io.File
import PluginCollection.LoadSpec


/** A container class holding plug-in directory scan results. The container is configured to
  * use a particular plug-in directory and game-specific plug-in loading specification and
  * starts out with an empty plug-in load result collection.
  * Each time a game round is about to begin, the collection is updated via a call to
  * 'rescan()' and incrementally:
  *     (a) re-loads all updated plug-ins
  *     (b) loads all newly published plug-ins (new on disk)
  *     (c) flushes all newly removed plug-ins (gone on disk)
  * @param pluginDirectoryPath e.g. "/Users/Scalatron/Scalatron/bots" (no terminating slasgh)
  * @param loadSpec game-specific details about where to load plug-ins from
  * @param verbose if true, plug-in loading progress will be printed verbosely
  * @param loadResults the collection of plug-in loading attempt results
  */
case class PluginCollection(
    pluginDirectoryPath: String,
    loadSpec: LoadSpec,
    verbose: Boolean,
    loadResults: Iterable[Either[Plugin.External, Plugin.LoadFailure]]) {
    def plugins: Iterable[Plugin.External] = loadResults.filter(_.isLeft).map(_.left.get)

    def loadFailures: Iterable[Plugin.LoadFailure] = loadResults.filter(_.isRight).map(_.right.get)


    /** Rescans the plug-in base directory for plug-ins incrementally:
      *     (a) re-loads all updated plug-ins
      *     (b) loads all newly published plug-ins (new on disk)
      *     (c) flushes all newly removed plug-ins (gone on disk)
      * It does so by building a new load result list from scratch based on scanning the
      * directories on disk, but it only re-loads plugins if their file date changed.
      */
    def incrementalRescan: PluginCollection = {
        // println("Scanning incrementally for updated plugins in '" + pluginDirectoryPath + "'...")

        /** Searches the existing list of load results for an entry with the given plug-in file
          * path. If none exists, returns None. If one exists, it checks whether the file time
          * differs. If yes, it returns None. If no, it returns a Some containing the existing,
          * recyclable Either.
          */
        def existingRecyclableLoadResult(pluginFilePath: String, fileTime: Long): Option[Either[Plugin.External, Plugin.LoadFailure]] =
            loadResults.find(_ match {
                case Left(externalPlugin) => externalPlugin.filePath == pluginFilePath && externalPlugin.fileTime == fileTime
                case Right(loadFailure) => loadFailure.filePath == pluginFilePath && loadFailure.fileTime == fileTime
            })

        val pluginParentDirectory = new File(pluginDirectoryPath)
        val pluginDirectories = pluginParentDirectory.listFiles()

        if( pluginDirectories == null ) {
            System.err.println("Plug-in parent directory not valid: '" + pluginDirectoryPath + "'")
            PluginCollection(pluginDirectoryPath, loadSpec, verbose)
        } else {
            // filter out directories like ".info"
            val filteredPluginDirectories = pluginDirectories.filter(_.getName.head != '.')
            val pluginJarFilename = loadSpec.jarFilename

            // for each sub-directory in the base directory, try to load a plug-in
            val options: Iterable[Option[Either[Plugin.External, Plugin.LoadFailure]]] =
                filteredPluginDirectories.map(pluginDirectory => {
                    val pluginDirectoryPath = pluginDirectory.getAbsolutePath

                    // is there are .jar file?
                    val pluginJarFilePath = pluginDirectoryPath + "/" + pluginJarFilename
                    val pluginJarFile = new File(pluginJarFilePath)

                    try {
                        if( pluginJarFile.exists() ) {
                            val fileTime = pluginJarFile.lastModified

                            // does this plug-in already exist?
                            val eitherPluginOrLoadFailure =
                                existingRecyclableLoadResult(pluginJarFilePath, fileTime) match {
                                    case None =>
                                        // there is no recyclable existing plug-in
                                        val userName = pluginDirectory.getName
                                        val eitherFactoryOrException = Plugin.loadFrom(userName, pluginJarFile, loadSpec.factoryClassPackage, loadSpec.factoryClassName, verbose)

                                        eitherFactoryOrException match {
                                            case Left(controlFunctionFactory) =>
                                                val externalPlugin = Plugin.External(pluginDirectoryPath, pluginJarFilePath, fileTime, pluginDirectory.getName, controlFunctionFactory)
                                                println("plugin loaded: " + externalPlugin)
                                                Left(externalPlugin)
                                            case Right(exception) =>
                                                val loadFailure = Plugin.LoadFailure(pluginDirectoryPath, pluginJarFilePath, fileTime, exception)
                                                println("plugin load failure: " + loadFailure)
                                                Right(loadFailure)
                                        }

                                    case Some(existingEither) =>
                                        // there is a recyclable existing plug-in
                                        // println("recycling already-loaded plugin: " + existingEither.merge)
                                        existingEither
                                }

                            Some(eitherPluginOrLoadFailure)
                        } else {
                            println("warning: plug-in file does not exist: " + pluginJarFilePath)
                            None
                        }
                    } catch {
                        case t: Throwable =>
                            System.err.println("warning: exception while examining plug-in file: " + pluginJarFilePath)
                            None
                    }
                })

            PluginCollection(pluginDirectoryPath, loadSpec, verbose, options.flatten)
        }
    }


    /** Rescans the plug-in base directory for plug-ins from scratch, discarding all
      * currently loaded plug-ins. */
    def fullRescan: PluginCollection = {
        println("Rescanning from scratch for plugins in '" + pluginDirectoryPath + "'...")

        val pluginParentDirectory = new File(pluginDirectoryPath)
        val pluginDirectories = pluginParentDirectory.listFiles()

        if( pluginDirectories == null ) {
            println("Plugin parent directory not valid: '" + pluginDirectoryPath + "'")
            PluginCollection(pluginDirectoryPath, loadSpec, verbose)
        } else {
            // filter out directories like ".info"
            val filteredPluginDirectories = pluginDirectories.filter(_.getName.head != '.')
            val pluginJarFilename = loadSpec.jarFilename

            // for each sub-directory in the base directory, try to load a plug-in
            val options: Iterable[Option[Either[Plugin.External, Plugin.LoadFailure]]] =
                filteredPluginDirectories.map(pluginDirectory => {
                    val pluginDirectoryPath = pluginDirectory.getAbsolutePath
                    val pluginFilePath = pluginDirectoryPath + "/" + pluginJarFilename
                    try {
                        val pluginFile = new File(pluginFilePath)
                        if( pluginFile.exists() ) {
                            val fileTime = pluginFile.lastModified

                            val userName = pluginDirectory.getName
                            val eitherFactoryOrException = Plugin.loadFrom(userName, pluginFile, loadSpec.factoryClassPackage, loadSpec.factoryClassName, verbose)

                            val eitherPluginOrLoadFailure = eitherFactoryOrException match {
                                case Left(controlFunctionFactory) =>
                                    val externalPlugin = Plugin.External(pluginDirectoryPath, pluginFilePath, fileTime, pluginDirectory.getName, controlFunctionFactory)
                                    println("plugin loaded: " + externalPlugin)
                                    Left(externalPlugin)
                                case Right(exception) =>
                                    val loadFailure = Plugin.LoadFailure(pluginDirectoryPath, pluginFilePath, fileTime, exception)
                                    println("plugin load failure: " + loadFailure)
                                    Right(loadFailure)
                            }

                            Some(eitherPluginOrLoadFailure)
                        } else {
                            System.err.println("warning: plug-in file does not exist: " + pluginFilePath)
                            None
                        }
                    } catch {
                        case t: Throwable =>
                            System.err.println("warning: exception while examining plug-in file: " + pluginFilePath)
                            None
                    }
                })

            PluginCollection(pluginDirectoryPath, loadSpec, verbose, options.flatten)
        }
    }
}


object PluginCollection {
    def apply(pluginDirectoryPath: String, loadSpec: LoadSpec, verbose: Boolean): PluginCollection =
        PluginCollection(pluginDirectoryPath, loadSpec, verbose, Iterable.empty)


    /** PluginCollection.LoadSpec:
      * a plug-in loading specification specifies from where plugins should be loaded and which
      * class should be extracted. They are game-specific (e.g. to the game BotWar running within
      * the Scalatron server) and are provided by the game factory.
      */
    case class LoadSpec(
        jarFilename: String, // "ScalatronBot.jar"
        factoryClassPackage: String, // "scalatron.botwar.botPlugin"
        factoryClassName: String)


    // "ControlFunctionFactory"
}