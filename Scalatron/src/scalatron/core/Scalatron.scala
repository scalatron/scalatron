package scalatron.core

/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */

import scalatron.core.Scalatron.{Sample, User, SourceFileCollection}
import java.io.{FileWriter, File}
import scalatron.util.FileUtil
import FileUtil.use


/** This is the "outward" API that Scalatron exposes towards the main function and the web server.
  * It represents the main API entry point of the Scalatron server. It is distinct from the "inward"
  * API that Scalatron exposes towards the game plug-ins it loads.
  */
trait Scalatron
{
    //----------------------------------------------------------------------------------------------
    // configuration information
    //----------------------------------------------------------------------------------------------

    /** Returns the version of the Scalatron server exposing this API, e.g. "1.0.0.2". */
    def version: String

    /** Returns whether the Scalatron server is running in secure mode. In secure mode, bot plug-in
      * code is subject to a range of restrictions: access to sensitive resources (network, file system)
      * is denied; control function processing time is restricted; number of entities a plug-in can
      * have at any one time is restricted.
      * @return true if Scalatron is running in "secure" mode and bot plug-ins should be isolated and constrained. */
    def secureMode: Boolean

    /** @return true if Scalatron is running in "verbose" mode and debug information may be logged to the console. */
    def verbose: Boolean

    /** @return map containing command line arguments passed to the Scalatron server at startup. */
    def argMap: Map[String, String]

    /** Returns the path of the directory where Scalatron is installed.
      * Note: this path is passed into the API as a parameter; it is determined by detecting the
      * path where the Scalatron.jar file resides. You can use this path to construct dependent
      * file system locations (whic the server does for /samples, /bots, /users, /webui, etc.) */
    def installationDirectoryPath: String



    //----------------------------------------------------------------------------------------------
    // (web) user management
    //----------------------------------------------------------------------------------------------

    /** Returns a list of user accounts. The list of users is determined by listing the
      * directories in the user base directory (e.g. "/Scalatron/users").
      * The method has side effects:
      * - The user base directory ("/Scalatron/users") is created if it does not exist.
      * - The Administrator account directory ("/Scalatron/users/Administrator") and
      * configuration file are created if they do not exist.
      * @throws IOError if failed to create user base directory or Administrator directory or config file
      */
    def users(): Iterable[User]

    /** Retrieves the user account with the given name. Returns a Some(User) if an account for
      * the given name exists and None if it does not.
      * @throws ScalatronException.IllegalUserName if the user name contains invalid characters
      */
    def user(name: String): Option[User]

    /** Creates a new user account for a user with the given name, assigning it the given
      * password. The password can be empty if the user should be able to log in without being
      * prompted for a password. Creates a source code directory for the user (e.g. at
      * "Scalatron/users/{user}/src/") and populates it with the given initial source files.
      * @throws ScalatronException.IllegalUserName if the user name contained illegal characters
      * @throws ScalatronException.Exists if a user with the given name already exists
      * @throws ScalatronException.CreationFailure if the user could not be created (e.g. because /src dir could not be created).
      * @throws IOError if the user's initial source files could not be written to disk.
      */
    def createUser(name: String, password: String, initialSourceFiles: SourceFileCollection): User

    /** Checks if the given string would represent a valid user name by examining the characters
      * in the string. A variety of characters are not allowed primarily for the following reasons:
      * (a) the user names are used as components of path names on disk;
      * this poses a security problem if we allow names that include substrings like "../"
      * (b) the user names are used as components of package names in Scala source files;
      * we need to obey Scala's rules. This rules out space, dash, leading numbers, etc.
      */
    def isUserNameValid(name: String): Boolean


    //----------------------------------------------------------------------------------------------
    // sample code management
    //----------------------------------------------------------------------------------------------

    /** Returns a collection of samples, i.e. of named source file packages, such as bot versions
      * taken from the Scalatron tutorial. The samples are enumerated by enumerating the
      * sub-directories of the /sample base directory on disk, e.g. "/Scalatron/samples/{sample}". */
    def samples: Iterable[Sample]

