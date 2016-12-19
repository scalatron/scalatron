import sbt._
import Versions._

object Dependencies {
  val cli = Seq(
    "org.apache.httpcomponents" % "httpclient"                % HttpClient,
    "org.scala-lang.modules"    %% "scala-parser-combinators" % ParserCombinators
  )

  def scalatron(scalaVersion: String) = Seq(
    "org.scala-lang"              % "scala-compiler"               % scalaVersion,
    "org.eclipse.jetty.aggregate" % "jetty-webapp"                 % Jetty intransitive (),
    "com.fasterxml.jackson.jaxrs" % "jackson-jaxrs-json-provider"  % Jackson,
    "com.sun.jersey"              % "jersey-bundle"                % Jersey exclude ("javax.ws.rs", "jsr311-api"),
    "javax.servlet"               % "servlet-api"                  % ServletApi,
    "org.eclipse.jgit"            % "org.eclipse.jgit"             % JGit,
    "org.eclipse.jgit"            % "org.eclipse.jgit.http.server" % JGit,
    "org.scalatest"               %% "scalatest"                   % Scalatest % Test,
    "org.specs2"                  %% "specs2-core"                 % Specs2    % Test
  )

  val markdown = Seq(
    "org.scalatest" %% "scalatest" % Scalatest % Test
  )

  val core = Seq(
    "com.typesafe.akka" %% "akka-actor" % Akka
  )

  val botWar = Seq.empty[ModuleID]
}
