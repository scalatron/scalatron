organization := "Scalatron"

name         := "Scalatron"

version in Global := "1.1.0.2"

scalaVersion := "2.10.4"

fork in ( Test, run ) := true
//scalacOptions in ThisBuild ++= Seq ("-feature", "-deprecation")
//autoScalaLibrary := false