    /** Returns the Sample instance associated with a particular name, which can then be used to
      * retrieve the source file collection associated with the sample or to delete the sample.
      * Returns Some(Sample) if a sample with the given name exists, None otherwise. */
    def sample(name: String): Option[Sample]

    /** Creates a new sample with the given name from the given source file collection.
      * Users can employ this to share bot code across the network. */
    def createSample(name: String, sourceFiles: SourceFileCollection): Sample


    //----------------------------------------------------------------------------------------------
    // tournament management
    //----------------------------------------------------------------------------------------------

    /** Returns the number of rounds played in the currently running tournament. */
    def tournamentRoundsPlayed: Int

    /** Returns a collection of tournament round results for up to the given number of most
      * recent rounds, sorted from most to least recent. The result is a collection of maps
      * from name to score. */
    def tournamentRoundResults(maxRounds: Int): Iterable[Map[String, Int]]

    /** Returns an aggregated tournament result for up to the given number of most recent rounds.
      * The result is a collection of maps from name to average score. The list contains only
      * those participants that actually have a result in all of the requested rounds. */
    def tournamentAverageResults(maxRounds: Int): Map[String, Int]

    /** Returns the leader board of the tournament, which holds the winners, sorted by score,
      * across the most recent 1,5,20 and all rounds. The returned array contains four entries,
      * each a tuple: (rounds, Array[(name,score)]). */
    def tournamentLeaderboard: Scalatron.LeaderBoard
}


object Scalatron
{
    /** A Leaderboard is an array holding the winners across the most recent 1,5,20 and all rounds.
      * The Array contains tuples: (rounds, Array[(name,score)] */
    type LeaderBoard = Array[(Int, Array[(String, Int)])]



    /** Specific exceptions thrown by Scalatron API. */
    class ScalatronException(msg: String) extends RuntimeException(msg)

    object ScalatronException
    {

        /** ScalatronException.IllegalUserName: user name contained illegal characters. */
        case class IllegalUserName(userName: String, violation: String) extends ScalatronException("illegal user name '" + userName + "': " + violation)

        /** ScalatronException.Exists: if an user/version/sample/etc. with the given id/name already exists. */
        case class Exists(entityName: String) extends ScalatronException("'" + entityName + "' already exists")

        /** ScalatronException.CreationFailure: if a user/version/sample/etc. could not be created. */
        case class CreationFailed(entityName: String, reason: String) extends ScalatronException("'" + entityName + "' could not be created: " + reason)

        /** ScalatronException.Forbidden: if an operation is forbidden, such as deleting the Administrator account. */
        case class Forbidden(reason: String) extends ScalatronException(reason)

    }


    /** Scalatron.User: trait that provides an interface for interaction with web user accounts of
      * the Scalatron server. Essentially just a wrapper for a user name and a reference to the
      * main Scalatron trait instance. */
    trait User
    {
        //----------------------------------------------------------------------------------------------
        // account management
        //----------------------------------------------------------------------------------------------

        /** Returns the user name associated with this web user object. */
        def name: String

        def isAdministrator: Boolean

        /** Deletes the contents of an existing web user account. Also deletes the users' plug-in
          * directory below the tournament plug-in base directory, if it exists.
          * CAUTION: all information in the user's web user directory will be destroyed (!).
          * @throws ScalatronException.Forbidden attempt to delete the Administrator account
          * @throws IOError if the user's workspace directories could not be deleted.
          */
        def delete()


        // get/set configuration attributes

        /** Updates the configuration attribute map of the user.
          * Values present in the given map but not in the config file are added.
          * Values present in the given map and in the config file are updated.
          * Values present in the config files but not in the given map are retained.
          * the password of the given user to the given string.
          * Caution: ensure proper credentials for remote users when updating the 'password' value this way.
          * @throws IOError on failure to read or write the user's configuration file.
          */
        def updateAttributes(map: Map[String, String])

        /** Returns the configuration attribute value associated with the given key.
          * Returns None if the attempt to find a configuration file or to find the key fails.
          * Returns Some(value) if a value was found (but it may be empty). If the config file is empty, the password is returned as an empty string.
          * map for this user or None if no such map exists.
          * Caution: do not return the 'password' value through a remote connection.
          * Does not throw any exceptions (IO errors are mapped to None). */
        def getAttributeOpt(key: String): Option[String] =
            getAttributeMapOpt match {
                case None => None
                case Some(attibuteMap) => attibuteMap.get(key)
            }

