/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */

package scalatronCLI.cmdline

import scalatronRemote.api.ScalatronRemote
import scalatronRemote.api.ScalatronRemote.{ScalatronException, ConnectionConfig}
import java.io.{FileWriter, File}
import io.Source
import scalatronRemote.Version
import java.text.DateFormat
import java.util.Date


/** A command line interface for interaction with a remote Scalatron server over the RESTful HTTP API.
  */
object CommandLineProcessor {
    def apply(args: Array[String]) {
        if(args.length == 0) {
            println("Scalatron Remote Command Line Client " + Version.VersionString)
            println("use -help to list available command line parameters")
            println("e.g. java -jar ScalatronTest.jar -help")
            System.exit(0)
        } else
        if(args.find(_ == "-help").isDefined) {
            // print list of available command line parameters to the console
            println("Scalatron Remote Command Line Client " + Version.VersionString)
            println("invocation syntax:")
            println("   java -jar ScalatronTest.jar -help")
            println("or")
            println("   java -jar ScalatronTest.jar [-key value] [-key value] [...]")
            println("")
            println("where [-key value] corresponds to one of the following key/value pairs:")
            println("   -verbose yes|no     print verbose output (default: no)")
            println("   -api <string>       the relative path of the server api (default: " + ScalatronRemote.Constants.ApiEntryPoint + ")")
            println("   -port <int>         the port the server is listening on (default: " + ScalatronRemote.Constants.DefaultPort + ")")
            println("   -hostname <name>    the hostname of the server (default: localhost)")
            println("   -user <name>        the user name to log on as (default: " + ScalatronRemote.Constants.AdminUserName + ")")
            println("   -password <string>  the password to use for log on (default: empty password)")
            println("   -cmd <command>")
            println("")
            println("where <command> may require addition parameters:")
            println("   users                       lists all users; does not require logon")
            println("")
            println("   createUser                  create new user; as Administrator only")
            println("       -targetUser <name>      the user name for the new user (required)")
            println("       -newPassword <string>   the password for the new user (default: empty password)")
            println("")
            println("   deleteUser                  deletes an existing user (along with all content!); Administrator only")
            println("       -targetUser <name>      the name of the user to delete (required)")
            println("")
            println("   setUserAttribute            sets a configuration attribute for a user; as user or Administrator")
            println("       -targetUser <name>      the name of the user to set attribute on (default: name of '-user' option)")
            println("       -key <name>             the key of the attribute to set")
            println("       -value <name>           the value of the attribute to set")
            println("")
            println("   getUserAttribute            gets a configuration attribute from a user; as user or Administrator")
            println("       -targetUser <name>      the name of the user to get attribute from (default: name of '-user' option)")
            println("       -key <name>             the key of the attribute to set")
            println("")
            println("   sources                     gets the source files from a user's server workspace; as user only")
            println("       -targetDir <path>       the path of the local directory where the source files should be stored")
            println("")
            println("   updateSources               updates a source files in the user's server workspace; as user only")
            println("       -sourceDir <path>       the path of the local directory where the source files can be found")
            println("")
            println("   build                       builds the source files currently in the user's server workspace; as user only")
            println("")
            println("   versions                    lists all versions available in the user workspace; as user only")
            println("")
            println("   createVersion               creates a new version in the user's server workspace; as user only")
            println("       -sourceDir <path>       the path of the local directory where the source files can be found")
            println("       -label <name>           the label to apply to the versions (default: empty)")
            println("")
            println("   benchmark                   runs standard isolated-bot benchmark on given source files; as user only")
            println("       -sourceDir <path>       the path of the local directory where the source files can be found")
            println("")
            println("Examples:")
            println(" java -jar ScalatronCLI.jar -cmd users")
            println(" java -jar ScalatronCLI.jar -user Administrator -password a -cmd createUser -targetUser Frankie")
            println(" java -jar ScalatronCLI.jar -user Administrator -password a -cmd setUserAttribute -targetUser Frankie -key theKey -value theValue")
            println(" java -jar ScalatronCLI.jar -user Administrator -password a -cmd getUserAttribute -targetUser Frankie -key theKey")
            println(" java -jar ScalatronCLI.jar -user Frankie -password a -cmd sources -targetDir /tempsrc")
            println(" java -jar ScalatronCLI.jar -user Frankie -password a -cmd updateSources -sourceDir /tempsrc")
            println(" java -jar ScalatronCLI.jar -user Frankie -password a -cmd build")
            println(" java -jar ScalatronCLI.jar -user Frankie -password a -cmd versions")
            println(" java -jar ScalatronCLI.jar -user Frankie -password a -cmd createVersion -sourceDir /tempsrc -label \"updated\"")
            println(" java -jar ScalatronCLI.jar -user Frankie -password a -cmd benchmark -sourceDir /tempsrc")
            System.exit(0)
        }

        // convert the command line parameters into a map of key/value pairs
        val argMap = args.grouped(2).filter(_.length == 2).map(a => (a(0), a(1))).toMap // Map["-key" -> "value"]

        // find out if we should provide verbose output
        val verbose = (argMap.get("-verbose").getOrElse("no") == "yes")

        // find out where we can connect to the server
        val webServerHostname = argMap.get("-hostname").getOrElse("localhost")
        val webServerPort = argMap.get("-port").map(_.toInt).getOrElse(ScalatronRemote.Constants.DefaultPort)
        val webServerApi = argMap.get("-api").getOrElse(ScalatronRemote.Constants.ApiEntryPoint)

        val connectionConfig = ConnectionConfig(webServerHostname, webServerPort, webServerApi, verbose)


        argMap.get("-cmd") match {
            case Some(command) =>
                try {
                    command match {
                        case "users" => cmd_users(connectionConfig)
                        case "createUser" => cmd_createUser(connectionConfig, argMap)
                        case "deleteUser" => cmd_deleteUser(connectionConfig, argMap)
                        case "setUserAttribute" => cmd_setUserAttribute(connectionConfig, argMap)
                        case "getUserAttribute" => cmd_getUserAttribute(connectionConfig, argMap)
                        case "sources" => cmd_sources(connectionConfig, argMap)
                        case "updateSources" => cmd_updateSources(connectionConfig, argMap)
                        case "build" => cmd_buildSources(connectionConfig, argMap)
                        case "versions" => cmd_versions(connectionConfig, argMap)
                        case "createVersion" => cmd_createVersion(connectionConfig, argMap)
                        case "benchmark" => cmd_benchmark(connectionConfig, argMap)
                        case _ => System.err.println("unknown command: " + command)
                    }
                } catch {
                    case t: Throwable =>
                        System.err.println("error: command '" + command + "' failed: " + t.toString)
                        System.exit(-1)
                }

            case None =>
                System.err.println("no command provided")
                System.exit(-1)
        }
    }



