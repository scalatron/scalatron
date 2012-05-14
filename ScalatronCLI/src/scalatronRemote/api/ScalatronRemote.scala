/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatronRemote.api

import ScalatronRemote._
import scalatronRemote.impl.ScalatronRemoteImpl
import scala.io.Source
import java.io.{FileWriter, File}


/** Client-side interface for the Scalatron RESTful web API.
  */
trait ScalatronRemote {
    //----------------------------------------------------------------------------------------------
    // version
    //----------------------------------------------------------------------------------------------

    /** Returns the version of the remote Scalatron server exposing this API, e.g. "0.9.7". */
    def version: String

    def hostname: String
    def port: Int


    //----------------------------------------------------------------------------------------------
    // (web) user management
    //----------------------------------------------------------------------------------------------

    /** Returns a list of user accounts. */
    def users(): UserList

    /** Creates a new user account for a user with the given name, assigning it the given
      * password. Creates a source code directory for the user and populates it with default
      * source files.
      * @throws ScalatronException.NotAuthorized if not logged on as Administrator
      * @throws ScalatronException.IllegalUserName if the user name contained illegal characters
      * @throws ScalatronException.Exists if a user with the given name already exists
      * @throws ScalatronException.CreationFailure if the user could not be created (e.g. because /src dir could not be created).
      */
    def createUser(name: String, password: String): User



    //----------------------------------------------------------------------------------------------
    // sample code management
    //----------------------------------------------------------------------------------------------

    /** Returns a collection of samples, i.e. of named source file packages, such as bot versions
      * taken from the Scalatron tutorial. The samples are enumerated by enumerating the
      * sub-directories of the /sample base directory on disk, e.g. "/Scalatron/samples/{sample}". */
    def samples: SampleList

    /** Creates a new sample with the given name from the given source file collection.
      * Users can employ this to share bot code across the network. */
    def createSample(name: String, sourceFiles: SourceFileCollection): Sample
}

object ScalatronRemote {

    object Constants {
        val DefaultPort = 8080
        val ApiEntryPoint = "/api"
        val AdminUserName = "Administrator"
        val PasswordKey = "password"
    }

    /** Specific exceptions thrown by Scalatron Remote API. */
    class ScalatronException(msg: String) extends RuntimeException(msg)

    object ScalatronException {

        /** ScalatronException.Forbidden: if an operation is forbidden, such as deleting the Administrator account. */
        case class Forbidden(serverMessage: String) extends ScalatronException(serverMessage)

        /** ScalatronException.NotAuthorized: unable to perform the action because of insufficient privileges. */
        case class NotAuthorized(serverMessage: String) extends ScalatronException(serverMessage)

        /** ScalatronException.IllegalUserName: user name contained illegal characters. */
        case class IllegalUserName(serverMessage: String) extends ScalatronException(serverMessage)

        /** ScalatronException.Exists: if an user/version/sample/etc. with the given id/name already exists. */
        case class Exists(serverMessage: String) extends ScalatronException(serverMessage)

        /** ScalatronException.NotFound: if an user/version/sample/etc. with the given id/name does not exist. */
        case class NotFound(serverMessage: String) extends ScalatronException(serverMessage)

        /** ScalatronException.CreationFailure: if a user/version/sample/etc. could not be created. */
        case class CreationFailed(serverMessage: String) extends ScalatronException(serverMessage)

        /** ScalatronException.InternalServerError: if the server could not execute the request,
          * e.g. a user cannot be logged on because the configuration file for the user is invalid. */
        case class InternalServerError(serverMessage: String) extends ScalatronException(serverMessage)

    }

    case class ConnectionConfig(hostname: String, port: Int, apiEntryPoint: String, verbose: Boolean)

    def apply(connectionConfig: ConnectionConfig): ScalatronRemote =
        ScalatronRemoteImpl(connectionConfig)

    def apply(hostname: String, port: Int, entryPoint: String, verbose: Boolean = false): ScalatronRemote =
        ScalatronRemoteImpl(ConnectionConfig(hostname, port, entryPoint, verbose))


    trait UserList extends Iterable[User] {
        /** Retrieves the user account with the given name. Returns a Some(User) if an account for
          * the given name exists and None if it does not.
          */
        def get(name: String): Option[User]

