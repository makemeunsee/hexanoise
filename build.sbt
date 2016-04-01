enablePlugins(ScalaJSPlugin)

name := "Scrambl - Scala.js"

version := "1.0"

scalaVersion := "2.11.8"

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature")

scalaJSStage in Global := FastOptStage

resolvers += sbt.Resolver.bintrayRepo("denigma", "denigma-releases")

libraryDependencies += "org.denigma" %%% "threejs-facade" % "0.0.71-0.1.5"

libraryDependencies += "org.scala-lang.modules" % "scala-async_2.11" % "0.9.6-RC2"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.12.4" % "test"