    //------------------------------------------------------------------------------------------------------------------
    // command handlers
    //------------------------------------------------------------------------------------------------------------------

    /** -command users          lists all users")
      */
    def cmd_users(connectionConfig: ConnectionConfig) {
        val scalatron = ScalatronRemote(connectionConfig)
        val users = scalatron.users()
        if(connectionConfig.verbose) println("Users on Scalatron server '" + scalatron.hostname + "':")
        users.foreach(u => println(u.name))
    }

    /** -command createUser          create new user
      * -targetUser name           the user name for the new user (required)
      * -newPassword string     the password for the new user (default: empty password)
      */
    def cmd_createUser(connectionConfig: ConnectionConfig, argMap: Map[String, String]) {
        argMap.get("-targetUser") match {
            case None =>
                System.err.println("error: command 'createUser' requires option '-targetUser'")
                System.exit(-1)

            case Some(targetUser) =>
                doAsAdministrator(
                    connectionConfig,
                    argMap,
                    (scalatron: ScalatronRemote, users: ScalatronRemote.UserList) => {
                        // create the new user account (1 round-trip)
                        handleScalatronExceptionsFor {
                            val newPassword = argMap.get("-newPassword").getOrElse("")
                            val user = scalatron.createUser(targetUser, newPassword)
                            if(connectionConfig.verbose) println("Created user '" + user.name + "' on the Scalatron server '" + scalatron.hostname + "'")
                        }
                    }
                )
        }
    }

