package scalatron.scalatron.api

import java.io.{File, IOException}

import akka.actor.ActorSystem
import org.scalatest.{FlatSpec, Matchers}

import scalatron.core.Scalatron
import scalatron.core.Scalatron.Constants._
import scalatron.scalatron.impl.FileUtil


class ScalatronApiSpec extends FlatSpec with Matchers {
  val sourceCode =
    """
    class ControlFunctionFactory { def create = new Bot().respond _ }
    class Bot { def respond(input: String) = "Log(text=This is a test 1\nThis is a test 2\nThis is a test 3)" }
    """
  val sourceFiles = Iterable(Scalatron.SourceFile("Bot.scala", sourceCode))


  // see http://stackoverflow.com/questions/617414/create-a-temporary-directory-in-java
  private def createTempDirectory(): String = {
    val temp = File.createTempFile("temp", System.nanoTime().toString)
    if (!temp.delete()) throw new IOException("Could not delete temp file: " + temp.getAbsolutePath)
    if (!temp.mkdir()) throw new IOException("Could not create temp directory: " + temp.getAbsolutePath)
    temp.getAbsolutePath
  }


  /** Creates a temporary directory, starts a Scalatron server, then runs the given test
    * method, invoking it with (scalatron, usersBaseDirPath, samplesBaseDirPath, pluginBaseDirPath),
    * then shuts down the Scalatron server and deletes the temp directory.
    */
  def runScalatron(test: (Scalatron, String, String, String) => Unit, verbose: Boolean = false): Unit = {
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
          "-users" -> usersBaseDirPath,
          "-samples" -> samplesBaseDirPath,
          "-plugins" -> pluginBaseDirPath
        ),
        actorSystem,
        verbose
      )

      // start the server, launching the background thread(s) (e.g., compile server)
      scalatron.start()

      test(scalatron, usersBaseDirPath, samplesBaseDirPath, pluginBaseDirPath)

      // shut down the server, terminating the background thread(s)
      scalatron.shutdown()

    } finally {
      // delete the temporary directory
      FileUtil.deleteRecursively(tmpDirPath, atThisLevel = true, verbose = verbose)
    }
  }

  //------------------------------------------------------------------------------------------
  // test (web) user management
  //------------------------------------------------------------------------------------------

  "Scalatron API running against a temporary /users directory" should "initially contain only the Administrator user" in {
    runScalatron((scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
      scalatron.users().map(_.name).mkString(",") shouldBe "Administrator"
      scalatron.user("ExampleUser") shouldBe None
    })
  }

  it should "be able to create a new user" in {
    runScalatron((scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
      scalatron.createUser(name = "ExampleUser", password = "", initialSourceFiles = sourceFiles)

      scalatron.users() should have size 2
      new File(usersBaseDirPath + "/" + "ExampleUser").exists shouldBe true
      scalatron.user("ExampleUser").isDefined shouldBe true
      new File(usersBaseDirPath + "/ExampleUser/src/Bot.scala").exists shouldBe true
    })
  }

  it should "be able to delete a newly created user" in {
    runScalatron((scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
      val user = scalatron.createUser("ExampleUser", "", sourceFiles)
      user.delete()

      scalatron.users().map(_.name).mkString(",") shouldBe "Administrator"
      scalatron.user("ExampleUser") shouldBe None
      new File(usersBaseDirPath + "/ExampleUser").exists shouldBe false
    })
  }

  //------------------------------------------------------------------------------------------
  // test versioning
  //------------------------------------------------------------------------------------------

  "Scalatron API running against a temporary /users directory with a newly created user" should "initially contain one version" in {
    runScalatron((scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
      val user = scalatron.createUser("ExampleUser", "", sourceFiles)
      assert(new File(usersBaseDirPath + "/ExampleUser/src/.git").exists())
      user.versions.size shouldBe 1
    })
  }

  it should "be able to create a new version" in {
    runScalatron((scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
      val user = scalatron.createUser("ExampleUser", "", sourceFiles)
      assert(new File(usersBaseDirPath + "/ExampleUser/src/.git").exists())

      user.updateSourceFiles(Iterable(Scalatron.SourceFile("Bot.scala", "a")))
      val version0 = user.createVersion("testVersion0").get
      version0.id should not be null
      version0.label shouldBe "testVersion0"
      version0.user.name shouldBe "ExampleUser"

      user.updateSourceFiles(Iterable(Scalatron.SourceFile("Bot.scala", "b")))
      val version1 = user.createVersion("testVersion1").get
      version1.id should not equal version0.id
      version1.label shouldBe "testVersion1"
      version1.user.name shouldBe "ExampleUser"

      val versionList = user.versions
      versionList.size shouldBe 3
      versionList.head.id shouldBe version1.id
      versionList.head.label shouldBe "testVersion1"
      versionList.tail.head.id shouldBe version0.id
      versionList.tail.head.label shouldBe "testVersion0"

      // retrieve version object
      val version0retrieved = user.version(version0.id).get
      version0retrieved.id shouldBe version0.id
      version0retrieved.label shouldBe version0.label
      version0retrieved.date shouldBe version0.date
      version0retrieved.user.name shouldBe "ExampleUser"

      // TODO: don't update the files, then verify that no version is generated
      // TODO: verify that latestVersion returns the latest version
      // TODO: test restoring an older version
    })
  }

  //------------------------------------------------------------------------------------------
  // test building
  //------------------------------------------------------------------------------------------

  it should "be able to build from sources from disk containing no errors" in {
    runScalatron((scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
      val user = scalatron.createUser("ExampleUser", "", sourceFiles)

      // build a local bot .jar
      val compileResult = user.buildSources()
      compileResult.successful shouldBe true
      compileResult.errorCount shouldBe 0
      compileResult.warningCount shouldBe 0
      compileResult.messages.isEmpty shouldBe true
      new File(usersBaseDirPath + "/ExampleUser/bot/ScalatronBot.jar").exists() shouldBe true
    })
  }


  it should "be able to report errors when building invalid sources" in {
    runScalatron((scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
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
      compileResult.successful shouldBe false
      compileResult.errorCount shouldBe 2
      compileResult.warningCount shouldBe 0
      compileResult.messages.size shouldBe 2

      val sortedMessages = compileResult.messages.toArray.sortBy(_.sourceFile)
      val msg0 = sortedMessages(0)
      msg0.sourceFile shouldBe "1.scala"
      msg0.lineAndColumn shouldBe ((1, 1))
      msg0.severity shouldBe 2
      msg0.multiLineMessage shouldBe "expected class or object definition"

      val msg1 = sortedMessages(1)
      msg1.sourceFile shouldBe "2.scala"
      msg1.lineAndColumn shouldBe ((1, 12))
      msg1.severity shouldBe 2
      msg1.multiLineMessage shouldBe "expected class or object definition"
    })
  }


  //------------------------------------------------------------------------------------------
  // test publishing into tournament
  //------------------------------------------------------------------------------------------


  it should "be able to publish a jar built from sources into the tournament" in {
    runScalatron((scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
      val user = scalatron.createUser("ExampleUser", "", sourceFiles)
      user.buildSources()

      // publish the bot into the tournament plug-in directory
      user.publish()
      new File(pluginBaseDirPath + "/ExampleUser/ScalatronBot.jar").exists() shouldBe true

      // ... if we now called scalatron.run, the tournament should pick up the plug-in
    })
  }


  //------------------------------------------------------------------------------------------
  // test publishing into sandbox
  //------------------------------------------------------------------------------------------


  it should "be able to run a sandboxed game using a jar built from sources" in {
    runScalatron((scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
      val user = scalatron.createUser("ExampleUser", "", sourceFiles)
      user.buildSources()

      // create a sandbox game
      val sandbox = user.createSandbox(
        Map(
          "-x" -> "50",
          "-y" -> "50",
          "-perimeter" -> "open",
          "-walls" -> "20",
          "-snorgs" -> "20",
          "-fluppets" -> "20",
          "-toxifera" -> "20",
          "-zugars" -> "20"
        )
      )

      // simulate the sandbox game by performing 10 single-steps
      val initialSandboxState = sandbox.initialState
      val sandboxState = initialSandboxState.step(10)

      // verify the sandbox game state after 100 steps
      sandboxState.time shouldBe 10
      val entities = sandboxState.entities
      entities.size shouldBe 1

      val masterEntity = entities.find(_.isMaster).get
      masterEntity.isMaster shouldBe true
      masterEntity.name shouldBe "ExampleUser"

      // extract bot's most recent view
      // entities.foreach(e => println(e.mostRecentControlFunctionInput))

      // extract bot's most recent log output
      // entities.foreach(e => println(e.debugOutput))
    })
  }




  //------------------------------------------------------------------------------------------
  // test sample code management
  //------------------------------------------------------------------------------------------

  "Scalatron API running against a temporary /samples directory" should "initially find no samples" in {
    runScalatron((scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
      scalatron.samples.isEmpty shouldBe true
      scalatron.sample("SampleA").isEmpty shouldBe true
    })
  }

  it should "be able to create a new sample from given sources" in {
    runScalatron((scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
      // create a sample
      val createdSample = scalatron.createSample("SampleA", sourceFiles)
      createdSample.name shouldBe "SampleA"

      // enumerate samples again
      val samples = scalatron.samples
      samples.size shouldBe 1
      samples.map(_.name).mkString(",") shouldBe "SampleA"

      // retrieve created sample
      val retrievedSampleOpt = scalatron.sample("SampleA")
      retrievedSampleOpt.isDefined shouldBe true
      val retrievedSample = retrievedSampleOpt.get
      val sampleFiles = retrievedSample.sourceFiles
      sampleFiles.size shouldBe 1
      sampleFiles.head.filename shouldBe "Bot.scala"

      // we could now push the sample code into the user's workspace with user.updateSources()
    })
  }

  it should "be able to delete a newly created sample" in {
    runScalatron((scalatron: Scalatron, usersBaseDirPath: String, samplesBaseDirPath: String, pluginBaseDirPath: String) => {
      val createdSample = scalatron.createSample("SampleA", sourceFiles)
      scalatron.samples.size shouldBe 1

      createdSample.delete()
      scalatron.samples.isEmpty shouldBe true
    })
  }
}