        /** Retrieves the Administrator user account with the given name.
          * Fails with an IllegalStateException if 'Administrator' is not in the user list.
          */
        def adminUser: User
    }


    trait User {
        //----------------------------------------------------------------------------------------------
        // authentication
        //----------------------------------------------------------------------------------------------

        /** Logs this user on with the given password.
          * @param password the password to use for logging on
          * @throws ScalatronException.NotAuthorized wrong password for this user
          * @throws ScalatronException.NotFound no user with this user name exists
          * @throws ScalatronException.InternalServerError if user configuration file could not be read
          */
        def logOn(password: String)

        def logOff()


        //----------------------------------------------------------------------------------------------
        // account management
        //----------------------------------------------------------------------------------------------

        def name: String

        def isAdministrator: Boolean

        /** Deletes the user account on the server. CAUTION: all user data is deleted.
          * This user object becomes unusable.
          * @throws ScalatronException.NotAuthorized if not logged on as this user or as Administrator
          * @throws ScalatronException.NotFound if user does not exist on the server
          * @throws ScalatronException.IllegalUsername if user name contained invalid characters
          * @throws ScalatronException.InternalServerError if user's workspace files could not be deleted
          */
        def delete()

        /** Updates the configuration attribute map of the user.
          * Values present in the given map but not in the config file are added.
          * Values present in the given map and in the config file are updated.
          * Values present in the config files but not in the given map are retained.
          * @throws ScalatronException.NotAuthorized if not logged on as this user or as Administrator
          * @throws ScalatronException.NotFound if user does not exist on the server
          * @throws ScalatronException.IllegalUsername if user name contained invalid characters
          * @throws ScalatronException.InternalServerError if user's configuration file could not be updated
          */
        def updateAttributes(map: Map[String, String])

        /** Returns the configuration attribute map for this user or None if no such map exists.
          * Note: the 'password' key will be suppressed by the server and not returned to the client.
          * @throws ScalatronException.NotAuthorized if not logged on as this user or as Administrator
          * @throws ScalatronException.NotFound if user does not exist on the server
          * @throws ScalatronException.IllegalUsername if user name contained invalid characters
          * @throws ScalatronException.InternalServerError if user's configuration attributes are invalid
          */
        def getAttributeMap: Map[String, String]

        /** Sets the password of the given user to the given string.
          * @throws ScalatronException.NotAuthorized if not logged on as this user or as Administrator
          * @throws ScalatronException.NotFound if user does not exist on the server
          * @throws ScalatronException.IllegalUsername if user name contained invalid characters
          * @throws ScalatronException.InternalServerError if user's configuration file could not be updated
          */
        def setPassword(password: String) {
            updateAttributes(Map(Constants.PasswordKey -> password))
        }


        //----------------------------------------------------------------------------------------------
        // source code & build management
        //----------------------------------------------------------------------------------------------

        /** Returns the source files currently present in the user's source code directory
          * @throws ScalatronException.NotAuthorized if not logged on as this user or as Administrator
          * @throws ScalatronException.NotFound if user does not exist on the server
          * @throws ScalatronException.InternalServerError if user's source files could not be read
          */
        def sourceFiles: SourceFileCollection

        /** Updates the sources of the given user to the given source files.
          * @throws ScalatronException.NotAuthorized if not logged on as this user or as Administrator
          * @throws ScalatronException.NotFound if user does not exist on the server
          * @throws ScalatronException.InternalServerError if user's source files could not be written
          */
        def updateSourceFiles(sourceFileCollection: SourceFileCollection)

        /** Builds a local (unpublished) .jar bot plug-in from the sources currently present in
          * the user's workspace.
          * @throws ScalatronException.NotAuthorized if not logged on as this user or as Administrator
          * @throws ScalatronException.NotFound if user does not exist on the server
          * @throws ScalatronException.InternalServerError if compile service could not be started, or disk error, etc.
          */
        def buildSources(): BuildResult


        //----------------------------------------------------------------------------------------------
        // version control & sample bots
        //----------------------------------------------------------------------------------------------