    /** -command deleteUser          deletes an existing user (along with all content!); Administrator only
      * -targetUser name        the name of the user to delete (required)
      */
    def cmd_deleteUser(connectionConfig: ConnectionConfig, argMap: Map[String, String]) {
        argMap.get("-targetUser") match {
            case None =>
                System.err.println("error: command 'deleteUser' requires option '-targetUser'")
                System.exit(-1)

            case Some(targetUser) =>
                doAsAdministrator(
                    connectionConfig,
                    argMap,
                    (scalatron: ScalatronRemote, users: ScalatronRemote.UserList) => {
                        users.user(targetUser) match {
                            case None =>
                                System.err.println("error: user '" + targetUser + "' does not exist")
                                System.exit(-1)
                            case Some(user) =>
                                // create the new user account (1 round-trip)
                                handleScalatronExceptionsFor {
                                    user.delete()
                                    if(connectionConfig.verbose) println("Deleted user '" + user.name + "' on the Scalatron server '" + scalatron.hostname + "'")
                                }
                        }
                    }
                )
        }
    }


    /** -command setUserAttribute       sets a configuration attribute for a user; user or Administrator
      * -key name               the key of the attribute to set
      * -value name             the value of the attribute to set
      */
    def cmd_setUserAttribute(connectionConfig: ConnectionConfig, argMap: Map[String, String]) {
        val targetUserName: String = argMap.get("-targetUser") match {
            case None => argMap.getOrElse("-user", "")
            case Some(value) => value
        }
        if(targetUserName.isEmpty) {
            System.err.println("error: command 'setUserAttribute' requires option '-user' or '-targetUser'")
            System.exit(-1)
        }

        argMap.get("-key") match {
            case None =>
                System.err.println("error: command 'setUserAttribute' requires option '-key'")
                System.exit(-1)

            case Some(key) =>
                argMap.get("-value") match {
                    case None =>
                        System.err.println("error: command 'setUserAttribute' requires option '-value'")
                        System.exit(-1)

                    case Some(value) =>
                        doAsUser(
                            connectionConfig,
                            argMap,
                            (scalatron: ScalatronRemote, loggedonUser: ScalatronRemote.User, users: ScalatronRemote.UserList) => {
                                users.user(targetUserName) match {
                                    case None =>
                                        System.err.println("error: user '" + targetUserName + "' does not exist")
                                        System.exit(-1)
                                    case Some(targetUser) =>
                                        // set user attribute (1 round-trip)
                                        handleScalatronExceptionsFor {
                                            targetUser.updateAttributes(Map(key -> value))
                                            if(connectionConfig.verbose) println("Updated attribute '" + key + "' for user '" + targetUserName + "' on the Scalatron server '" + scalatron.hostname + "'")
                                        }
                                }
                            }
                        )
                }
        }
    }