        /** Returns the configuration attribute map for this user or None if no such map exists.
          * Caution: do not return the 'password' value through a remote connection.
          * Does not throw any exceptions (IO errors are mapped to None). */
        def getAttributeMapOpt: Option[Map[String, String]]


        // get/set password

        /** Returns the password for the given user.
          * Returns None if no password can be found (log-on should be disabled).
          * Returns Some(value) if a password was found (but it may be empty).
          * Caution: do not return the 'password' value through a remote connection.
          * Does not throw any exceptions (IO errors are mapped to None). */
        def getPasswordOpt: Option[String] = { getAttributeOpt(Scalatron.Constants.PasswordKey) }

        /** Sets the password of the given user to the given string.
          * @throws IOError on failure to read or write the user's configuration file.
          */
        def setPassword(password: String) { updateAttributes(Map(Scalatron.Constants.PasswordKey -> password)) }


        //----------------------------------------------------------------------------------------------
        // source code & build management
        //----------------------------------------------------------------------------------------------

        /** Returns the source files currently present in the user's source code directory
          * @throws IllegalStateException if the source directory does not exist.
          * @throws IOError if the source files could not be read.
          */
        def sourceFiles: SourceFileCollection

        /** Updates the source code of the given user to the given source files.
          * The updated source file(s) are written to e.g. "/Scalatron/users/{user}/src/".
          * This method on its own basically corresponds to a "Save" or auto-save function in the
          * browser UI.
          * This is intended for use by the web ui when a user saves her work to the server
          * before instructing it to build them.
          * @throws IOError if the source files could not be written.
          **/
        def updateSourceFiles(sourceFiles: SourceFileCollection)

        /** Builds a local (unpublished) .jar bot plug-in from the given (in-memory) source files.
          *
          * Internals:
          * - patches up the package statements to make the fully-qualified class names user-specific
          * - compiles those source files into a temporary output directory (e.g. "/Scalatron/users/{user}/out")
          * - if successful, zips the resulting .class files into a .jar archive in the user's bot
          * directory (e.g. as "/Scalatron/users/{user}/bot/ScalatronBot.jar")
          * - deletes the temporary output directory
          *
          * @return a build result container, which specifies whether the build (compilation and
          *         .jar zipping) was successful, how many errors and warnings were seen, and the list of
          *         compiler messages (which include line and column number information).
          * @throws IllegalStateException if compilation service is unavailable, sources don't exist etc.
          * @throws IOError if source files cannot be read from disk, etc.
          */
        def buildSourceFiles(sourceFiles: SourceFileCollection): BuildResult

        /** Builds a local (unpublished) .jar bot plug-in from the sources currently present in
          * the user's workspace.
          *
          * Internals:
          * - patches up the package statements to make the fully-qualified class names user-specific
          * - enumerates the source files in the source directory of the current user (all files
          * residing in e.g. "/Scalatron/users/{user}/src")
          * - compiles those source files into a temporary output directory (e.g. "/Scalatron/users/{user}/out")
          * - if successful, zips the resulting .class files into a .jar archive in the user's bot
          * directory (e.g. as "/Scalatron/users/{user}/bot/ScalatronBot.jar")
          * - deletes the temporary output directory
          *
          * @return a build result container, which specifies whether the build (compilation and
          *         .jar zipping) was successful, how many errors and warnings were seen, and the list of
          *         compiler messages (which include line and column number information).
          * @throws IllegalStateException if compilation service is unavailable, sources don't exist etc.
          * @throws IOError if source files cannot be read from disk, etc.
          */
        def buildSources(): BuildResult

        /** Returns the path at which the API expects the local (unpublished) .jar bot plug-in
          * to reside, e.g. at "/Scalatron/users/{user}/bot/ScalatronBot.jar"
          *
          * This is intended for use by the web ui or custom tools using the web API when a
          * user wants to upload an already-built bot plug-in. Stream the plug-in .jar file to
          * this location, then publish into the tournament or start a private game. */
        def unpublishedBotPluginPath: String


