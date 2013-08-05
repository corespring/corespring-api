import play.Project._
import sbt.Keys._
import sbt._


object Build extends sbt.Build {

  val appName = "corespring"
  val appVersion = "1.0"
  val ScalaVersion = "2.10.1"
  val org = "org.corespring"

  val builders = new Builders(appName, org, appVersion, ScalaVersion)

  val customImports = Seq("se.radley.plugin.salat.Binders._",
    "org.corespring.platform.data.mongo.models.VersionedId",
    "org.bson.types.ObjectId",
    "org.corespring.platform.core.models.versioning.VersionedIdImplicits.Binders._")

  /** Any shared test helpers in here */
  val testLib = builders.testLib("test-helpers")

  import Dependencies._


  /** Core data model */
  val core = builders.lib("core").settings(
    libraryDependencies ++= Seq(salatPlay, salatVersioningDao, specs2, playS3, playFramework, securesocial)
  )

  /** The Qti library */
  val qti = builders.lib("qti").settings(
    libraryDependencies ++= Seq(specs2, salatPlay, playJson % "test")
  ).dependsOn(core, testLib % "test->compile")


  val main = play.Project(appName, appVersion, Dependencies.all ).settings(
    scalaVersion := ScalaVersion,
    parallelExecution.in(Test) := false,
    routesImport ++= customImports,
    templatesImport ++= Seq("org.bson.types.ObjectId", "org.corespring.platform.data.mongo.models.VersionedId"),
    resolvers ++= Dependencies.Resolvers.all,
    Keys.fork.in(Test) := false,
    scalacOptions ++= Seq("-feature", "-deprecation"),
    (test in Test) <<= (test in Test).map(Commands.runJsTests)
  )
    .dependsOn(qti, core)
    .aggregate(qti, core)
}
