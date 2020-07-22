import sbt._
import Keys._
import sbtassembly.AssemblyPlugin.autoImport._
import scala.sys.process._

val compilerVersion = "2.13.2"

lazy val libAkkaActors = "com.typesafe.akka" %% "akka-actor" % "2.6.8"

def standardSettings = Defaults.coreDefaultSettings ++ src ++ Seq(
  organization := "Scalatron",
  name := "Scalatron",
  version in Global := "1.1.0.2",
  scalaVersion := "2.13.2",
  scalaVersion := compilerVersion,
  scalacOptions := Seq("-language:postfixOps"),
  assemblyMergeStrategy in assembly := {
    case "plugin.properties" => MergeStrategy.first
    case "about.html"        => MergeStrategy.first
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)

lazy val implVersion = Seq(packageOptions := version.map { scalatronVersion =>
  Seq(Package.ManifestAttributes(("Implementation-Version", scalatronVersion)))
}.value)

lazy val all = (project in file("."))
  .settings(standardSettings ++ Seq(distTask))
  .aggregate(main, cli, markdown, referenceBot, tagTeamBot)

lazy val src = Seq(
  Compile / scalaSource := baseDirectory.value / "src",
  Test / scalaSource := baseDirectory.value / "test"
)

lazy val core = (project in file("ScalatronCore")).settings(
  standardSettings ++ Seq(libraryDependencies ++= Seq(libAkkaActors)) ++ Seq(
    assembly / assemblyJarName := "ScalatronCore.jar" // , logLevel in assembly := Level.Debug
  )
)

lazy val botwar = (project in file("BotWar"))
  .settings(
    standardSettings ++ Seq(libraryDependencies ++= Seq(libAkkaActors)) ++ Seq(
      assembly / assemblyJarName := "BotWar.jar" // , logLevel in assembly := Level.Debug
    )
  )
  .dependsOn(core)

lazy val main = (project in file("Scalatron"))
  .settings(
    standardSettings ++ Seq(
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-compiler" % compilerVersion,
        libAkkaActors,
        "org.eclipse.jetty.aggregate" % "jetty-webapp" % "7.6.2.v20120308" intransitive,
        "org.codehaus.jackson" % "jackson-jaxrs" % "1.9.2",
        "com.sun.jersey" % "jersey-bundle" % "1.12",
        "javax.servlet" % "servlet-api" % "2.5",
        "org.eclipse.jgit" % "org.eclipse.jgit" % "1.3.0.201202151440-r",
        "org.eclipse.jgit" % "org.eclipse.jgit.http.server" % "1.3.0.201202151440-r",
        "org.testng" % "testng" % "6.5.1" % "test",
        "org.specs2" %% "specs2-core" % "4.10.0" % "test"
      ),
      resolvers ++= Seq(
        "JGit Repository" at "http://download.eclipse.org/jgit/maven",
        "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
      )
    ) ++ Seq(
      assembly / assemblyJarName := "Scalatron.jar" // , logLevel in assembly := Level.Debug
    )
  )
  .dependsOn(botwar)

lazy val cli = (project in file("ScalatronCLI"))
  .settings(
    standardSettings ++ Seq(
      libraryDependencies ++= Seq(
        "org.apache.httpcomponents" % "httpclient" % "4.1.3",
        "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"
      )
    ) ++ Seq(assembly / assemblyJarName := "ScalatronCLI.jar")
  )

lazy val markdown = (project in file("ScalaMarkdown"))
  .settings(
    standardSettings ++ Seq(
      Compile / scalaSource := baseDirectory.value / "src",
      Test / scalaSource := baseDirectory.value / "test/scala",
      Test / resourceDirectory := baseDirectory.value / "test/resources"
    ) ++ Seq(
      libraryDependencies ++= Seq(
        "org.specs2" %% "specs2-core" % "4.10.0" % "test",
        "commons-io" % "commons-io" % "2.3",
        "org.apache.commons" % "commons-lang3" % "3.1"
      )
    ) ++ Seq(assembly / assemblyJarName := "ScalaMarkdown.jar")
  )