        /** Returns a sorted collection of version identifiers of the bot sources, as tuples of
          * (id, label, date). The versions are enumerated by listing the directories below the
          * 'versions' directory, e.g. at "/Scalatron/users/{user}/versions/{versionId}"
          * @throws ScalatronException.NotAuthorized if not logged on as this user or as Administrator
          * @throws ScalatronException.NotFound if user does not exist on the server
          * @throws ScalatronException.InternalServerError if version list could not be read from disk
          */
        def versions: Iterable[Version]

        /** Returns the version with the given ID, as a Some(Version) if it exists or None if it
          * does not exist. Will attempt to locate the version in the appropriate directory below
          * the 'versions' directory, e.g. at "/Scalatron/users/{user}/versions/{versionId}".
          * Note that the current implementation calls 'versions()' to fetch the list of versions,
          * so if you already have that, you are better of finding the version yourself.
          * */
        def version(id: String): Option[Version]

        /** Creates a new version by storing the given source files into a version directory
          * below the 'versions' directory. */
        def createVersion(label: String, sourceFileCollection: SourceFileCollection): Version


        //----------------------------------------------------------------------------------------------
        // tournament management
        //----------------------------------------------------------------------------------------------

        /** Publishes the already compiled-and-zipped bot of the given user into the tournament bot
          * directory by copying the file from the user's bot directory (e.g. from
          * "/Scalatron/users/{user}/bot/ScalatronBot.jar") into the user's tournament plug-in
          * directory (e.g. to "/Scalatron/bots/{user}/ScalatronBot.jar"), where the tournament
          * game server will automatically pick it up once the next tournament round is started.
          */
        def publish()


        //----------------------------------------------------------------------------------------------
        // sandbox management
        //----------------------------------------------------------------------------------------------

        /** Starts a new, headless Scalatron BotWar game in a user-specific sandbox and returns
          * a wrapper for the simulation state, which here contains the initial simulation state.
          * Successor states can subsequently be computed using state.step(). The game
          * will attempt to load the local version of the user's compiled-and-zipped plug-in (e.g. from
          * "/Scalatron/users/{user}/bot/Scalatron.jar").
          *
          * The game can be configured by passing in a map of command line options, such as
          * Map("-steps" -> "1000", "-x" -> "50", "-y" -> "50")
          */
        def createSandbox(argMap: Map[String, String] = Map.empty): Sandbox
    }


    /** ScalatronRemote.SourceFile: trait that provides an interface for dealing with source code
      * files handed through the API.
      * CBB: do we need to specify some particular kind of encoding?
      * @param filename the file name of the source file (e.g. "Bot.scala").
      *                 The extension is arbitrary, but will usually be ".scala".
      * @param code the source code text associated with this source file.
      */
    case class SourceFile(filename: String, code: String) {
        require(!filename.startsWith("/")) // minimize security issues
        require(!filename.contains("..")) // minimize security issues
    }



    /** Type alias for a collection of source files; each holds a filename and code. */
    type SourceFileCollection = Iterable[SourceFile]

    /** Utility methods for working with collections of source files. */
    object SourceFileCollection
    {
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
                System.err.println("error: directory expected to contain source files does not exist: %s".format(directoryPath))
                System.exit(-1)
            }

            val sourceFileList = directory.listFiles()
            if( sourceFileList == null || sourceFileList.isEmpty ) {
                // no source files there!
                System.err.println("error: local source directory is empty: '%s'".format(directoryPath))
                System.exit(-1)
            }

