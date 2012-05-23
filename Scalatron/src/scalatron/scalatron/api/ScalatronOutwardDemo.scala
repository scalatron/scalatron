/**This material is intended as a community resource and is licensed under the
  * Creative Commons Attribution 3.0 Unported License. Feel free to use, modify and share it.
  */
package scalatron.scalatron.api

import scalatron.core.Scalatron.Constants._
import java.io.{IOException, File}
import akka.actor.ActorSystem
import scalatron.scalatron.impl.FileUtil
import scalatron.scalatron.impl.FileUtil
import scalatron.core.Scalatron


object ScalatronOutwardDemo
{
    def demo(args: Array[String]) {
        //------------------------------------------------------------------------------------------
        // prepare environment, start server
        //------------------------------------------------------------------------------------------

        // create a temporary directory for testing
        // see http://stackoverflow.com/questions/617414/create-a-temporary-directory-in-java
        def createTempDirectory(): String = {
            val temp = File.createTempFile("temp", System.nanoTime().toString)
            if( !temp.delete() ) throw new IOException("Could not delete temp file: " + temp.getAbsolutePath)
            if( !temp.mkdir() ) throw new IOException("Could not create temp directory: " + temp.getAbsolutePath)
            temp.getAbsolutePath
        }
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
                    verbose = true
                )

            // start the server, launching the background thread(s) (e.g., compile server)
            scalatron.start()


            //------------------------------------------------------------------------------------------
            // test web user management
            //------------------------------------------------------------------------------------------

            // find out which users exist - initially there should only be the Administrator
            assert(scalatron.users().map(_.name).mkString(",") == "Administrator")
            assert(scalatron.user("ExampleUser").isEmpty)

            // define some source code file(s)
            val sourceCode = """
            // version 1
            class ControlFunctionFactory { def create = new Bot().respond _ }
            class Bot { def respond(input: String) = "Log(text=This is a test 1\nThis is a test 2\nThis is a test 3)" }
            """
            val sourceFiles = Iterable(Scalatron.SourceFile("Bot.scala", sourceCode))

            // create a web user
            scalatron.createUser(name = "ExampleUser", password = "", initialSourceFiles = sourceFiles)

            // find out which users exist - now there should be the Administrator and the ExampleUser
            assert(scalatron.users().size == 2)

            assert(new File(usersBaseDirPath + "/" + "ExampleUser").exists())
            val userOpt = scalatron.user("ExampleUser")
            assert(userOpt.isDefined)
            val user = userOpt.get


            // the source code directory should not exist and be populated, if necessary
            assert(new File(usersBaseDirPath + "/ExampleUser/src/Bot.scala").exists())



            //------------------------------------------------------------------------------------------
            // test versioning
            //------------------------------------------------------------------------------------------

            // initially, there should be one version, the auto-generated initial commit
            assert(user.versions.size == 1)
            assert(new File(usersBaseDirPath + "/ExampleUser/.git").exists())

            user.updateSourceFiles(sourceFiles)
            val version0 = user.createVersion("testVersion0").get
            assert(version0.id == 0)
            assert(version0.label == "testVersion0")
            assert(version0.user.name == "ExampleUser")

            user.updateSourceFiles(sourceFiles)
            val version1 = user.createVersion("testVersion1").get
            assert(version1.id == 1)
            assert(version1.label == "testVersion1")
            assert(version1.user.name == "ExampleUser")

            val versionList = user.versions
            assert(versionList.size == 3)
            assert(versionList.tail.head.id == version0.id)
            assert(versionList.tail.head.label == "testVersion0")
            assert(versionList.last.id == version1.id)
            assert(versionList.last.label == "testVersion1")

            // retrieve version object
            val version0retrieved = user.version(versionList.head.id).get
            assert(version0retrieved.id == version0.id)
            assert(version0retrieved.label == version0.label)
            assert(version0retrieved.date == version0.date)
            assert(version0retrieved.user.name == "ExampleUser")

            // we could now push an older version into the user's workspace with user.updateSources()


            //------------------------------------------------------------------------------------------
            // test sample code management
            //------------------------------------------------------------------------------------------

            // initially, there should be no samples
            assert(scalatron.samples.isEmpty)
            assert(scalatron.sample("SampleA").isEmpty)

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


            //------------------------------------------------------------------------------------------
            // test building
            //------------------------------------------------------------------------------------------

            // build a local bot .jar
            val compileResult = user.buildSources()
            assert(compileResult.successful)
            assert(compileResult.errorCount == 0)
            assert(compileResult.warningCount == 0)
            assert(compileResult.messages.isEmpty)
            assert(new File(usersBaseDirPath + "/ExampleUser/bot/ScalatronBot.jar").exists())


            //------------------------------------------------------------------------------------------
            // test publishing into tournament
            //------------------------------------------------------------------------------------------

            // publish the bot into the tournament plug-in directory
            user.publish()
            assert(new File(pluginBaseDirPath + "/ExampleUser/ScalatronBot.jar").exists())

            // ... if we now called scalatron.run, the tournament should pick up the plug-in


            //------------------------------------------------------------------------------------------
            // test publishing into sandbox
            //------------------------------------------------------------------------------------------

            // create a sandbox game
            val sandbox = user.createSandbox(
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
            entities.foreach(e => println(e.mostRecentControlFunctionInput))

            // extract bot's most recent log output
            entities.foreach(e => println(e.debugOutput))


            //------------------------------------------------------------------------------------------
            // shut down server, clean up environment
            //------------------------------------------------------------------------------------------

            // delete the web user we created
            user.delete()
            assert(scalatron.users().map(_.name).mkString(",") == "Administrator")
            assert(scalatron.user("ExampleUser").isEmpty)
            assert(new File(usersBaseDirPath + "/" + "ExampleUser").exists() == false)

            // shut down the server, terminating the background thread(s)
            scalatron.shutdown()

        } finally {
            // delete the temporary directory
            FileUtil.deleteRecursively(tmpDirPath, atThisLevel = true, verbose = false)
        }
    }
}