    /** -command getUserAttribute       gets a configuration attribute of a user; user or Administrator
      * -key name               the key of the attribute to set
      */
    def cmd_getUserAttribute(connectionConfig: ConnectionConfig, argMap: Map[String, String]) {
        val targetUserName: String = argMap.get("-targetUser") match {
            case None => argMap.getOrElse("-user", "")
            case Some(value) => value
        }
        if(targetUserName.isEmpty) {
            System.err.println("error: command 'getUserAttribute' requires option '-user' or '-targetUser'")
            System.exit(-1)
        }

        argMap.get("-key") match {
            case None =>
                System.err.println("error: command 'getUserAttribute' requires option '-key'")
                System.exit(-1)

            case Some(key) =>
                doAsUser(
                    connectionConfig,
                    argMap,
                    (scalatron: ScalatronRemote, loggedonUser: ScalatronRemote.User, users: ScalatronRemote.UserList) => {
                        users.user(targetUserName) match {
                            case None =>
                                System.err.println("error: user '" + targetUserName + "' does not exist")
                                System.exit(-1)
                            case Some(targetUser) =>
                                // get user attributes (1 round-trip)
                                handleScalatronExceptionsFor {
                                    val attributeMap = targetUser.getAttributeMap
                                    attributeMap.get(key) match {
                                        case None =>
                                            // key is not defined
                                            System.err.println("no value is associated with key '" + key + "' for user '" + targetUserName + "'")
                                            System.exit(-2)
                                        case Some(value) =>
                                            println(value)
                                    }
                                }
                        }
                    }
                )
        }
    }


    /** -command sources         gets a source files from a user workspace; user or Administrator
      * -targetDir path         the path of the local directory where the source files should be stored
      */
    def cmd_sources(connectionConfig: ConnectionConfig, argMap: Map[String, String]) {
        argMap.get("-targetDir") match {
            case None =>
                System.err.println("error: command 'sources' requires option '-targetDir'")
                System.exit(-1)

            case Some(targetDirPath) =>
                doAsUser(
                    connectionConfig,
                    argMap,
                    (scalatron: ScalatronRemote, loggedonUser: ScalatronRemote.User, users: ScalatronRemote.UserList) => {
                        // get user source files (1 round-trip)
                        handleScalatronExceptionsFor {
                            val targetDir = new File(targetDirPath)
                            if(!targetDir.exists()) {
                                if(!targetDir.mkdirs()) {
                                    System.err.println("error: cannot create local directory '%s'".format(targetDirPath))
                                    System.exit(-1)
                                }
                            }

                            val sourceFiles = loggedonUser.getSourceFiles
                            sourceFiles.foreach(sf => {
                                val filename = sf.filename
                                val filePath = targetDir.getAbsolutePath + "/" + filename
                                val fileWriter = new FileWriter(filePath)
                                fileWriter.append(sf.code)
                                fileWriter.close()
                            })

                            if(connectionConfig.verbose)
                                println("Wrote %d source files to '%s'".format(sourceFiles.size, targetDirPath))
                        }
                    }
                )
        }
    }


    /** -command updateSources      updates a source files in the user's server workspace; as user only
      * -sourceDir path         the path of the local directory where the source files can be found
      */
    def cmd_updateSources(connectionConfig: ConnectionConfig, argMap: Map[String, String]) {
        argMap.get("-sourceDir") match {
            case None =>
                System.err.println("error: command 'updateSources' requires option '-sourceDir'")
                System.exit(-1)

            case Some(sourceDirPath) =>
                doAsUser(
                    connectionConfig,
                    argMap,
                    (scalatron: ScalatronRemote, loggedonUser: ScalatronRemote.User, users: ScalatronRemote.UserList) => {
                        // update user source files (1 round-trip)
                        handleScalatronExceptionsFor {
                            val sourceFiles = readSourceFiles(sourceDirPath)

                            loggedonUser.updateSourceFiles(sourceFiles)

                            if(connectionConfig.verbose)
                                println("Updated %d source files from '%s'".format(sourceFiles.size, sourceDirPath))
                        }
                    }
                )
        }
    }


