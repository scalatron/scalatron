/** This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */

package scalatronCLI.cmdline

import scalatronRemote.api.ScalatronRemote
import scalatronRemote.Version
import java.text.DateFormat
import java.util.Date
import scalatronRemote.api.ScalatronRemote.{SourceFileCollection, ScalatronException, ConnectionConfig}


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
            println("   deleteAllUsers              deletes all existing users (along with all content!); Administrator only")
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
            println("   publish                     publishes the most recently built bot version into the tournament loop; as user only")
            println("")
            println("   versions                    lists all versions available in the user workspace; as user only")
            println("")
            println("   createVersion               creates a new version in the user's server workspace; as user only")
            println("       -sourceDir <path>       the path of the local directory where the source files can be found")
            println("       -label <name>           the label to apply to the versions (default: empty)")
            println("")
            println("   restoreVersion              restores the version with the given ID in the user's workspace and fetches the associated files; as user only")
            println("       -targetDir <path>       the path of the local directory where the source files should be stored")
            println("       -id <string>            the version's ID")
            println("")
            println("   benchmark                   runs standard isolated-bot benchmark on given source files; as user only")
            println("       -sourceDir <path>       the path of the local directory where the source files can be found")
            println("")
            println("   stresstest                  runs a stress test, simulating a hack-a-thon workload on the server; as Administrator only")
            println("       -clients <int>          the number of clients to simulate (default: 1)")
            println("")
            println("Examples:")
            println(" java -jar ScalatronCLI.jar -cmd users")
            println(" java -jar ScalatronCLI.jar -user Administrator -password a -cmd createUser -targetUser Frankie")
            println(" java -jar ScalatronCLI.jar -user Administrator -password a -cmd deleteAllUsers")
            println(" java -jar ScalatronCLI.jar -user Administrator -password a -cmd setUserAttribute -targetUser Frankie -key theKey -value theValue")
            println(" java -jar ScalatronCLI.jar -user Administrator -password a -cmd getUserAttribute -targetUser Frankie -key theKey")
            println(" java -jar ScalatronCLI.jar -user Frankie -password a -cmd sources -targetDir /tempsrc")
            println(" java -jar ScalatronCLI.jar -user Frankie -password a -cmd updateSources -sourceDir /tempsrc")
            println(" java -jar ScalatronCLI.jar -user Frankie -password a -cmd build")
            println(" java -jar ScalatronCLI.jar -user Frankie -password a -cmd publish")
            println(" java -jar ScalatronCLI.jar -user Frankie -password a -cmd versions")
            println(" java -jar ScalatronCLI.jar -user Frankie -password a -cmd createVersion -sourceDir /tempsrc -label \"updated\"")
            println(" java -jar ScalatronCLI.jar -user Frankie -password a -cmd restoreVersion -targetDir /tempsrc -id a1ae813f274b4a33bc61535e0e0de5345bb08d42")
            println(" java -jar ScalatronCLI.jar -user Frankie -password a -cmd benchmark -sourceDir /tempsrc")
            println(" java -jar ScalatronCLI.jar -cmd stresstest -clients 10")
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
                        case "deleteAllUsers" => cmd_deleteAllUsers(connectionConfig, argMap)
                        case "setUserAttribute" => cmd_setUserAttribute(connectionConfig, argMap)
                        case "getUserAttribute" => cmd_getUserAttribute(connectionConfig, argMap)
                        case "sources" => cmd_sources(connectionConfig, argMap)
                        case "updateSources" => cmd_updateSources(connectionConfig, argMap)
                        case "build" => cmd_buildSources(connectionConfig, argMap)
                        case "publish" => cmd_publish(connectionConfig, argMap)
                        case "versions" => cmd_versions(connectionConfig, argMap)
                        case "createVersion" => cmd_createVersion(connectionConfig, argMap)
                        case "restoreVersion" => cmd_restoreVersion(connectionConfig, argMap)
                        case "benchmark" => cmd_benchmark(connectionConfig, argMap)
                        case "stresstest" => cmd_stresstest(connectionConfig, argMap)
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
                        users.get(targetUser) match {
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


    /** -command deleteAllUsers          deletes all existing users (along with all content!); Administrator only
      */
    def cmd_deleteAllUsers(connectionConfig: ConnectionConfig, argMap: Map[String, String]) {
        doAsAdministrator(
            connectionConfig,
            argMap,
            (scalatron: ScalatronRemote, users: ScalatronRemote.UserList) => {
                if(connectionConfig.verbose) println("Deleting %d users on server '%s'...".format(users.size, scalatron.hostname))
                users.foreach(user => {
                    if(!user.isAdministrator) {
                        handleScalatronExceptionsFor {
                            user.delete()
                            if(connectionConfig.verbose) println("Deleted user '%s'" format user.name)
                        }
                    }
                })
            }
        )
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
                                users.get(targetUserName) match {
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
                        users.get(targetUserName) match {
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
                            val sourceFileCollection = loggedonUser.sourceFiles
                            SourceFileCollection.writeTo(targetDirPath, sourceFileCollection, connectionConfig.verbose)

                            if(connectionConfig.verbose)
                                println("Wrote %d source files to '%s'".format(sourceFileCollection.size, targetDirPath))
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
                            val sourceFileCollection = SourceFileCollection.loadFrom(sourceDirPath)

                            loggedonUser.updateSourceFiles(sourceFileCollection)

                            if(connectionConfig.verbose)
                                println("Updated %d source files from '%s'".format(sourceFileCollection.size, sourceDirPath))
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


    /** -command publish
      */
    def cmd_publish(connectionConfig: ConnectionConfig, argMap: Map[String, String]) {
        doAsUser(
            connectionConfig,
            argMap,
            (scalatron: ScalatronRemote, loggedonUser: ScalatronRemote.User, users: ScalatronRemote.UserList) => {
                handleScalatronExceptionsFor { loggedonUser.publish() }
            }
        )
    }


    /** -command versions         gets a list of versions for a specific user; as user only
      */
    def cmd_versions(connectionConfig: ConnectionConfig, argMap: Map[String, String]) {
        doAsUser(
            connectionConfig,
            argMap,
            (scalatron: ScalatronRemote, loggedonUser: ScalatronRemote.User, users: ScalatronRemote.UserList) => {
                // get version list (1 round-trip)
                handleScalatronExceptionsFor {
                    val versionList = loggedonUser.versions
                    versionList.foreach(v => println("id=\"%s\", label=\"%s\", date=\"%s\"".format(v.id, v.label, DateFormat.getDateTimeInstance.format(new Date(v.date)))))
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
                            val sourceFileCollection = SourceFileCollection.loadFrom(sourceDirPath)

                            val version = loggedonUser.createVersion(label, sourceFileCollection)

                            if(connectionConfig.verbose)
                                println("Create version %s from %d source files from '%s'".format(version.id, sourceFileCollection.size, sourceDirPath))
                        }
                    }
                )
        }
    }


    /** -command restoreVersion     restores the version with the given ID in the user's workspace and fetches the associated files; as user only
      *     -targetDir path         the path of the local directory where the source files should be stored
      *     -id int                 the version's ID
      */
    def cmd_restoreVersion(connectionConfig: ConnectionConfig, argMap: Map[String, String]) {
        argMap.get("-targetDir") match {
            case None =>
                System.err.println("error: command 'restoreVersion' requires option '-targetDir'")
                System.exit(-1)

            case Some(targetDirPath) =>
                argMap.get("-id") match {
                    case None =>
                        System.err.println("error: command 'restoreVersion' requires option '-id'")
                        System.exit(-1)

                    case Some(versionIdStr) =>
                        doAsUser(
                            connectionConfig,
                            argMap,
                            (scalatron: ScalatronRemote, loggedonUser: ScalatronRemote.User, users: ScalatronRemote.UserList) => {
                                // get user source files (1 round-trip)
                                handleScalatronExceptionsFor {
                                    loggedonUser.version(versionIdStr) match {
                                        case None =>
                                            System.err.println("error: cannot locate version with id '%s'".format(versionIdStr))
                                            System.exit(-1)

                                        case Some(version) =>
                                            val sourceFileCollection = version.sourceFiles
                                            SourceFileCollection.writeTo(targetDirPath, sourceFileCollection, connectionConfig.verbose)

                                            if(connectionConfig.verbose)
                                                println("Wrote %d source files to '%s'".format(sourceFileCollection.size, targetDirPath))
                                    }
                                }
                            }
                        )
                }
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
                            val sourceFileCollection = SourceFileCollection.loadFrom(sourceDirPath)
                            loggedonUser.updateSourceFiles(sourceFileCollection)
                            if(connectionConfig.verbose)
                                println("Updated %d source files from '%s'".format(sourceFileCollection.size, sourceDirPath))

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



    /** -command stresstest     runs a stress test, simulating a hack-a-thon workload on the server; as Administrator only
      * -clients int            the number of clients to simulate (default: 1)
      */
    def cmd_stresstest(connectionConfig: ConnectionConfig, argMap: Map[String, String])
    {
        // clean up the server before we start the stress test
        cmd_deleteAllUsers(connectionConfig, argMap)

        val clientCount = argMap.get("-clients").map(_.toInt).getOrElse(1)

        def stressTask(clientId: Int) {
            def log(s: String) { System.out.println("[" + clientId + "] " + s) }
            try {
                // create one connection per task
                val scalatron = ScalatronRemote(connectionConfig)

                // --- create temporary user ---

                // retrieve Administrator credentials
                val adminUserName = ScalatronRemote.Constants.AdminUserName
                val adminPassword = argMap.get("-password").getOrElse("")

                log("logging on as %s" format adminUserName)
                val initialUsers = scalatron.users()       // retrieve user list (1 round-trip)
                val adminUser = initialUsers.adminUser     // fetch Administrator user (no round-trips)
                adminUser.logOn(adminPassword)      // log-on as Administrator (1 round-trip)

                // create the new user account (1 round-trip)
                val userName = "Stresstest_" + clientId
                val userPassword = "Stresstest_" + clientId
                log("creating user %s" format userName)
                scalatron.createUser(userName, userPassword)

                log("logging off as %s" format adminUserName)
                adminUser.logOff()  // log off as Administrator (1 round-trip)


                // --- outer stress loop ---
                (0 until Int.MaxValue).foreach(outerCounter => {
                    log("cycle %d: logging on as %s" format(outerCounter, userName))
                    val updatedUsers = scalatron.users()                // retrieve user list (1 round-trip)
                    val regularUser = updatedUsers.get(userName).get   // fetch Administrator user (no round-trips)
                    regularUser.logOn(userPassword)                     // log-on as Administrator (1 round-trip)

                    // update the source code to the reference bot
                    val sampleList = scalatron.samples
                    val referenceBotSample = sampleList.get("ExampleBot01-Reference").get
                    val referenceBotSourceFiles = referenceBotSample.sourceFiles
                    regularUser.updateSourceFiles(referenceBotSourceFiles)

                    // --- inner stress loop ---
                    val innerCycleCount = 10
                    (0 until innerCycleCount).foreach(innerCounter => {
                        val (buildResult, buildTime) = timedResult { regularUser.buildSources() }
                        log("%d:%d - buildSources() - %d ms total, %d ms pure - %s".format(
                            outerCounter, innerCounter,
                            buildTime, buildResult.duration,
                            (if(buildResult.successful) "succeeded" else "failed (%s)".format(buildResult.messages.mkString(",")))))

                        val (sandbox, sandboxTime) = timedResult { regularUser.createSandbox() }
                        // log("%d:%d - createSandbox() - %d ms" format(outerCounter, innerCounter, sandboxTime))

                        val publishTime = timed { regularUser.publish() }
                        // log("%d:%d - publish() - %d ms" format(outerCounter, innerCounter, publishTime))
                    })

                    regularUser.logOff()    // log off as regular user password (1 round-trip)
                })
            } catch {
                case t: Throwable =>
                    log("exception: " + t)
                    t.printStackTrace()
                    throw t
            }
        }

        (0 until clientCount).foreach(n => new Thread( new Runnable { def run() { stressTask(n) } }, "LoadThread-" + n).start() )
    }






    //------------------------------------------------------------------------------------------------------------------
    // helpers
    //------------------------------------------------------------------------------------------------------------------

    def timed(action: => Unit) : Long = {
        val startTime = System.currentTimeMillis
        action
        System.currentTimeMillis - startTime
    }

    def timedResult[T](action: => T) : (T, Long) = {
        val startTime = System.currentTimeMillis
        val result = action
        val duration = System.currentTimeMillis - startTime
        (result, duration)
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
            System.err.println("error: command requires log-on as user '" + ScalatronRemote.Constants.AdminUserName + "'")
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

        // perform the action (1+ round-trips)
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
        users.get(logonUserName) match {
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

                // perform the action (1+ round-trips)
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