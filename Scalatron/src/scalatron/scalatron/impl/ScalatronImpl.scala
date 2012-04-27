/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.scalatron.impl

import java.io._
import akka.actor._
import scalatron.botwar.BotWar
import scalatron.scalatron.api.Scalatron.Constants._
import scalatron.scalatron.api.Scalatron
import scalatron.scalatron.impl.ScalatronUser.writeSourceFiles
import scalatron.Version
import scalatron.scalatron.api.Scalatron.{ScalatronException, SourceFile, User}
import java.text.DateFormat
import java.util.Date


object ScalatronImpl
{
    /** Creates an instance of a Scalatron server. The returned instance is also the main entry
      * point for the Scalatron API. Calling this method does not start any background threads.
      * Before using the API, call start() to launch any required the background thread(s), such
      * as the background compile server). When done, call shutdown() to terminate the background
      * thread(s).
      * @param actorSystem the Akka actor system to use, e.g. for compilation
      * @param argMap map of command line arguments
      * @param verbose if true, use verbose logging
      * @return
      */
    def apply(argMap: Map[String, String], verbose: Boolean)(implicit actorSystem: ActorSystem): Scalatron = {
        // find out which game variant the server should host, since, in theory, the game may be configurable some day
        val gameName = argMap.get("-game").getOrElse("BotWar")
        val game = gameName match {
            case "BotWar" => BotWar
            case _ => throw new IllegalArgumentException("unknown game name: " + gameName)
        }

        // try to locate a base directory for the installation, e.g. '/Scalatron'
        val scalatronInstallationDirectoryPath = detectInstallationDirectory(verbose)


        // extract the web user base directory from the command line ("/users")
        val usersBaseDirectoryPathFallback = scalatronInstallationDirectoryPath + "/" + UsersDirectoryName
        val usersBaseDirectoryPathArg = argMap.get("-users").getOrElse(usersBaseDirectoryPathFallback)
        val usersBaseDirectoryPath = if(usersBaseDirectoryPathArg.last == '/') usersBaseDirectoryPathArg.dropRight(1) else usersBaseDirectoryPathArg
        if(verbose) println("Will maintain user workspace in: " + usersBaseDirectoryPath)

        // extract the samples base directory from the command line ("/samples")
        val samplesBaseDirectoryPathFallback = scalatronInstallationDirectoryPath + "/" + SamplesDirectoryName
        val samplesBaseDirectoryPathArg = argMap.get("-samples").getOrElse(samplesBaseDirectoryPathFallback)
        val samplesBaseDirectoryPath = if(samplesBaseDirectoryPathArg.last == '/') samplesBaseDirectoryPathArg.dropRight(1) else samplesBaseDirectoryPathArg
        if(verbose) println("Will look for samples in: " + samplesBaseDirectoryPath)

        // extract the plugin base directory from the command line
        // construct the complete plug-in path and inform the user about it
        val pluginBaseDirectoryPathFallback = scalatronInstallationDirectoryPath + "/" + TournamentBotsDirectoryName
        val pluginBaseDirectoryPathArg = argMap.get("-plugins").getOrElse(pluginBaseDirectoryPathFallback)
        val pluginBaseDirectoryPath = if(pluginBaseDirectoryPathArg.last == '/') pluginBaseDirectoryPathArg.dropRight(1) else pluginBaseDirectoryPathArg
        if(verbose) println("Will search for sub-directories containing bot plug-ins in: " + pluginBaseDirectoryPath)


        // prepare (but do not start) the Scalatron API entry point
        ScalatronImpl(
            game,
            scalatronInstallationDirectoryPath,
            usersBaseDirectoryPath,
            samplesBaseDirectoryPath,
            pluginBaseDirectoryPath,
            TournamentState.Empty,
            verbose
        )
    }



    // try to locate a base directory for the installation, e.g. '/Scalatron'
    def detectInstallationDirectory(verbose: Boolean) = {
        // Strategy A: use the Java user directory
        // -- problematic, because: this fails unless the user double-clicked the .jar file
        // val userDirectoryPath = System.getProperty("user.dir")
        // val scalatronInstallationDirectoryPath = userDirectoryPath + "/.."

        // Strategy B: use the path to the Scalatron.jar file
        val scalatronJarFilePath = classOf[ScalatronImpl].getProtectionDomain.getCodeSource.getLocation.getPath
        if (verbose) println("Detected Scalatron class path to be: " + scalatronJarFilePath)

        val scalatronInstallationDirectoryPath =
            cleanJarFilePath(scalatronJarFilePath) match {
                case Left(filePath) =>
                    // e.g. "/Scalatron/bin/Scalatron.jar" => "/Scalatron"
                    val scalatronJarFile = new File(filePath)
                    scalatronJarFile.getParentFile.getParentFile.getAbsolutePath
                case Right(dirPath) =>
                    // e.g. "/Scalatron/out/production/Scalatron/" => "/Scalatron"
                    val scalatronOutDirectory = new File(dirPath)
                    scalatronOutDirectory.getParentFile.getParentFile.getParentFile.getAbsolutePath
            }
        if (verbose) println("Scalatron installation directory is: " + scalatronInstallationDirectoryPath)

        // as an added precaution, also check whether the installation directory exists
        val scalatronInstallationDirectory = new File(scalatronInstallationDirectoryPath)
        if(!scalatronInstallationDirectory.exists()) {
            System.err.println("warning: detected installation directory does not exist: " + scalatronInstallationDirectory)
            // but try it anyway...
        }

        scalatronInstallationDirectoryPath
    }