    /** -command build
      */
    def cmd_buildSources(connectionConfig: ConnectionConfig, argMap: Map[String, String]) {
        doAsUser(
            connectionConfig,
            argMap,
            (scalatron: ScalatronRemote, loggedonUser: ScalatronRemote.User, users: ScalatronRemote.UserList) => {
                // update user source files (1 round-trip)
                handleScalatronExceptionsFor {
                    val buildResult = loggedonUser.buildSources()
                    if(buildResult.successful) {
                        buildResult.messages.foreach(m => println(m.sourceFile + ": line " + m.lineAndColumn._1 + ", col " + m.lineAndColumn._2 + ": " + m.multiLineMessage))
                        println("%d errors, %d warnings".format(buildResult.errorCount, buildResult.warningCount))
                    } else {
                        buildResult.messages.foreach(m => System.err.println(m.sourceFile + ": line " + m.lineAndColumn._1 + ", col " + m.lineAndColumn._2 + ": " + m.multiLineMessage))
                        System.err.println("%d errors, %d warnings".format(buildResult.errorCount, buildResult.warningCount))
                        System.exit(-1)
                    }
                }
            }
        )
    }


    /** -command sources         gets a source files from a user workspace; user or Administrator
      * -targetDir path         the path of the local directory where the source files should be stored
      */
    def cmd_versions(connectionConfig: ConnectionConfig, argMap: Map[String, String]) {
        doAsUser(
            connectionConfig,
            argMap,
            (scalatron: ScalatronRemote, loggedonUser: ScalatronRemote.User, users: ScalatronRemote.UserList) => {
                // get version list (1 round-trip)
                handleScalatronExceptionsFor {
                    val versionList = loggedonUser.versions
                    versionList.foreach(v => println("id=%d, label=\"%s\", date=\"%s\"".format(v.id, v.label, DateFormat.getDateTimeInstance.format(new Date(v.date)))))
                }
            }
        )
    }


    /** -command createVersion      creates a new version in the user's server workspace; as user only
      * -sourceDir path             the path of the local directory where the source files can be found
      * -label string
      */
    def cmd_createVersion(connectionConfig: ConnectionConfig, argMap: Map[String, String]) {
        argMap.get("-sourceDir") match {
            case None =>
                System.err.println("error: command 'createVersion' requires option '-sourceDir'")
                System.exit(-1)

            case Some(sourceDirPath) =>
                doAsUser(
                    connectionConfig,
                    argMap,
                    (scalatron: ScalatronRemote, loggedonUser: ScalatronRemote.User, users: ScalatronRemote.UserList) => {
                        // update user source files (1 round-trip)
                        handleScalatronExceptionsFor {
                            val label = argMap.getOrElse("-label", "")
                            val sourceFiles = readSourceFiles(sourceDirPath)

                            val version = loggedonUser.createVersion(label, sourceFiles)

                            if(connectionConfig.verbose)
                                println("Create version #%d from %d source files from '%s'".format(version.id, sourceFiles.size, sourceDirPath))
                        }
                    }
                )
        }
    }


    /** -command benchmark
      * -sourceDir path     the path of the local directory where the source files can be found
      */
    def cmd_benchmark(connectionConfig: ConnectionConfig, argMap: Map[String, String]) {
        argMap.get("-sourceDir") match {
            case None =>
                System.err.println("error: command 'benchmark' requires option '-sourceDir'")
                System.exit(-1)

            case Some(sourceDirPath) =>
                doAsUser(
                    connectionConfig,
                    argMap,
                    (scalatron: ScalatronRemote, loggedonUser: ScalatronRemote.User, users: ScalatronRemote.UserList) => {
                        handleScalatronExceptionsFor {
                            // update user source files (1 round-trip)
                            val sourceFiles = readSourceFiles(sourceDirPath)
                            loggedonUser.updateSourceFiles(sourceFiles)
                            if(connectionConfig.verbose)
                                println("Updated %d source files from '%s'".format(sourceFiles.size, sourceDirPath))

                            // build uploaded user source files (1 round-trip)
                            val buildResult = loggedonUser.buildSources()
                            if(!buildResult.successful) {
                                buildResult.messages.foreach(m => System.err.println(m.sourceFile + ": line " + m.lineAndColumn._1 + ", col " + m.lineAndColumn._2 + ": " + m.multiLineMessage))
                                System.err.println("%d errors, %d warnings".format(buildResult.errorCount, buildResult.warningCount))
                                System.exit(-1)
                            } else {
                                if(connectionConfig.verbose) {
                                    buildResult.messages.foreach(m => println(m.sourceFile + ": line " + m.lineAndColumn._1 + ", col " + m.lineAndColumn._2 + ": " + m.multiLineMessage))
                                    println("%d errors, %d warnings".format(buildResult.errorCount, buildResult.warningCount))
                                }
                            }

                            for(i <- 0 until 20) {
                                // create sandboxed game (1 round-trip)
                                val sandbox =
                                    loggedonUser.createSandbox(
                                        Map(
                                            "-perimeter" -> "closed",
                                            "-x" -> "100",
                                            "-y" -> "100"
                                        )
                                    )

                                val initialState = sandbox.initialState
                                val finalSandbox = initialState.step(5000)
                                finalSandbox.entities.filter(_.isMaster).foreach(e => {
                                    if(connectionConfig.verbose) {
                                        println("Simulated until time = " + finalSandbox.time)
                                        println("Final bot energy score = " + e.mostRecentControlFunctionInput.params.getOrElse("energy", "?"))
                                    } else {
                                        println(e.mostRecentControlFunctionInput.params.getOrElse("energy", "?"))
                                    }
                                })

                                // delete the sandbox
                                sandbox.delete()
                            }
                        }
                    }
                )
        }
    }



