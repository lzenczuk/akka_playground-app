import sbt._
import Keys._

object Dependencies {
  val akkaVersion = "2.4.11"
  val scalaTestVersion = "3.0.0"

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaHttp = "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion
  val akkaActorTest = "com.typesafe.akka" %% "akka-testkit" % akkaVersion

  val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion % "test"

  val akkaDependencies: Seq[ModuleID] = Seq(
    akkaActor
  )

  val akkaHttpDependencies: Seq[ModuleID] = Seq(
    akkaHttp
  )

  val akkaTestDependencies: Seq[ModuleID] = Seq(
    akkaActorTest,
    scalaTest
  )

}