    /** This function helps in cleaning up .jar file paths. When you invoke:
      *   val jarFilePath = classOf[ScalatronImpl].getProtectionDomain.getCodeSource.getLocation.getPath
      * you may get back one of several possible results, including the following:
      * (a) when running from unpackaged .class files: "/Users/dev/Scalatron/Scalatron/out/production/Scalatron/"
      * (b) when running from a .jar produced by onejar: "file:/Users/dev/Scalatron/dist/bin/Scalatron.jar!/main/scalatron_2.9.1-0.1-SNAPSHOT.jar"
      * (c) when running from a .jar produced by IDEA: "/Users/dev/Scalatron/bin/Scalatron.jar"
      * This function returns:
      *  Left(jarFilePath) in casees (b) and (c) -- but cleaned up to remove "file:" and the exclamation mark
      *  Right(jarDirectoryPath) in case (a)
      * It does not fail if the file or directory does not actually exist (returns Left()).
      */
    def cleanJarFilePath(jarFilePath: String) : Either[String,String] =
    {
        // remove a leading "file:", if present:
        val withoutFilePrefix = if(jarFilePath.startsWith("file:")) jarFilePath.drop(5) else jarFilePath

        // truncate at "!", if present in the form ".jar!/":
        val indexOfJarContentMarker = withoutFilePrefix.toLowerCase.indexOf(".jar!/")   // safer than just '!', think about paths like ".../!TEST!/..."
        val truncatedAtExclamation = if(indexOfJarContentMarker>0) withoutFilePrefix.take(indexOfJarContentMarker+4) else withoutFilePrefix

        // now check whether the file or directory exists
        val jarFile = new File(truncatedAtExclamation)
        if(!jarFile.exists()) {
            System.err.println("warning: no .jar file found at detected class path: " + truncatedAtExclamation)
            Left(truncatedAtExclamation)
        } else {
            // we expect this to be a '.jar' file or a directory
            if(new File(truncatedAtExclamation).isFile) {
                // it is a file
                if(!truncatedAtExclamation.toLowerCase.endsWith(".jar")) {
                    System.err.println("warning: class path detected for Scalatron classes does not point into .jar file: " + truncatedAtExclamation)
                    // but try it anyway...
                }
                Left(truncatedAtExclamation)
            } else {
                // it is a directory -- OK
                Right(truncatedAtExclamation)
            }
        }
    }
}


/**
  * @param actorSystem the Akka actor system to use, e.g. for compilation
  * @param game the Game implementation to run
  * @param installationDirectoryPath the installation directory, e.g. /Scalatron
  * @param usersBaseDirectoryPath the user base directory, e.g. /users
  * @param samplesBaseDirectoryPath the samples base directory, e.g. /samples
  * @param pluginBaseDirectoryPath the bot plug-in base directory, e.g. /bots
  * @param tournamentState the tournament state holding object (mutable), to allow external access to state
  * @param verbose if true, use verbose logging
  */