        //----------------------------------------------------------------------------------------------
        // version control & sample bots
        //----------------------------------------------------------------------------------------------

        /** Returns the git repository associated with this user. You'll only need to use this if you want to
          * manipulate the underlying git repository yourself, e.g. to provide web access. */
        def gitRepository: org.eclipse.jgit.lib.Repository


        /** Returns a sorted collection of versions, sorted by age, with newest version first.
          * @throws IOError if version directory cannot be read from disk, etc.
          */
        def versions: Iterable[Version]

        /** Returns the newest version, if any. */
        def latestVersion: Option[Version] = versions.headOption

        /** Returns the version with the given ID, as a Some(Version) if it exists or None if it
          * does not exist. **/
        def version(id: String): Option[Version]

        /** Creates a new version with the given label from the files currently present in the workspace (/src directory)
          * if any changes were made to them.
          * @param label an optional label to apply to the version (may be empty).
          * @throws IllegalStateException if something went wrong internally with the version control system
          * @throws IOError if source files cannot be written to disk, etc.
          * @return Some(version) if a version was created, None otherwise.
          **/
        def createVersion(label: String): Option[Version]

        /*
                /** Creates a new version with the given label if any changes were made. Creating a version overwrites the
                  * source files that are currently in the user's working directory, then generates a new version from them.
                  * @param label an optional label to apply to the version (may be empty).
                  * @param sourceFiles the source files that will be stored in the version.
                  * @throws IllegalStateException if version (base) directory could not be created
                  * @throws IOError if source files cannot be written to disk, etc.
                  * */
                @deprecated("use udateSources() and createVersion() separately")
                def updateSourcesAndCreateVersion(label: String, sourceFiles: SourceFileCollection): Option[Version]

                /** Creates a new version by storing a backup copy of the source files currently present in the source
                  * code directory of the user if the given version creation policy requires it. This method is intended as
                  * a convenience call you can perform before overwriting the source code in the user's workspace with
                  * new, updated source files.
                  * The function implements the following logic:
                  * - if policy == Always, a backup version is created, no matter what
                  * - if policy == Never, no backup version is created, no matter what
                  * - if policy == IfDifferent, a backup version is created if and only if:
                  *     (a) the current version is different from the incoming, updated version AND
                  *     (b) the current version is different from the latest version in the history, or the history is empty
                  * @param policy the version creation policy: IfDifferent, Always, Never.
                  * @param label an optional label to apply to the version IF it is created (may be empty).
                  * @param b the incoming, updated source files that will be used to determine whether the files on disk are
                  *                    different in the policy "IfDifferent". Note that theses are NOT the files that
                  *                    will be stored in the version!
                  * @return an optional Version object, valid if a version was actually created.
                  * @throws IllegalStateException if version (base) directory could not be created or source directory could not be read.
                  * @throws IOError if source files cannot be written to disk, etc.
                  * */
                @deprecated("obsolete; use udateSources() and createVersion() separately")
                def createBackupVersion(policy: VersionPolicy, label: String, b: SourceFileCollection): Option[Version]
        */


        //----------------------------------------------------------------------------------------------
        // tournament management
        //----------------------------------------------------------------------------------------------

        /** Publishes the already compiled-and-zipped bot of the given user into the tournament bot
          * directory by copying the file from the user's bot directory (e.g. from
          * "/Scalatron/users/{user}/bot/ScalatronBot.jar") into the user's tournament plug-in
          * directory (e.g. to "/Scalatron/bots/{user}/ScalatronBot.jar"), where the tournament
          * game server will automatically pick it up once the next tournament round is started.
          * @throws IllegalStateException if the old plug-in file could not be backed up or the backup deleted
          * @throws IOError if the unpublished plug-in file could not be copied
          */
        def publish()


        //----------------------------------------------------------------------------------------------
        // sandbox management
        //----------------------------------------------------------------------------------------------