    //------------------------------------------------------------------------------------------------------------------
    // helpers
    //------------------------------------------------------------------------------------------------------------------

    /** Reads a collection of source code files from disk, from a given directory.
      */
    private def readSourceFiles(sourceDirPath: String) : Iterable[ScalatronRemote.SourceFile] = {
        val sourceDir = new File(sourceDirPath)
        if(!sourceDir.exists()) {
            System.err.println("error: local source directory does not exist: '%s'".format(sourceDirPath))
            System.exit(-1)
        }

        val fileList = sourceDir.listFiles()
        if(fileList == null || fileList.isEmpty) {
            System.err.println("error: local source directory is empty: '%s'".format(sourceDirPath))
            System.exit(-1)
        }
        fileList.map(f => {
            val code = Source.fromFile(f).getLines().mkString("\n")
            ScalatronRemote.SourceFile(f.getName, code)
        })
    }


    /** Accepts a closure; handles typical server exceptions. Exists with error code on such exceptions.
      */
    private def handleScalatronExceptionsFor(action: => Unit) {
        try {
            action
        } catch {
            case e: ScalatronException.NotAuthorized =>
                System.err.println("error: " + e.serverMessage)
                System.exit(-1)
            case e: ScalatronException.NotFound =>
                System.err.println("error: " + e.serverMessage)
                System.exit(-1)
            case e: ScalatronException.IllegalUserName =>
                System.err.println("error: " + e.serverMessage)
                System.exit(-1)
            case e: ScalatronException.InternalServerError =>
                System.err.println("error: " + e.serverMessage)
                System.exit(-1)
            case t: Throwable =>
                System.err.println(t.toString)
                System.exit(-1)
        }
    }


