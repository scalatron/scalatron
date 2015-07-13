package scalatron.scalatron.api

import org.specs2._
import java.io.{IOException, File}
import akka.actor.ActorSystem
import scalatron.core.Scalatron.Constants._
import scalatron.core.Scalatron
import ScalatronApiTest._
import org.specs2.execute.Result
import scalatron.scalatron.impl.FileUtil


class ScalatronApiSpec extends mutable.Specification
{
    //------------------------------------------------------------------------------------------
    // test (web) user management
    //------------------------------------------------------------------------------------------

    "Scalatron API running against a temporary /users directory" should {

        "initially contain only the Administrator user" in {
            runTest((scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
                scalatron.users().map(_.name).mkString(",") mustEqual "Administrator" and
                    (scalatron.user("ExampleUser") must beNone)
            })
        }

        "be able to create a new user" in {
            runTest((scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
                scalatron.createUser(name = "ExampleUser", password = "", initialSourceFiles = sourceFiles)

                (scalatron.users() must have size (2)) and
                    ((usersBaseDirPath + "/" + "ExampleUser") must be) and
                    (scalatron.user("ExampleUser") must beSome) and
                    (new File(usersBaseDirPath + "/ExampleUser/src/Bot.scala").exists must beTrue)
            })
        }

        "be able to delete a newly created user" in {
            runTest((scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
                val user = scalatron.createUser("ExampleUser", "", sourceFiles)
                user.delete()

                scalatron.users().map(_.name).mkString(",") mustEqual "Administrator" and
                    (scalatron.user("ExampleUser") must beNone) and
                    (new File(usersBaseDirPath + "/ExampleUser").exists must beFalse)
            })
        }
    }