        /** Creates a new private simulation instance for this user. The returned instance is a
          * container for a unique sandbox ID and the initial state of the simulation generated for
          * that sandbox. See the notes on trait Sandbox for some caveats.
          *
          * The simulation will attempt to load the local version of the user's compiled-and-zipped
          * plug-in (e.g. from "/Scalatron/users/{user}/bot/Scalatron.jar").
          *
          * The sandboxed simulation can be configured by passing in a map of command line options, such as
          * Map("-steps" -> "1000", "-x" -> "50", "-y" -> "50")
          */
        def createSandbox(argMap: Map[String, String] = Map.empty): Sandbox
    }


    /** Scalatron.SourceFile: trait that provides an interface for dealing with source code
      * files handed through the API.
      * CBB: do we need to specify some particular kind of encoding?
      * @param filename the file name of the source file (e.g. "Bot.scala").
      *                 The extension is arbitrary, but will usually be ".scala".
      * @param code the source code text associated with this source file.
      */
    case class SourceFile(filename: String, code: String)
    {
        require(!filename.startsWith("/")) // minimize security issues
        require(!filename.contains("..")) // minimize security issues
    }


    /** Type alias for a collection of source files; each holds a filename and code. */
    type SourceFileCollection = Iterable[SourceFile]

    /** Utility methods for working with collections of source files. */
    object SourceFileCollection
    {
        /** Returns an initial source file collection that incorporates the given user name.
          * @param userName the name of the user to generate an initial source file collection for.
          * @return a non-empty collection of SourceFile objects
          */
        def initial(userName: String): SourceFileCollection =
            Iterable(SourceFile(
                "Bot.scala",
                "// this is the source code for your bot - have fun!\n" +
                    "\n" +
                    "class ControlFunctionFactory {\n" +
                    "    def create = new Bot().respond _\n" +
                    "}\n" +
                    "\n" +
                    "class Bot {\n" +
                    "    def respond(input: String) = \"Status(text=" + userName + ")\"\n" +
                    "}\n"
            ))


        def areEqual(as: SourceFileCollection, bs: SourceFileCollection, verbose: Boolean): Boolean =
            (as.size == bs.size) && // same number of files?
                as.forall(a => {
                    bs.find(_.filename == a.filename) match {
                        case None =>
                            if(verbose) println("  SourceFileCollection.areEqual: file exists in as but not bs: " + a.filename)
                            false // file present in a, but not in b => different

                        case Some(b) =>
                            if(verbose) {
                                // 2012-05-04 temp debugging information
                                println("  SourceFileCollection.areEqual: file still exists: " + a.filename)
                                if(b.code != a.code) {
                                    println("   file contents different")

                                    if(b.code.length != a.code.length) {
                                        println("   file sizes are different: a %d vs b %d".format(a.code.length, b.code.length))
                                    }

                                    val aLineCount = a.code.lines.length
                                    val bLineCount = b.code.lines.length
                                    if(bLineCount > aLineCount) {
                                        println("   file b contains more lines: b %d vs a %d".format(bLineCount, aLineCount))
                                        val bLines = b.code.lines
                                        val aLines = a.code.lines
                                        val linesWithIndex = bLines.zip(aLines).zipWithIndex.map(l => "%04d: %60s %60s".format(l._2, l._1._1, l._1._2))
                                        println(linesWithIndex.mkString("\n"))
                                    } else
                                    if(bLineCount < aLineCount) {
                                        println("   file a contains more lines: b %d vs a %d".format(bLineCount, aLineCount))
                                    } else {
                                        println("   files a and b contain same number of lines")
                                    }
                                }
                            }

                            a.code == b.code // file present in a and b, so compare code
                    }
                })

        /** Returns a collection of SourceFile objects representing source code files residing in the given directory.
          * Excludes directories and files that have the same name as the config files ("config.txt").
          * If the directory does not exist, is empty or does not contain valid files, an empty collection is returned.
          * @param directoryPath the directory to scan for source files.
          * @param verbose if true, information about the files read is logged to the console.
          * @return a collection of SourceFile objects; may be empty
          * @throws IOError on IO errors encountered while loading source file contents from disk
          */
        def loadFrom(directoryPath: String, verbose: Boolean = false): SourceFileCollection = {
            val directory = new File(directoryPath)
            if(!directory.exists) {
                System.err.println("warning: directory expected to contain source files does not exist: %s".format(directoryPath))
                Iterable.empty
            }
            else {
                val sourceFiles = directory.listFiles()
                if(sourceFiles == null || sourceFiles.isEmpty) {
                    // no source files there!
                    Iterable.empty
                } else {
                    // read whatever is on disk now
                    sourceFiles
                    .filter(file => file.isFile && file.getName != Constants.ConfigFilename)
                    .map(file => {
                        val filename = file.getName
                        val filePath = file.getAbsolutePath
                        val code = FileUtil.loadTextFileContents(filePath)
                        if(verbose) println("loaded source code from file: '%s'".format(filePath))
                        SourceFile(filename, code)
                    })
                }
            }
        }


