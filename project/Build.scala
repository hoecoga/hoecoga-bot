import sbt.Keys._
import sbt._

object Build extends sbt.Build {
  import Settings._

  lazy val root = Project(
    id = "hoecoga-bot",
    base = file("."),
    settings = defaultSettings ++ akkaSettings ++ wsSettings ++ configSettings ++ httpSettings ++
      jsonSettings ++ quartzSettings ++ scoptSettings ++ guiceSettings ++ testSettings)
}

object Settings {
  val defaultSettings = Seq(
    name := "hoecoga-bot",
    version := "1.0",
    scalaVersion := "2.11.6",
    fork in Test := true,
    scalacOptions in (Compile, compile) ++= Seq("-Xlint", "-Xfatal-warnings", "-feature", "-unchecked", "-deprecation"))

  val akkaVersion = "2.3.9"

  val akkaSettings = Seq(
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    libraryDependencies += "com.typesafe.akka" %% "akka-persistence-experimental" % akkaVersion,
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.13",
    libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test")

  val wsSettings = Seq(libraryDependencies += "org.java-websocket" % "Java-WebSocket" % "1.3.0")

  val configSettings = Seq(libraryDependencies += "com.typesafe" % "config" % "1.2.1")

  val httpSettings = Seq(libraryDependencies += "org.scalaj" %% "scalaj-http" % "1.1.4")

  val jsonSettings = Seq(libraryDependencies += "com.typesafe.play" %% "play-json" % "2.3.8")

  val quartzSettings = Seq(libraryDependencies += "org.quartz-scheduler" % "quartz" % "2.2.1")

  val scoptSettings = Seq(
    libraryDependencies += "com.github.scopt" %% "scopt" % "3.3.0",
    resolvers += Resolver.sonatypeRepo("public"))

  val guiceSettings = Seq(libraryDependencies += "com.google.inject" % "guice" % "3.0")

  val testSettings = Seq(
    libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
    libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.12.2",
    libraryDependencies += "org.mockito" % "mockito-core" % "1.10.19")
}