    "Scalatron API running against a temporary /users directory with a newly created user" should {

        //------------------------------------------------------------------------------------------
        // test versioning
        //------------------------------------------------------------------------------------------

        "initially contain one version" in {
            runTest((scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
                val user = scalatron.createUser("ExampleUser", "", sourceFiles)
                assert(new File(usersBaseDirPath + "/ExampleUser/src/.git").exists())
                user.versions.size must beEqualTo(1)
            })
        }

        // TODO: refactor this from asserts to Specs2
        "be able to create a new version" in {
            runTest((scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
                val user = scalatron.createUser("ExampleUser", "", sourceFiles)
                assert(new File(usersBaseDirPath + "/ExampleUser/src/.git").exists())

                user.updateSourceFiles(Iterable(Scalatron.SourceFile("Bot.scala", "a")))
                val version0 = user.createVersion("testVersion0").get
                assert(version0.id != null)
                assert(version0.label == "testVersion0")
                assert(version0.user.name == "ExampleUser")

                user.updateSourceFiles(Iterable(Scalatron.SourceFile("Bot.scala", "b")))
                val version1 = user.createVersion("testVersion1").get
                assert(version1.id != version0.id)
                assert(version1.label == "testVersion1")
                assert(version1.user.name == "ExampleUser")

                val versionList = user.versions
                assert(versionList.size == 3)
                assert(versionList.head.id == version1.id)
                assert(versionList.head.label == "testVersion1")
                assert(versionList.tail.head.id == version0.id)
                assert(versionList.tail.head.label == "testVersion0")

                // retrieve version object
                val version0retrieved = user.version(version0.id).get
                assert(version0retrieved.id == version0.id)
                assert(version0retrieved.label == version0.label)
                assert(version0retrieved.date == version0.date)
                assert(version0retrieved.user.name == "ExampleUser")

                // TODO: don't update the files, then verify that no version is generated
                // TODO: verify that latestVersion returns the latest version
                // TODO: test restoring an older version

                success
            })
        }



        //------------------------------------------------------------------------------------------
        // test building
        //------------------------------------------------------------------------------------------

        "be able to build from sources from disk containing no errors" in {
            runTest((scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
                val user = scalatron.createUser("ExampleUser", "", sourceFiles)

                // TODO: refactor this from asserts to Specs2

                // build a local bot .jar
                val compileResult = user.buildSources()
                assert(compileResult.successful)
                assert(compileResult.errorCount == 0)
                assert(compileResult.warningCount == 0)
                assert(compileResult.messages.isEmpty)
                assert(new File(usersBaseDirPath + "/ExampleUser/bot/ScalatronBot.jar").exists())

                success
            })
        }


        "be able to report errors when building invalid sources" in {
            runTest((scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
                val user = scalatron.createUser("ExampleUser", "", sourceFiles)

                // TODO: refactor this from asserts to Specs2

                // create some source files with errors
                val sourceCodeWithErrors1 = "this is an error on line 1"

                val sourceCodeWithErrors2 = "class XYZ; this is an error on line 1"

                val sourceFilesWithErrors =
                    Iterable(
                        Scalatron.SourceFile("1.scala", sourceCodeWithErrors1),
                        Scalatron.SourceFile("2.scala", sourceCodeWithErrors2)
                    )
                user.updateSourceFiles(sourceFilesWithErrors)

                // build a local bot .jar
                val compileResult = user.buildSources()
                assert(!compileResult.successful)
                assert(compileResult.errorCount == 2)
                assert(compileResult.warningCount == 0)
                assert(compileResult.messages.size == 2)

                val sortedMessages = compileResult.messages.toArray.sortBy(_.sourceFile)
                val msg0 = sortedMessages(0)
                assert(msg0.sourceFile == "1.scala")
                assert(msg0.lineAndColumn ==(1, 1))
                assert(msg0.severity == 2)
                assert(msg0.multiLineMessage == "expected class or object definition")

                val msg1 = sortedMessages(1)
                assert(msg1.sourceFile == "2.scala")
                assert(msg1.lineAndColumn ==(1, 12))
                assert(msg1.severity == 2)
                assert(msg1.multiLineMessage == "expected class or object definition")

                success
            })
        }


        //------------------------------------------------------------------------------------------
        // test publishing into tournament
        //------------------------------------------------------------------------------------------


        "be able to publish a jar built from sources into the tournament" in {
            runTest((scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
                val user = scalatron.createUser("ExampleUser", "", sourceFiles)
                user.buildSources()

                // TODO: refactor this from asserts to Specs2

                // publish the bot into the tournament plug-in directory
                user.publish()
                assert(new File(pluginBaseDirPath + "/ExampleUser/ScalatronBot.jar").exists())

                // ... if we now called scalatron.run, the tournament should pick up the plug-in

                success
            })
        }


        //------------------------------------------------------------------------------------------
        // test publishing into sandbox
        //------------------------------------------------------------------------------------------


        "be able to run a sandboxed game using a jar built from sources" in {
            runTest((scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
                val user = scalatron.createUser("ExampleUser", "", sourceFiles)
                user.buildSources()

                // TODO: refactor this from asserts to Specs2

                // create a sandbox game
                val sandbox = user.createSandbox(
                    Map(
                        ("-x" -> "50"),
                        ("-y" -> "50"),
                        ("-perimeter" -> "open"),
                        ("-walls" -> "20"),
                        ("-snorgs" -> "20"),
                        ("-fluppets" -> "20"),
                        ("-toxifera" -> "20"),
                        ("-zugars" -> "20")
                    )
                )

                // simulate the sandbox game by performing 10 single-steps
                val initialSandboxState = sandbox.initialState
                val sandboxState = initialSandboxState.step(10)

                // verify the sandbox game state after 100 steps
                assert(sandboxState.time == 10)
                val entities = sandboxState.entities
                assert(entities.size == 1)

                val masterEntity = entities.find(_.isMaster).get
                assert(masterEntity.isMaster)
                assert(masterEntity.name == "ExampleUser")

                // extract bot's most recent view
                // entities.foreach(e => println(e.mostRecentControlFunctionInput))

                // extract bot's most recent log output
                // entities.foreach(e => println(e.debugOutput))

                success
            })
        }

    }



    //------------------------------------------------------------------------------------------
    // test sample code management
    //------------------------------------------------------------------------------------------

