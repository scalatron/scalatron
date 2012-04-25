import sbt._
import Keys._
import com.github.retronym.SbtOneJar._

object build extends Build {
    def standardSettings = Seq(exportJars := true) ++ Defaults.defaultSettings ++ src ++ Seq (
        artifactName in oneJar <<= moduleName(_ => (_,_,a) => "%s.%s" format (a.name, a.extension))
    )

    lazy val all = Project(
        id        = "all",
        base      = file("."),
        settings  = standardSettings ++ Seq(distTask),
        aggregate = Seq(main, cli, markdown)
    )

    lazy val src = Seq(
        scalaSource in Compile <<= baseDirectory / "src",
        scalaSource in Test <<= baseDirectory / "test"
    )

    lazy val main = Project("Scalatron", file("Scalatron"),
        settings = standardSettings ++ oneJarSettings ++ Seq(
            libraryDependencies ++= Seq(  
                "org.scala-lang" % "scala-compiler" % "2.9.1",
                "com.typesafe.akka" % "akka-actor" % "2.0",
                "org.eclipse.jetty.aggregate" % "jetty-webapp" % "7.6.2.v20120308" intransitive,
                "org.codehaus.jackson" % "jackson-jaxrs" % "1.9.2",
                "com.sun.jersey" % "jersey-bundle" % "1.12",
                "javax.servlet" % "servlet-api" % "2.5",
                "org.scalatest" %% "scalatest" % "1.7.2" % "test",
                "org.testng" % "testng" % "6.5.1" % "test"
            ),
            resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
        )
    )

    lazy val cli = Project("ScalatronCLI", file("ScalatronCLI"),
        settings = standardSettings ++ oneJarSettings ++ Seq(
            libraryDependencies ++= Seq(
                "org.apache.httpcomponents" % "httpclient" % "4.1.3"
            )
        )
    )

    lazy val markdown = Project("ScalaMarkdown", file("ScalaMarkdown"),
        settings = standardSettings ++ Seq(
          scalaSource in Compile <<= baseDirectory / "src",
          scalaSource in Test <<= baseDirectory / "test/scala",
          resourceDirectory in Test <<= baseDirectory / "test/resources"
        ) ++ oneJarSettings ++ Seq(
            libraryDependencies ++= Seq(
                "org.scala-tools.testing" %% "specs" % "1.6.9",
                "commons-io" % "commons-io" % "2.1",
                "commons-lang" % "commons-lang" % "2.6"
            )
        )
    )

    val dist = TaskKey[Unit]("dist", "Makes the distribution zip file")
    val distTask = dist <<= (scalaVersion) map { (version) =>
        val distDir = file("dist")

	// clean distribution directory
        println("Deleting dist directory")
        IO delete distDir

	// create new distribution directory
        IO createDirectory distDir
        val scalatronDir = file("Scalatron")

	// generate HTML from Markdown, for /doc and /devdoc
        for (dir <- List("doc", "devdoc")) {
            val docDir = scalatronDir / dir
            val htmlDir = distDir / dir / "html"
            IO createDirectory htmlDir
            for (doc <- file(docDir + "/markdown").listFiles if doc.getName.endsWith(".md")) {
                println ("Generating html for " + doc.getName)
                Seq("java", "-Xmx1G", "-jar", "ScalaMarkdown/target/scala-" + version + "/scalamarkdown.jar", doc.getPath, htmlDir.getPath) !
            }
        }

	// TODO: generate Tutorial HTML from Markdown, from /doc/tutorial to /webui/tutorial
        // TODO: maybe best to remove /webui/tutorial from the /Scalatron source folder, then
        // this is not good: IO.copyDirectory(scalatronDir / "doc/tutorial", distDir / "webui/tutorial")

        for (fileToCopy <- List("Readme.txt", "License.txt")) {
            IO.copyFile(scalatronDir / fileToCopy, distDir / fileToCopy)
        }
        for (dirToCopy <- List("samples", "webui", "doc/pdf", "bots")) {
            println("Copying " + dirToCopy)
            IO.copyDirectory(scalatronDir / dirToCopy, distDir / dirToCopy)
        }
        val versionString = "scala-" + version
        for (jar <- List("Scalatron", "ScalatronCLI")) {
            IO.copyFile(file(jar) / "target" / versionString / (jar.toLowerCase + ".jar"), distDir / "bin" / (jar + ".jar"))
        }

        // TODO: zip into something like "scalatron-0.9.8.4.zip" when done
    } dependsOn (oneJar in main, oneJar in cli, oneJar in markdown)

}