        /** Writes the given collection of source files into the given directory, which must exist
          * and should be empty. Throws an exception on IO errors.
          * @param directoryPath the directory to write the source files to.
          * @param sourceFileCollection the collection of source files to write to disk.
          * @param verbose if true, information about the written files is logged to the console.
          * @throws IOError on IO errors encountered while writing source file contents to disk.
          */
        def writeTo(directoryPath: String, sourceFileCollection: SourceFileCollection, verbose: Boolean = false) {
            sourceFileCollection.foreach(sf => {
                val path = directoryPath + "/" + sf.filename
                use(new FileWriter(path)) {_.append(sf.code)}
                if(verbose) println("wrote source file: " + path)
            })
        }
    }


    /** Policy used for optional version generation (e.g. when uploading new source files) that dictates whether
      * and under which circumstances a backup version should be generated.
      */
    sealed trait VersionPolicy

    object VersionPolicy
    {

        case object IfDifferent extends VersionPolicy

        // create version if new and old files differ
        case object Never extends VersionPolicy

        // don't create a version, even if new and old files differ
        case object Always extends VersionPolicy

        // always create a version, even if new and old files do not differ
    }


    /** A container for build results that can be reported back to a user. Contains a flag
      * indicating whether the build was successful (primarily: zero errors and not aborted),
      * the count of errors and warnings, and a collection of build message objects.
      * The duration is the time actually spent building the files, excluding the queue wait time. */
    case class BuildResult(
        successful: Boolean,
        duration: Int,
        errorCount: Int,
        warningCount: Int,
        messages: Iterable[BuildResult.BuildMessage])

    object BuildResult
    {

        /** A single build message, such as an error or a warning.
          * @param sourceFile the source file in which the error occurred. The path is relative
          *                   to the user's /src directory, e.g. /Scalatron/users/{user}/
          * @param lineAndColumn the line and column numbers where the error occurred
          * @param multiLineMessage a (potentially) multi-line message explaining the error
          * @param severity the severity, 2 = error, 1 = warning, 0 = info
          */
        case class BuildMessage(sourceFile: String, lineAndColumn: (Int, Int), multiLineMessage: String, severity: Int)

        val Severity_Error = 2
        val Severity_Warning = 1
    }


    /** Scalatron.Sample: interface for dealing with source code samples.
      */
    trait Sample
    {
        /** Returns the name of this sample, e.g. "Tutorial Bot 01".
          * The name derives from the name of the associated directory in the /sample base
          * directory on disk, e.g. "/Scalatron/samples/Tutorial Bot 01". */
        def name: String

        /** Returns the source code files associated with this sample.
          * @throws IOError on IO errors encountered while loading source file contents from disk
          */
        def sourceFiles: SourceFileCollection

        /** Deletes this sample, including all associated source code files.
          * @throws IOError if sample's source files cannot be deleted on disk
          */
        def delete()
    }


    /** Scalatron.Version: interface for dealing with source code versions of a user.
      */
    trait Version
    {
        /** Returns the user-unique version ID of this version. */
        def id: String

        /** Returns the label string of this version. */
        def label: String

        /** Returns the date of this version, namely as the number of milliseconds since the
          * standard base time known as "the epoch", namely January 1, 1970, 00:00:00 GMT. */
        def date: Long

        /** Returns the user object of the user that owns this version. */
        def user: User

        /** Restores the contents of the source directory to this version.
          * @throws IOError if the repository is in a corrupt state.
          */
        def restore()
    }