    "Scalatron API running against a temporary /samples directory" should {

        "initially find no samples" in {
            runTest((scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
                // TODO: refactor this from asserts to Specs2
                assert(scalatron.samples.isEmpty)
                assert(scalatron.sample("SampleA").isEmpty)

                success
            })
        }

        "be able to create a new sample from given sources" in {
            runTest((scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
                // TODO: refactor this from asserts to Specs2
                // create a sample
                val createdSample = scalatron.createSample("SampleA", sourceFiles)
                assert(createdSample.name == "SampleA")

                // enumerate samples again
                val samples = scalatron.samples
                assert(samples.size == 1)
                assert(samples.map(_.name).mkString(",") == "SampleA")

                // retrieve created sample
                val retrievedSampleOpt = scalatron.sample("SampleA")
                assert(retrievedSampleOpt.isDefined)
                val retrievedSample = retrievedSampleOpt.get
                val sampleFiles = retrievedSample.sourceFiles
                assert(sampleFiles.size == 1)
                assert(sampleFiles.head.filename == "Bot.scala")

                // we could now push the sample code into the user's workspace with user.updateSources()

                success
            })
        }

        "be able to delete a newly created sample" in {
            runTest((scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
                // TODO: refactor this from asserts to Specs2
                val createdSample = scalatron.createSample("SampleA", sourceFiles)
                assert(scalatron.samples.size == 1)

                createdSample.delete()
                assert(scalatron.samples.size == 0)

                success
            })
        }
    }
}



/** Helper object for API verification. */
object ScalatronApiTest
{
    val sourceCode = """
    class ControlFunctionFactory { def create = new Bot().respond _ }
    class Bot { def respond(input: String) = "Log(text=This is a test 1\nThis is a test 2\nThis is a test 3)" }
    """
    val sourceFiles = Iterable(Scalatron.SourceFile("Bot.scala", sourceCode))


    // see http://stackoverflow.com/questions/617414/create-a-temporary-directory-in-java
    private def createTempDirectory(): String = {
        val temp = File.createTempFile("temp", System.nanoTime().toString)
        if( !temp.delete() ) throw new IOException("Could not delete temp file: " + temp.getAbsolutePath)
        if( !temp.mkdir() ) throw new IOException("Could not create temp directory: " + temp.getAbsolutePath)
        temp.getAbsolutePath
    }


    /** Creates a temporary directory, starts a Scalatron server, then runs the given test
      * method, invoking it with (scalatron, usersBaseDirPath, samplesBaseDirPath, pluginBaseDirPath),
      * then shuts down the Scalatron server and deletes the temp directory.
      */
    def runTest(test: (Scalatron, String, String, String) => Result, verbose: Boolean = false) : Result = {
        //------------------------------------------------------------------------------------------
        // prepare environment, start server
        //------------------------------------------------------------------------------------------

        // create a temporary directory for testing
        val tmpDirPath = createTempDirectory() // serves as "/Scalatron"

        try {
            val usersBaseDirPath = tmpDirPath + "/" + UsersDirectoryName
            val samplesBaseDirPath = tmpDirPath + "/" + SamplesDirectoryName
            val pluginBaseDirPath = tmpDirPath + "/" + TournamentBotsDirectoryName

            // prepare the Akka actor system to be used by the various servers of the application
            val actorSystem = ActorSystem("Scalatron")

            // create a Scalatron server instance - this is the main API entry point
            val scalatron =
                ScalatronOutward(
                    Map(
                        ( "-users" -> usersBaseDirPath ),
                        ( "-samples" -> samplesBaseDirPath ),
                        ( "-plugins" -> pluginBaseDirPath )
                    ),
                    actorSystem,
                    verbose
                )

            // start the server, launching the background thread(s) (e.g., compile server)
            scalatron.start()

            val result = test(scalatron, usersBaseDirPath, samplesBaseDirPath, pluginBaseDirPath)

            // shut down the server, terminating the background thread(s)
            scalatron.shutdown()

            result
        } finally {
            // delete the temporary directory
            FileUtil.deleteRecursively(tmpDirPath, atThisLevel = true, verbose = verbose)
        }
    }

}