            // read whatever is on disk now
            sourceFileList
                .filter(file => file.isFile)
                .map(file => {
                val filename = file.getName
                val code = Source.fromFile(file).mkString
                if(verbose) println("loaded source code from file: '%s'".format(file.getAbsolutePath))
                SourceFile(filename, code)
            })
        }


        /** Writes the given collection of source files into the given directory, which must exist
          * and should be empty. Throws an exception on IO errors.
          * @param directoryPath the directory to write the source files to.
          * @param sourceFileCollection the collection of source files to write to disk.
          * @param verbose if true, information about the written files is logged to the console.
          * @throws IOError on IO errors encountered while writing source file contents to disk.
          */
        def writeTo(directoryPath: String, sourceFileCollection: SourceFileCollection, verbose: Boolean = false) {
            val targetDir = new File(directoryPath)
            if(!targetDir.exists()) {
                if(!targetDir.mkdirs()) {
                    System.err.println("error: cannot create local directory '%s'".format(directoryPath))
                    System.exit(-1)
                }
            }

            sourceFileCollection.foreach(sf => {
                val path = directoryPath + "/" + sf.filename
                val sourceFile = new FileWriter(path)
                sourceFile.append(sf.code)
                sourceFile.close()
                if(verbose) println("wrote source file: " + path)
            })
        }
    }





    /** A container for build results that can be reported back to a user. Contains a flag
      * indicating whether the build was successful (primarily: zero errors and not aborted),
      * the count of errors and warnings, and a collection of build message objects. */
    case class BuildResult(
        successful: Boolean,
        duration: Int,          // milliseconds
        errorCount: Int,
        warningCount: Int,
        messages: Iterable[BuildResult.BuildMessage])

    object BuildResult {

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



    trait SampleList extends Iterable[Sample] {
        /** Returns the Sample instance associated with a particular name, which can then be used to
          * retrieve the source file collection associated with the sample or to delete the sample.
          * Returns Some(Sample) if a sample with the given name exists, None otherwise. */
        def get(name: String): Option[Sample]
    }



    /** ScalatronRemote.Sample: interface for dealing with source code samples.
      */
    trait Sample {
        /** Returns the name of this sample, e.g. "Tutorial Bot 01".
          * The name derives from the name of the associated directory in the /sample base
          * directory on disk, e.g. "/Scalatron/samples/Tutorial Bot 01". */
        def name: String

        /** Returns the source code files associated with this sample. */
        def sourceFiles: SourceFileCollection

        /** Deletes this sample, including all associated source code files. */
        def delete()
    }


    /** ScalatronRemote.Version: interface for dealing with source code versions of a user.
      */
    trait Version {
        /** Returns the user-unique version ID of this version. */
        def id: String

        /** Returns the label string of this version. */
        def label: String

        /** Returns the date of this version, namely as the number of milliseconds since the
          * standard base time known as "the epoch", namely January 1, 1970, 00:00:00 GMT. */
        def date: Long

        /** Returns the user object of the user that owns this version. */
        def user: User

        /** Returns the source code files associated with this version. */
        def sourceFiles: SourceFileCollection
    }


    /** ScalatronRemote.Sandbox encapsulates a private sandboxed game with a particular ID.
      * Note that while the API is designed to allow for multiple currently valid sandboxes with unique IDs
      * and access to arbitrary times, the current server implementation (v0.9.8.4) only retains a single state (time)
      * of a single sandbox - the most recently created sandbox with the most recently returned state.
      * So while that is the case, don't retain a sandbox reference once you created a new one - it won't be a valid
      * resource any more.
      */
    trait Sandbox {
        /** Returns the user object with which this sandbox is associated. */
        def user: User

        /** Returns the unique id this sandbox. */
        def id: Int

        /** Returns the initial state of this sandboxed game, corresponding to time zero. */
        def initialState: SandboxState

        /** Deletes this sandbox on the server. Note that the current implementation (0.9.8.4) calls the API
          * that deletes all sandboxes on the server - not a big issue, since there is only this one. */
        def delete()
    }


    /** ScalatronRemote.SandboxState encapsulates the state of a web user's private sandbox game
      * simulation.
      */
    trait SandboxState {
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


    case class Command(opcode: String, params: Map[String, String])

    trait SandboxEntity {
        /** Returns the unique ID of this entity. */
        def id: Int

        /** Returns the unique name of this entity, e.g. "Master" or "Slave_12345". */
        def name: String

        /** Returns true if this entity is a master (bot), false if it is a slave (mini-bot). */
        def isMaster: Boolean

        /** Returns the input string that was most recently received by the control function of
          * this entity. Note that for Master bots this may be out of date, since they are only
          * called every second cycle. */
        def mostRecentControlFunctionInput: Command

        /** Returns the list of commands that was most recently returned by the control function
          * of this entity. Note that for Master bots this may be out of date, since they are only
          * called every second cycle. Also note that this is NOT the command string returned by
          * the bot; rather, it is the collection of commands actually recognized and accepted by
          * the server. The returned structure is: Iterable[(opcode,Iterable[(paramName,value])]
          */
        def mostRecentControlFunctionOutput: Iterable[Command]

        /** Returns the debug log output most recently made by the bot. */
        def debugOutput: String
    }

}