    /** Scalatron.Sandbox encapsulates a private sandboxed simulation with a particular ID. More specifically, it
      * associates a unique sandbox ID with the first state of a simulation. To run the simulation, fetch the
      * initial state and simulate from there.
      * Caution: at the moment, certain plug-ins used by the simulation may not be stateless, in which case
      * fetching multiple successor states from the same simulation state will lead to unpredictable results.
      */
    trait Sandbox
    {
        /** Returns the user object with which this sandbox is associated. */
        def user: User

        /** Returns the unique id this sandbox. */
        def id: Int

        /** Returns the initial state of this sandboxed game, corresponding to time zero.
          * Successor states can subsequently be computed using state.step().
          * */
        def initialState: SandboxState
    }


    /** Scalatron.SandboxState encapsulates a particular state in time of a user's private, sandboxed simulation.
      * Caution: at the moment, certain plug-ins used by the simulation may not be stateless, in which case
      * fetching multiple successor states from the same simulation state will lead to unpredictable results.
      */
    trait SandboxState
    {
        /** Returns the sandbox instance with which this state is associated. */
        def sandbox: Sandbox

        /** Returns the time (i.e., the step count) of this simulation state. */
        def time: Int

        /** Computes the next simulation step and returns the resulting sandbox state. */
        def step: SandboxState = step(1)

        /** Computes the given number of simulation steps and returns the resulting sandbox state. */
        def step(count: Int): SandboxState

        /** Returns a list of all entities (bots and mini-bots) owned by the user associated with
          * this sandbox. */
        def entities: Iterable[SandboxEntity]
    }


    trait SandboxEntity
    {
        /** Returns the unique ID of this entity. */
        def id: Int

        /** Returns the unique name of this entity, e.g. "Master" or "Slave_12345". */
        def name: String

        /** Returns true if this entity is a master (bot), false if it is a slave (mini-bot). */
        def isMaster: Boolean

        /** Returns the input string that was most recently received by the control function of
          * this entity. Note that for Master bots this may be out of date, since they are only
          * called every second cycle. */
        def mostRecentControlFunctionInput: String

        /** Returns the list of commands that was most recently returned by the control function
          * of this entity. Note that for Master bots this may be out of date, since they are only
          * called every second cycle. Also note that this is NOT the command string returned by
          * the bot; rather, it is the collection of commands actually recognized and accepted by
          * the server. The returned structure is: Iterable[(opcode,Iterable[(paramName,value])]
          */
        def mostRecentControlFunctionOutput: Iterable[(String, Iterable[(String, String)])]

        /** Returns the debug log output most recently made by the bot. */
        def debugOutput: String
    }


    object Constants
    {
        /** Name of the configuration file that resides in each web user's directory, containing
          * password, editor theme, etc.  */
        val ConfigFilename = "config.txt"
        val PasswordKey = "password"

        /** Hard-wired: user name of the web-based administrator account. */
        val AdminUserName = "Administrator"
        val AdminDefaultPassword = "" // empty password -- allow for login / can be changed later

        val TournamentBotsDirectoryName = "bots"
        // e.g. in "/Scalatron/bots"
        val UsersDirectoryName = "users"
        // e.g. in "/Scalatron/users"
        val UsersOutputDirectoryName = "out"
        // e.g. in "/Scalatron/users/{user}/out"
        val UsersBotDirectoryName = "bot"
        // e.g. in "/Scalatron/users/{user}/bot"
        val UsersSourceDirectoryName = "src"
        // e.g. in "/Scalatron/users/{user}/src"
        val UsersPatchedSourceDirectoryName = "patched"
        // e.g. in "/Scalatron/users/{user}/patched"
        val UsersVersionsDirectoryName = "versions"
        // e.g. in "/Scalatron/users/{user}/versions"
        val UsersSourceFileName = "Bot.scala"
        // e.g. in "/Scalatron/users/{user}/src/Bot.scala"
        val SamplesDirectoryName = "samples"
        // e.g. in "/Scalatron/samples"
        val SamplesSourceDirectoryName = "src" // e.g. in "/Scalatron/samples/{sample}/src"

        val gitDirectoryName = ".git"
    }


}

