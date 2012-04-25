/**This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.scalatron.api

import scalatron.scalatron.api.Scalatron.Constants._
import scalatron.scalatron.impl.ScalatronUser
import java.io.{IOException, File}
import org.testng.annotations.Test
import org.testng.Assert
import akka.actor.ActorSystem


class ScalatronTest {
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
    private def runTest(test: (Scalatron, String, String, String) => Unit, verbose: Boolean = false) {
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
            implicit val actorSystem = ActorSystem("Scalatron")

            // create a Scalatron server instance - this is the main API entry point
            val scalatron =
                Scalatron(
                    Map(
                        ( "-users" -> usersBaseDirPath ),
                        ( "-samples" -> samplesBaseDirPath ),
                        ( "-plugins" -> pluginBaseDirPath )
                    ),
                    verbose
                )

            // start the server, launching the background thread(s) (e.g., compile server)
            scalatron.start()

            test(scalatron, usersBaseDirPath, samplesBaseDirPath, pluginBaseDirPath)

            // shut down the server, terminating the background thread(s)
            scalatron.shutdown()

        } finally {
            // delete the temporary directory
            ScalatronUser.deleteRecursively(tmpDirPath, verbose)
        }
    }


    val sourceCode = """
    class ControlFunctionFactory { def create = new Bot().respond _ }
    class Bot { def respond(input: String) = "Log(text=This is a test 1\nThis is a test 2\nThis is a test 3)" }
    """
    val sourceFiles = Iterable(Scalatron.SourceFile("Bot.scala", sourceCode))




    //------------------------------------------------------------------------------------------
    // test (web) user management
    //------------------------------------------------------------------------------------------

    @Test def test_users_initial() {
        runTest( (scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
            // find out which users exist - initially there should only be the Administrator
            assert(scalatron.users().map(_.name).mkString(",") == "Administrator")
            assert(scalatron.user("ExampleUser").isEmpty)
        })
    }


    @Test def test_createUser() {
        runTest( (scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
            // create a web user
            scalatron.createUser(name = "ExampleUser", password = "", initialSourceFiles = sourceFiles)

            // find out which users exist - now there should be the Administrator and the ExampleUser
            assert(scalatron.users().size == 2)

            assert(new File(usersBaseDirPath + "/" + "ExampleUser").exists())
            val userOpt = scalatron.user("ExampleUser")
            assert(userOpt.isDefined)

            // some default source code should also now exist for the user
            assert(new File(usersBaseDirPath + "/ExampleUser/src/Bot.scala").exists())
        })
    }


    @Test def test_user_delete() {
        runTest( (scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
            val user = scalatron.createUser("ExampleUser", "", sourceFiles)

            // delete the web user we created
            user.delete()
            assert(scalatron.users().map(_.name).mkString(",") == "Administrator")
            assert(scalatron.user("ExampleUser").isEmpty)
            assert(new File(usersBaseDirPath + "/" + "ExampleUser").exists() == false)
        })
    }






    //------------------------------------------------------------------------------------------
    // test versioning
    //------------------------------------------------------------------------------------------

    @Test def test_user_versions_initial() {
        runTest( (scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
            val user = scalatron.createUser("ExampleUser", "", sourceFiles)

            // initially, there should be no versions
            assert(user.versions.isEmpty)
        })
    }



    @Test def test_user_createVersion() {
        runTest( (scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
            val user = scalatron.createUser("ExampleUser", "", sourceFiles)

            val version0 = user.createVersion("testVersion0", sourceFiles)
            assert(version0.id == 0)
            assert(version0.label == "testVersion0")
            assert(version0.user.name == "ExampleUser")
            assert(new File(usersBaseDirPath + "/ExampleUser/versions").exists())
            assert(new File(usersBaseDirPath + "/ExampleUser/versions/0").exists())
            assert(new File(usersBaseDirPath + "/ExampleUser/versions/0/config.txt").exists())
            assert(new File(usersBaseDirPath + "/ExampleUser/versions/0/Bot.scala").exists())

            val version1 = user.createVersion("testVersion1", sourceFiles)
            assert(version1.id == 1)
            assert(version1.label == "testVersion1")
            assert(version1.user.name == "ExampleUser")
            assert(new File(usersBaseDirPath + "/ExampleUser/versions/1").exists())
            assert(new File(usersBaseDirPath + "/ExampleUser/versions/1/config.txt").exists())
            assert(new File(usersBaseDirPath + "/ExampleUser/versions/1/Bot.scala").exists())

            val versionList = user.versions
            assert(versionList.size == 2)
            assert(versionList.head.id == 0)
            assert(versionList.head.label == "testVersion0")
            assert(versionList.last.id == 1)
            assert(versionList.last.label == "testVersion1")

            // retrieve version object
            val version0retrieved = user.version(0).get
            assert(version0retrieved.id == version0.id)
            assert(version0retrieved.label == version0.label)
            assert(version0retrieved.date == version0.date)
            assert(version0retrieved.user.name == "ExampleUser")

            // retrieve version files
            val version0files = version0retrieved.sourceFiles
            assert(version0files.size == 1)
            assert(version0files.head.filename == "Bot.scala")

            // we could now push an older version into the user's workspace with user.updateSources()
        })
    }



    //------------------------------------------------------------------------------------------
    // test building
    //------------------------------------------------------------------------------------------

    @Test def test_user_buildSources_noErrors() {
        runTest( (scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
            val user = scalatron.createUser("ExampleUser", "", sourceFiles)

            // build a local bot .jar
            val compileResult = user.buildSources()
            assert(compileResult.successful)
            assert(compileResult.errorCount == 0)
            assert(compileResult.warningCount == 0)
            assert(compileResult.messages.isEmpty)
            assert(new File(usersBaseDirPath + "/ExampleUser/bot/ScalatronBot.jar").exists())
        })
    }


    @Test def test_user_buildSources_withErrors() {
        runTest( (scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
            val user = scalatron.createUser("ExampleUser", "", sourceFiles)

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
            Assert.assertFalse(compileResult.successful)
            Assert.assertEquals(compileResult.errorCount, 2)
            Assert.assertEquals(compileResult.warningCount, 0)
            Assert.assertEquals(compileResult.messages.size, 2)

            val msg1 = compileResult.messages.head
            Assert.assertEquals(msg1.sourceFile, "2.scala")
            Assert.assertEquals(msg1.lineAndColumn, (1,12))
            Assert.assertEquals(msg1.severity, 2)
            Assert.assertEquals(msg1.multiLineMessage, "expected class or object definition")

            val msg2 = compileResult.messages.last
            Assert.assertEquals(msg2.sourceFile, "1.scala")
            Assert.assertEquals(msg2.lineAndColumn, (1,1))
            Assert.assertEquals(msg2.severity, 2)
            Assert.assertEquals(msg2.multiLineMessage, "expected class or object definition")
        })
    }



    //------------------------------------------------------------------------------------------
    // test publishing into tournament
    //------------------------------------------------------------------------------------------

    @Test def test_user_publishBotIntoTournament() {
        runTest( (scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
            val user = scalatron.createUser("ExampleUser", "", sourceFiles)
            user.buildSources()


            // publish the bot into the tournament plug-in directory
            user.publish()
            assert(new File(pluginBaseDirPath + "/ExampleUser/ScalatronBot.jar").exists())

            // ... if we now called scalatron.run, the tournament should pick up the plug-in
        })
    }



    //------------------------------------------------------------------------------------------
    // test publishing into sandbox
    //------------------------------------------------------------------------------------------

    @Test def test_user_startSandboxGame() {
        runTest( (scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
            val user = scalatron.createUser("ExampleUser", "", sourceFiles)
            user.buildSources()

            // create a sandbox game
            val initialSandboxState = user.createSandbox(
                Map(
                    ( "-x" -> "50" ),
                    ( "-y" -> "50" ),
                    ( "-perimeter" -> "open" ),
                    ( "-walls" -> "20" ),
                    ( "-snorgs" -> "20" ),
                    ( "-fluppets" -> "20" ),
                    ( "-toxifera" -> "20" ),
                    ( "-zugars" -> "20" )
                )
            )

            // simulate the sandbox game by performing 10 single-steps
            val sandboxState = initialSandboxState.step(10)

            // verify the sandbox game state after 100 steps
            assert(sandboxState.time == 10)
            val entities = sandboxState.entities
            assert(entities.size == 1)

            val masterEntity = entities.find(_.isMaster).get
            assert(masterEntity.isMaster)
            assert(masterEntity.name == "ExampleUser")

            // extract bot's most recent view
            entities.foreach(e => println(e.mostRecentControlFunctionInput))

            // extract bot's most recent log output
            entities.foreach(e => println(e.debugOutput))
        })
    }




    //------------------------------------------------------------------------------------------
    // test sample code management
    //------------------------------------------------------------------------------------------

    @Test def test_samples_initial() {
        runTest( (scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
            // initially, there should be no samples
            assert(scalatron.samples.isEmpty)
            assert(scalatron.sample("SampleA").isEmpty)
        })
    }


    @Test def test_createSample() {
        runTest( (scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
            // create a sample
            val createdSample = scalatron.createSample("SampleA", sourceFiles)
            assert(createdSample.name == "SampleA")

            // enumerate samples again
            val samples = scalatron.samples
            Assert.assertEquals(samples.size, 1)
            Assert.assertEquals(samples.map(_.name).mkString(","), "SampleA")

            // retrieve created sample
            val retrievedSampleOpt = scalatron.sample("SampleA")
            Assert.assertTrue(retrievedSampleOpt.isDefined)
            val retrievedSample = retrievedSampleOpt.get
            val sampleFiles = retrievedSample.sourceFiles
            Assert.assertEquals(sampleFiles.size, 1)
            Assert.assertEquals(sampleFiles.head.filename, "Bot.scala")

            // we could now push the sample code into the user's workspace with user.updateSources()
        })
    }


    @Test def test_deleteSample() {
        runTest( (scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
            val createdSample = scalatron.createSample("SampleA", sourceFiles)
            Assert.assertEquals(scalatron.samples.size, 1)

            createdSample.delete()
            Assert.assertEquals(scalatron.samples.size, 0)
        })
    }
}