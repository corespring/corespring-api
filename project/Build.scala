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

  import Dependencies._


  val commonUtils = builders.lib("common-utils").settings(
    libraryDependencies ++= Seq(specs2 % "test", playFramework, salatPlay, playJson % "test")
  )

  /** Any shared test helpers in here */
  val testLib = builders.testLib("test-helpers").settings(
    libraryDependencies ++= Seq(specs2 % "test->compile", playFramework, playTest, salatPlay)
  )

  /** The Qti library */
  val qti = builders.lib("qti").settings(
    libraryDependencies ++= Seq(specs2 % "test", salatPlay, playJson % "test")
  ).dependsOn(commonUtils, testLib % "test->compile")

  /** Core data model */
  val core = builders.lib("core").settings(
    libraryDependencies ++= Seq(
      salatPlay,
      salatVersioningDao,
      specs2 % "test",
      playS3,
      playFramework,
      securesocial,
      assetsLoader,
      mockito,
      playTest % "test"),

      testOptions in Test += Tests.Setup((l:ClassLoader) => println("------------> setup")),
      testOptions in Test += Tests.Cleanup((l:ClassLoader) => println("-------------> cleanup"))
  ).dependsOn(commonUtils, qti, testLib % "test->compile")


  val commonViews = builders.web("common-views").settings(
    libraryDependencies ++= Seq(playJson % "test")
  ).dependsOn(core)

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
    .dependsOn(qti, core, commonUtils, commonViews)
    .aggregate(qti, core, commonUtils, commonViews)
}