    /** Performs the given action by logging on as Administrator, running the command(s), then
      * logging off.
      * @param connectionConfig the connection configuration
      * @param argMap the command line argument map
      * @param action the action to perform.
      */
    private def doAsAdministrator(
        connectionConfig: ConnectionConfig,
        argMap: Map[String, String],
        action: (ScalatronRemote, ScalatronRemote.UserList) => Unit) {
        val scalatron = ScalatronRemote(connectionConfig)

        // retrieve Administrator credentials
        val adminUserName = argMap.get("-user").getOrElse(ScalatronRemote.Constants.AdminUserName)
        if(adminUserName != ScalatronRemote.Constants.AdminUserName) {
            System.err.println("error: command 'createUser' requires log-on as user '" + ScalatronRemote.Constants.AdminUserName + "'")
            System.exit(-1)
        }
        val adminPassword = argMap.get("-password").getOrElse("")

        // retrieve user list (1 round-trip)
        val users = scalatron.users()

        // fetch Administrator user (no round-trips)
        val adminUser = users.adminUser

        // log-on as Administrator (1 round-trip)
        try {
            adminUser.logOn(adminPassword)
        } catch {
            case e: ScalatronException.NotAuthorized =>
                System.err.println("error: " + e.serverMessage)
                System.exit(-1)
            case e: ScalatronException.NotFound =>
                System.err.println("error: " + e.serverMessage)
                System.exit(-1)
            case e: ScalatronException.InternalServerError =>
                System.err.println("error: " + e.serverMessage)
                System.exit(-1)
            case t: Throwable =>
                System.err.println(t.toString)
                System.exit(-1)
        }

        // create the new user account (1 round-trip)
        try {
            action(scalatron, users)
        } catch {
            case e: ScalatronException.NotAuthorized =>
                System.err.println("error: " + e.serverMessage)
                System.exit(-1)
            case e: ScalatronException.IllegalUserName =>
                System.err.println("error: " + e.serverMessage)
                System.exit(-1)
            case e: ScalatronException.Exists =>
                System.err.println("error: " + e.serverMessage)
                System.exit(-1)
            case e: ScalatronException.CreationFailed =>
                System.err.println("error: " + e.serverMessage)
                System.exit(-1)
            case t: Throwable =>
                System.err.println(t.toString)
                System.exit(-1)
        }

        // log off as that user (1 round-trip)
        adminUser.logOff()
    }


    /** Performs the given action by logging on as Administrator, running the command(s), then
      * logging off.
      * @param connectionConfig the connection configuration
      * @param argMap the command line argument map
      * @param action the action to perform.
      */
    private def doAsUser(
        connectionConfig: ConnectionConfig,
        argMap: Map[String, String],
        action: (ScalatronRemote, ScalatronRemote.User, ScalatronRemote.UserList) => Unit) {
        val scalatron = ScalatronRemote(connectionConfig)

        // retrieve Administrator credentials
        val logonUserName = argMap.get("-user").getOrElse(ScalatronRemote.Constants.AdminUserName)
        val logonPassword = argMap.get("-password").getOrElse("")

        // retrieve user list (1 round-trip)
        val users = scalatron.users()

        // fetch user (no round-trips)
        users.user(logonUserName) match {
            case None =>
                System.err.println("error: user '%s' does not exist".format(logonUserName))
                System.exit(-1)
            case Some(loggedonUser) =>
                // log-on as Administrator (1 round-trip)
                try {
                    loggedonUser.logOn(logonPassword)
                } catch {
                    case e: ScalatronException.NotAuthorized =>
                        System.err.println("error: " + e.serverMessage)
                        System.exit(-1)
                    case e: ScalatronException.NotFound =>
                        System.err.println("error: " + e.serverMessage)
                        System.exit(-1)
                    case e: ScalatronException.InternalServerError =>
                        System.err.println("error: " + e.serverMessage)
                        System.exit(-1)
                    case t: Throwable =>
                        System.err.println(t.toString)
                        System.exit(-1)
                }

                // create the new user account (1 round-trip)
                try {
                    action(scalatron, loggedonUser, users)
                } catch {
                    case e: ScalatronException.NotAuthorized =>
                        System.err.println("error: " + e.serverMessage)
                        System.exit(-1)
                    case e: ScalatronException.IllegalUserName =>
                        System.err.println("error: " + e.serverMessage)
                        System.exit(-1)
                    case e: ScalatronException.Exists =>
                        System.err.println("error: " + e.serverMessage)
                        System.exit(-1)
                    case e: ScalatronException.CreationFailed =>
                        System.err.println("error: " + e.serverMessage)
                        System.exit(-1)
                    case t: Throwable =>
                        System.err.println(t.toString)
                        System.exit(-1)
                }

                // log off as that user (1 round-trip)
                loggedonUser.logOff()
        }
    }


}