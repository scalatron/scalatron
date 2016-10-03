organization := "Scalatron"

name := "Scalatron"

version := "1.3.0"

lazy val targetJvm = SettingKey[String]("jvm-version", "The version of the JVM the build targets")

lazy val commonSettings = Seq( //Defaults.defaultSettings ++ src ++ Seq(
  scalaSource in Compile := baseDirectory.value / "src",
  scalaSource in Test := baseDirectory.value / "test",
  resourceDirectory in Test := baseDirectory.value / "test/resources",
  scalaVersion := "2.11.8",
  crossPaths := false,
  targetJvm := "1.8",
  parallelExecution in Test := false,
  scalacOptions ++= Seq("-target:jvm-" + targetJvm.value, "-feature", "-deprecation", "-unchecked", "-Xlint", "-Xfatal-warnings"),
  javacOptions ++= Seq("-source", targetJvm.value, "-target", targetJvm.value),
  externalResolvers := Seq(Resolver.jcenterRepo),
  assemblyMergeStrategy in assembly := {
    case "plugin.properties" => MergeStrategy.first
    case "about.html" => MergeStrategy.first
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)

lazy val lintingSettings = Seq(
  addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.15"),
  scalacOptions += "-P:linter:disable:UnusedParameter"
)

lazy val all = project.
  in(file(".")).
  settings(commonSettings).
  aggregate(Scalatron, ScalatronCLI, ScalaMarkdown, referenceBot, tagTeamBot)

lazy val ScalatronCore = project.
  in(file("ScalatronCore")).
  settings(
    commonSettings,
    lintingSettings,
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.10",
    assemblyJarName in assembly := "ScalatronCore.jar"
  )

lazy val BotWar = project.
  in(file("BotWar")).
  dependsOn(ScalatronCore).
  settings(
    commonSettings,
    lintingSettings,
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.10",
    assemblyJarName in assembly := "BotWar.jar"
  )

lazy val Scalatron = project.
  in(file("Scalatron")).
  dependsOn(ScalatronCore, BotWar).
  settings(
    commonSettings,
    lintingSettings,
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "com.typesafe.akka" %% "akka-actor" % "2.4.10",
      "org.eclipse.jetty.aggregate" % "jetty-webapp" % "7.6.2.v20120308" intransitive(),
      "org.codehaus.jackson" % "jackson-jaxrs" % "1.9.2",
      "com.sun.jersey" % "jersey-bundle" % "1.12",
      "javax.servlet" % "servlet-api" % "2.5",
      "org.eclipse.jgit" % "org.eclipse.jgit" % "4.5.0.201609210915-r",
      "org.eclipse.jgit" % "org.eclipse.jgit.http.server" % "4.5.0.201609210915-r",
      "org.scala-lang.modules" %% "scala-xml" % "1.0.6",

      "org.specs2" %% "specs2-core" % "3.8.5-scalaz-7.1.10" % Test
    ),
    assemblyJarName in assembly := "Scalatron.jar" // , logLevel in assembly := Level.Debug
  )

lazy val ScalatronCLI = project.
  in(file("ScalatronCLI")).
  settings(
    commonSettings,
    lintingSettings,
    libraryDependencies ++= Seq(
      "org.apache.httpcomponents" % "httpclient" % "4.1.3",
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
      "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
    ),
    assemblyJarName in assembly := "ScalatronCLI.jar"
  )

lazy val ScalaMarkdown = project.
  in(file("ScalaMarkdown")).
  settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.0.0" % Test
    ),
    assemblyJarName in assembly := "ScalaMarkdown.jar"
  )

lazy val samples = IO.listFiles(file("Scalatron") / "samples").filter(!_.isFile).map {
  sample: File => sample.getName -> Project(sample.getName.replace(" ", ""), sample, settings = commonSettings ++ Seq(
    artifactName in packageBin := ((_, _, _) => "ScalatronBot.jar")
  ))
}.toMap

lazy val referenceBot = samples("Example Bot 01 - Reference")
lazy val tagTeamBot = samples("Example Bot 02 - TagTeam")

lazy val dist = taskKey[Unit]("Makes the distribution zip file")
dist := {
  (assembly in ScalatronCore).value
  (assembly in BotWar).value
  (assembly in Scalatron).value
  (assembly in ScalatronCLI).value
  (assembly in ScalaMarkdown).value
  (packageBin in Compile in referenceBot).value
  (packageBin in Compile in tagTeamBot).value
  println("Beginning distribution generation...")
  val distDir = file("dist")

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
  def sampleJar(sample: Project) = sample.base / "target/ScalatronBot.jar"
  for (sample <- samples.values; if sampleJar(sample).exists) {
    println("Copying " + sample.base)
    IO.copyDirectory(sample.base / "src", distSamples / sample.base.getName / "src")
    IO.copyFile(sampleJar(sample), distSamples / sample.base.getName / "ScalatronBot.jar")
  }

  println("Copying Reference bot to /bots directory...")
  IO.copyFile(sampleJar(referenceBot), distDir / "bots" / "Reference" / "ScalatronBot.jar")

  def runmarkdown(docDir: File, htmlDir: File) = {
    Seq("java", "-Xmx1G", "-jar", "ScalaMarkdown/target/ScalaMarkdown.jar", docDir.getPath, htmlDir.getPath).!
  }

  // generate HTML from Markdown, for /doc and /devdoc
  println("Generating /dist/doc/html from /doc/markdown...")
  runmarkdown(scalatronDir / "doc/markdown", distDir / "doc/html")

  println("Generating /webui/tutorial from /dev/tutorial...")
  runmarkdown(scalatronDir / "doc/tutorial", distDir / "webui/tutorial")

  for (jar <- Seq("Scalatron", "ScalatronCLI", "ScalatronCore", "BotWar")) {
    IO.copyFile(file(jar) / "target" / (jar + ".jar"), distDir / "bin" / (jar + ".jar"))
  }

  // This is ridiculous, there has to be be an easier way to zip up a directory
  val zipFileName = s"scalatron-${version.value}.zip"
  println("Zipping up /dist into " + zipFileName + "...")
  def zip(srcDir: File, destFile: File, prepend: String) = {
    val allDistFiles = (srcDir ** "*").get.filter(_.isFile).map { f => (f, prepend + IO.relativize(distDir, f).get) }
    IO.zip(allDistFiles, destFile)
  }
  zip(distDir, file("./" + zipFileName), "Scalatron/")
}
