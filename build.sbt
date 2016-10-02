organization := "Scalatron"

name := "Scalatron"

version := "1.2.0"

lazy val standardSettings = Seq( //Defaults.defaultSettings ++ src ++ Seq(
  scalaSource in Compile <<= baseDirectory / "src",
  scalaSource in Test <<= baseDirectory / "test",
  resourceDirectory in Test <<= baseDirectory / "test/resources",
  scalaVersion := "2.11.8",
  assemblyMergeStrategy in assembly := {
    case "plugin.properties" => MergeStrategy.first
    case "about.html" => MergeStrategy.first
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)

lazy val implVersion = Seq(
  packageOptions <<= version map {
    scalatronVersion => Seq(Package.ManifestAttributes(
      ("Implementation-Version", scalatronVersion)
    ))
  }
)

lazy val all = Project(
  id = "all",
  base = file("."),
  settings = standardSettings ++ Seq(distTask),
  aggregate = Seq(main, cli, markdown, referenceBot, tagTeamBot)
)

lazy val core = Project("ScalatronCore", file("ScalatronCore"),
  settings = standardSettings ++ Seq(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.4.10"
    ),
    assemblyJarName in assembly := "ScalatronCore.jar"
  )
)

lazy val botwar = Project("BotWar", file("BotWar"),
  settings = standardSettings ++ Seq(
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.10",
    assemblyJarName in assembly := "BotWar.jar"
  )
) dependsOn core

lazy val main = Project("Scalatron", file("Scalatron"),
  settings = standardSettings ++ Seq(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "com.typesafe.akka" %% "akka-actor" % "2.4.10",
      "org.eclipse.jetty.aggregate" % "jetty-webapp" % "7.6.2.v20120308" intransitive(),
      "org.codehaus.jackson" % "jackson-jaxrs" % "1.9.2",
      "com.sun.jersey" % "jersey-bundle" % "1.12",
      "javax.servlet" % "servlet-api" % "2.5",
      "org.eclipse.jgit" % "org.eclipse.jgit" % "1.3.0.201202151440-r",
      "org.eclipse.jgit" % "org.eclipse.jgit.http.server" % "1.3.0.201202151440-r",
      "org.scala-lang.modules" %% "scala-xml" % "1.0.6",

      "org.scalatest" %% "scalatest" % "2.2.5" % "test",
      "org.testng" % "testng" % "6.5.1" % "test",
      "org.specs2" %% "specs2-core" % "3.8.5-scalaz-7.1.10" % "test"
      //                "org.specs2" %% "specs2-scalaz-core" % "7.1.0" % "test"
    ),
    resolvers ++= Seq(
      "JGit Repository" at "http://download.eclipse.org/jgit/maven",
      "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
    ),
    assemblyJarName in assembly := "Scalatron.jar" // , logLevel in assembly := Level.Debug
  )
) dependsOn botwar

lazy val cli = Project("ScalatronCLI", file("ScalatronCLI"),
  settings = standardSettings ++ Seq(
    libraryDependencies ++= Seq(
      "org.apache.httpcomponents" % "httpclient" % "4.1.3",
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
      "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
    ),
    assemblyJarName in assembly := "ScalatronCLI.jar"
  )
)

lazy val markdown = Project("ScalaMarkdown", file("ScalaMarkdown"),
  settings = standardSettings ++ Seq(
    libraryDependencies ++= Seq(
      "org.scala-tools.testing" % "specs_2.10" % "1.6.9",
      "commons-io" % "commons-io" % "2.3",
      "org.apache.commons" % "commons-lang3" % "3.1"
    ),
    assemblyJarName in assembly := "ScalaMarkdown.jar"
  )
)

lazy val samples = IO.listFiles(file("Scalatron") / "samples") filter (!_.isFile) map {
  sample: File => sample.getName -> Project(sample.getName.replace(" ", ""), sample, settings = standardSettings ++ Seq(
    scalaSource in Compile <<= baseDirectory / "src",
    artifactName in packageBin := ((_, _, _) => "ScalatronBot.jar")
  ))
} toMap

lazy val referenceBot = samples("Example Bot 01 - Reference")
lazy val tagTeamBot = samples("Example Bot 02 - TagTeam")

val dist = TaskKey[Unit]("dist", "Makes the distribution zip file")
lazy val distTask = dist <<= (version, scalaVersion) map { (scalatronVersion, version) =>
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
  val targetVersion = version.split("\\.").toList.take(2).mkString(".")
  def sampleJar(sample: Project) = sample.base / ("target/scala-%s/ScalatronBot.jar" format targetVersion)
  for (sample <- samples.values) {
    if (sampleJar(sample).exists) {
      println("Copying " + sample.base)
      IO.copyDirectory(sample.base / "src", distSamples / sample.base.getName / "src")
      IO.copyFile(sampleJar(sample), distSamples / sample.base.getName / "ScalatronBot.jar")
    }
  }

  println("Copying Reference bot to /bots directory...")
  IO.copyFile(sampleJar(referenceBot), distDir / "bots" / "Reference" / "ScalatronBot.jar")

  def markdown(docDir: File, htmlDir: File) = {
    Seq("java", "-Xmx1G", "-jar", "ScalaMarkdown/target/scala-%s/ScalaMarkdown.jar" format targetVersion, docDir.getPath, htmlDir.getPath).!
  }

  // generate HTML from Markdown, for /doc and /devdoc
  println("Generating /dist/doc/html from /doc/markdown...")
  markdown(scalatronDir / "doc/markdown", distDir / "doc/html")

  println("Generating /webui/tutorial from /dev/tutorial...")
  markdown(scalatronDir / "doc/tutorial", distDir / "webui/tutorial")

  for (jar <- List("Scalatron", "ScalatronCLI", "ScalatronCore", "BotWar")) {
    IO.copyFile(file(jar) / "target" / ("scala-%s" format targetVersion) / (jar + ".jar"), distDir / "bin" / (jar + ".jar"))
  }

  // This is ridiculous, there has to be be an easier way to zip up a directory
  val zipFileName = "scalatron-%s.zip" format scalatronVersion
  println("Zipping up /dist into " + zipFileName + "...")
  def zip(srcDir: File, destFile: File, prepend: String) = {
    val allDistFiles = (srcDir ** "*").get.filter(_.isFile).map { f => (f, prepend + IO.relativize(distDir, f).get) }
    IO.zip(allDistFiles, destFile)
  }
  zip(distDir, file("./" + zipFileName), "Scalatron/")
} dependsOn (
  assembly in core,
  assembly in botwar,
  assembly in main,
  assembly in cli,
  assembly in markdown,
  packageBin in Compile in referenceBot,
  packageBin in Compile in tagTeamBot
  )