case class ScalatronImpl(
    game: Game,
    installationDirectoryPath: String, // e.g. /Scalatron
    usersBaseDirectoryPath: String, // e.g. /Scalatron/users
    samplesBaseDirectoryPath: String, // e.g. /Scalatron/samples
    pluginBaseDirectoryPath: String, // e.g. /Scalatron/bots
    tournamentState: TournamentState, // receives and accumulates tournament round results
    verbose: Boolean)
        (implicit val actorSystem: ActorSystem)   // the Akka actor system to use, e.g. for compilation
    extends Scalatron
{
    var compileActorRefOpt: Option[ActorRef] = None


    //----------------------------------------------------------------------------------------------
    // start/run/stop
    //----------------------------------------------------------------------------------------------

    def version = Version.VersionString

    def start() {
        val compileActorRef = actorSystem.actorOf(Props(new CompileActor(verbose)), name = "CompileActor")
        compileActorRefOpt = Some(compileActorRef)
    }

    def run(argMap: Map[String, String]) {
        val rounds = argMap.get("-rounds").map(_.toInt).getOrElse(Int.MaxValue)
        val headless = (argMap.get("-headless").getOrElse("no") == "yes")
        if(headless) {
            game.runHeadless(pluginBaseDirectoryPath, argMap, rounds, tournamentState, verbose)
        } else {
            game.runVisually(pluginBaseDirectoryPath, argMap, rounds, tournamentState, verbose)
        }
    }

    def shutdown() {
        // stop accepting compile jobs
        compileActorRefOpt = None
    }


    //----------------------------------------------------------------------------------------------
    // (web) user management
    //----------------------------------------------------------------------------------------------

    /** Makes sure that the '/users' directory exists.
      * @throws IOError if failed to create user base directory
      */
    private def ensureUserBaseDirectoryExists() {
        val usersBaseDirectory = new File(usersBaseDirectoryPath)
        try {
            if(!usersBaseDirectory.exists()) {
                usersBaseDirectory.mkdirs()
                if(verbose) println("created user base directory: " + usersBaseDirectoryPath)
            }
        } catch {
            case t: Throwable =>
                System.err.println("error: failed to create user base directory: " + usersBaseDirectoryPath + ": " + t)
                throw t
        }
    }


    /** Makes sure that the user directory '/users/{userName}' exists. Returns its path. */
    private def ensureUserDirectoryExists(userName: String): String = {
        // make sure that the 'admin' user directory exists
        val userDirectoryPath = usersBaseDirectoryPath + "/" + userName
        val userDirectory = new File(userDirectoryPath)
        try {
            if(!userDirectory.exists()) {
                userDirectory.mkdirs()
                if(verbose) println("created admin user directory: " + userDirectoryPath)
            }
            userDirectoryPath
        } catch {
            case t: Throwable =>
                System.err.println("error: failed to create user directory: " + userDirectoryPath + ": " + t)
                throw t
        }
    }


    def users(): Iterable[User] = {
        // make sure that the 'users' directory exists
        ensureUserBaseDirectoryExists()

        // make sure that the 'Administrator' user directory exists
        val adminUserDirectoryPath = ensureUserDirectoryExists(Scalatron.Constants.AdminUserName)

        // make sure that the 'admin' config file exists
        val adminUserConfigFilePath = adminUserDirectoryPath + "/" + Scalatron.Constants.ConfigFilename
        val adminUserConfigFile = new File(adminUserConfigFilePath)
        try {
            if(!adminUserConfigFile.exists()) {
                ScalatronUser.updateConfigFileMulti(
                    adminUserConfigFilePath,
                    Map(
                        "password" -> Scalatron.Constants.AdminDefaultPassword,
                        "creationTime" -> DateFormat.getDateTimeInstance.format(new Date())
                    )
                )
                if(verbose) println("created Administrator user config file: " + adminUserConfigFilePath)
            }
        } catch {
            case t: Throwable =>
                System.err.println("error: failed to create Administrator user config file: " + adminUserConfigFilePath + ": " + t)
        }


        // then list all accounts
        try {
            val usersBaseDirectory = new File(usersBaseDirectoryPath)
            val usersBaseDirectoryFiles = usersBaseDirectory.listFiles()
            if(usersBaseDirectoryFiles == null) {
                Iterable.empty
            } else {
                val usersDirectories = usersBaseDirectoryFiles.filter(_.isDirectory)
                if(usersDirectories.isEmpty) {
                    Iterable.empty
                } else {
                    usersDirectories.map(dir => ScalatronUser(dir.getName, this))
                }
            }
        } catch {
            case t: Throwable =>
                System.err.println("error listing user directories in: " + usersBaseDirectoryPath + ": " + t)
                Iterable.empty
        }
    }


    def user(name: String) = {
        requireLegalUserName(name) // throws ScalatronException.IllegalUserName

        // prepare user object; we'll use this to fetch the paths
        val user = ScalatronUser(name, this)

        val userDirectoryPath = user.userDirectoryPath
        if(new File(userDirectoryPath).exists()) {
            Some(user)
        } else {
            None
        }
    }


    def createUser(name: String, password: String, initialSourceFiles: Iterable[SourceFile]) = {
        requireLegalUserName(name) // throws ScalatronException.IllegalUserName

        // make sure that the 'users' directory exists
        ensureUserBaseDirectoryExists()

        // prepare user object; we'll use this to fetch the paths
        val user = ScalatronUser(name, this)

        // create the user base directory, if necessary
        val userDirectoryPath = user.userDirectoryPath
        val userDirectory = new File(userDirectoryPath)
        if(userDirectory.exists()) {
            System.err.println("refused to create already-existing user: '" + name + "'")
            throw ScalatronException.Exists(name)
        } else {
            userDirectory.mkdirs()
            if(verbose) println("created user directory: " + userDirectoryPath)
        }

        // create the user password config file; caller handles exceptions
        val userConfigFilePath = user.userConfigFilePath
        val userConfigFile = new File(userConfigFilePath)
        if(userConfigFile.exists()) {
            System.err.println("configuration file for user '" + name + "' already exists")
            throw ScalatronException.Exists(name + ": configuration file")
        } else {
            ScalatronUser.updateConfigFileMulti(
                userConfigFilePath,
                Map(
                    "password" -> password,
                    "creationTime" -> DateFormat.getDateTimeInstance.format(new Date())
                )
            )
            if(verbose) println("created user config file: " + userConfigFilePath)
        }

        // create a /src directory if required
        val sourceDirectoryPath = user.sourceDirectoryPath
        val sourceDirectory = new File(sourceDirectoryPath)
        if(!sourceDirectory.exists) {
            if(!sourceDirectory.mkdirs()) {
                System.err.println("error: failed to create user /src directory for '" + name + "'")
                throw ScalatronException.CreationFailed(name + ": source directory", "could not create directory")
            }
            if(verbose) println("created user /src directory '" + sourceDirectoryPath + "'")
        }

        // write the initial source files to disk
        // CBB: this should be wrapped into try/catch, with cleanup on error
        initialSourceFiles.foreach(sf => {
            val path = sourceDirectoryPath + "/" + sf.filename
            val sourceFile = new FileWriter(path)
            sourceFile.append(sf.code)
            sourceFile.close()
            if(verbose) println("created initial source file for user '" + name + "': " + path)
        })

        user
    }


    /** Verfies that the user name is valid.
      * @param name the user name to verify.
      * @throws ScalatronException.IllegalUserName if the user name contained illegal characters
      */
    def requireLegalUserName(name: String) {
        name.foreach(c => {
            if(".,|/\\\"'*?".contains(c))
                throw new ScalatronException.IllegalUserName(name, "illegal character: '" + c + "'")
        })
    }

    // TODO: use a white list, not a black list
    // TODO: disallow leading numeric literals
    def isUserNameValid(name: String): Boolean = name.forall(c => !(" -.,|/\\\"'*?".contains(c)))


    //----------------------------------------------------------------------------------------------
    // sample code management
    //----------------------------------------------------------------------------------------------

    def samples = {
        // enumerate directory in e.g. "/Scalatron/samples"
        val samplesBaseDirectory = new File(samplesBaseDirectoryPath)
        val sampleDirectories = samplesBaseDirectory.listFiles()
        if(sampleDirectories == null || sampleDirectories.isEmpty) {
            Iterable.empty
        } else {
            sampleDirectories.filter(_.isDirectory).map(dir => ScalatronSample(dir.getName, this))
        }
    }

    def sample(name: String) = {
        val sampleDirectoryPath = samplesBaseDirectoryPath + "/" + name
        if(new File(sampleDirectoryPath).exists())
            Some(ScalatronSample(name, this))
        else
            None
    }

    def createSample(name: String, sourceFiles: Iterable[SourceFile]) = {
        // pre-create the sample so that we can use its calculated paths
        val sample = ScalatronSample(name, this)

        val sampleDirectoryPath = sample.sampleDirectoryPath
        val sampleDirectory = new File(sampleDirectoryPath)
        if(sampleDirectory.exists) {
            System.err.println("error: sample directory already exists: " + sampleDirectoryPath)
            throw new IllegalStateException("sample directory already exists: " + sampleDirectoryPath)
        }

        // write source files to disk
        val sampleSourceDirectoryPath = sample.sampleSourceDirectoryPath
        val sampleSourceDirectory = new File(sampleSourceDirectoryPath)
        if(!sampleSourceDirectory.mkdirs()) {
            System.err.println("error: failed to create sample source directory: " + sampleDirectoryPath)
            throw new IllegalStateException("could not create sample source directory: " + sampleDirectoryPath)
        }
        if(verbose) println("created sample source directory: " + sampleDirectoryPath)
        writeSourceFiles(sampleSourceDirectoryPath, sourceFiles, verbose)

        // future: write documentation file(s) to disk
        // future: write bot file

        sample
    }


    //----------------------------------------------------------------------------------------------
    // tournament management
    //----------------------------------------------------------------------------------------------

    def tournamentRoundsPlayed = tournamentState.roundsPlayed

    def tournamentRoundResults(maxRounds: Int) =
        tournamentState.roundResults(maxRounds).map(_.map)

    def tournamentAverageResults(maxRounds: Int) =
        tournamentState.aggregateResult(maxRounds).averageMustHavePlayedAllRounds

    def tournamentLeaderboard =
        tournamentState.leaderBoard

}