lazy val samples =
  (IO
    .listFiles(file("Scalatron") / "samples"))
    .filter(!_.isFile)
    .map { sample: File =>
      sample.getName -> Project(sample.getName.replace(" ", ""), sample)
        .settings(
          Defaults.coreDefaultSettings ++ Seq(
            Compile / scalaSource := baseDirectory.value / "src",
            packageBin / artifactName := ((_, _, _) => "ScalatronBot.jar"),
            scalaVersion := compilerVersion
          )
        )
    }
    .toMap

// TODO How can we do this automatically?!?
lazy val referenceBot = samples("Example Bot 01 - Reference")
lazy val tagTeamBot = samples("Example Bot 02 - TagTeam")

val dist = TaskKey[Unit]("dist", "Makes the distribution zip file")
val distTask = dist := {
  (assembly in core).value
  (assembly in botwar).value
  (assembly in main).value
  (assembly in cli).value
  (assembly in markdown).value
  (packageBin in Compile in referenceBot).value
  (packageBin in Compile in tagTeamBot).value

  println("Beginning distribution generation...")
  val distDir = file("dist")

  println("with scalaVersion = " + version)

  // clean distribution directory
  println("Deleting /dist directory...")
  IO delete distDir

  // create new distribution directory
  println("Creating /dist directory...")
  IO createDirectory distDir
  val scalatronDir = file("Scalatron")

  println("Copying Readme.txt and License.txt...")
  for (fileToCopy <- List("Readme.txt", "License.txt")) {
    IO.copyFile(scalatronDir / fileToCopy, distDir / fileToCopy)
  }

  for (dirToCopy <- List("webui", "doc/pdf")) {
    println("Copying " + dirToCopy)
    IO.copyDirectory(scalatronDir / dirToCopy, distDir / dirToCopy)
  }

  val distSamples = distDir / "samples"
  val targetVersion =
    scalaVersion.value.split("\\.").toList.take(2).mkString(".")
  def sampleJar(sample: Project) =
    sample.base / ("target/scala-%s/ScalatronBot.jar" format targetVersion)
  for (sample <- samples.values) {
    if (sampleJar(sample).exists) {
      println("Copying " + sample.base)
      IO.copyDirectory(
        sample.base / "src",
        distSamples / sample.base.getName / "src"
      )
      IO.copyFile(
        sampleJar(sample),
        distSamples / sample.base.getName / "ScalatronBot.jar"
      )
    }
  }

  println("Copying Reference bot to /bots directory...")
  IO.copyFile(
    sampleJar(referenceBot),
    distDir / "bots" / "Reference" / "ScalatronBot.jar"
  )

  def markdownExecute(docDir: File, htmlDir: File) = {
    Seq(
      "java",
      "-Xmx1G",
      "-jar",
      "ScalaMarkdown/target/scala-%s/ScalaMarkdown.jar" format targetVersion,
      docDir.getPath,
      htmlDir.getPath
    ) !
  }

  // generate HTML from Markdown, for /doc and /devdoc
  println("Generating /dist/doc/html from /doc/markdown...")
  markdownExecute(scalatronDir / "doc/markdown", distDir / "doc/html")

  println("Generating /webui/tutorial from /dev/tutorial...")
  markdownExecute(scalatronDir / "doc/tutorial", distDir / "webui/tutorial")

  for (jar <- List("Scalatron", "ScalatronCLI", "ScalatronCore", "BotWar")) {
    IO.copyFile(
      file(jar) / "target" / ("scala-%s" format targetVersion) / (jar + ".jar"),
      distDir / "bin" / (jar + ".jar")
    )
  }

  // This is ridiculous, there has to be be an easier way to zip up a directory
  val zipFileName = "scalatron-%s.zip" format version.value
  println("Zipping up /dist into " + zipFileName + "...")
  def zip(srcDir: File, destFile: File, prepend: String) = {
    val allDistFiles = (srcDir ** "*").get.filter(_.isFile).map { f =>
      (f, prepend + IO.relativize(distDir, f).get)
    }
    IO.zip(allDistFiles, destFile)
  }
  zip(distDir, file("./" + zipFileName), "Scalatron/")
}
