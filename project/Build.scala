import play.Project._
import sbt.Keys._
import sbt._
import MongoDbSeederPlugin._


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
    libraryDependencies ++= Seq(specs2 % "test", playFramework, salatPlay, playJson % "test"),
      Keys.fork in Test := false
  )

  /** Any shared test helpers in here */
  val testLib = builders.testLib("test-helpers").settings(
    libraryDependencies ++= Seq(specs2 % "test->compile", playFramework, playTest, salatPlay)
  )

  /** The Qti library */
  val qti = builders.lib("qti").settings(
    libraryDependencies ++= Seq(specs2 % "test", salatPlay, playJson % "test"),
    Keys.fork in Test := false
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
      Keys.fork in Test := false,
      parallelExecution.in(Test) := false,
      testOptions in Test += Tests.Setup{ () =>
        println( scala.Console.BLUE + "-------------> setup core "  + scala.Console.RESET)
        MongoDbSeederPlugin.seed("mongodb://localhost/api", "conf/seed-data/test", "seed-main", "INFO")
      }
   ).dependsOn(commonUtils, qti, testLib % "test->compile")


  val commonViews = builders.web("common-views").settings(
    libraryDependencies ++= Seq(playJson % "test")
  ).dependsOn(core)

  val main = play.Project(appName, appVersion, Dependencies.all )

    .settings(
      MongoDbSeederPlugin.newSettings ++ Seq( testPaths := "conf/seed-data/test", testUri := "mongodb://localhost/api" ) : _* )
    .settings(
    scalaVersion := ScalaVersion,
    parallelExecution.in(Test) := false,
    routesImport ++= customImports,
    templatesImport ++= Seq("org.bson.types.ObjectId", "org.corespring.platform.data.mongo.models.VersionedId"),
    resolvers ++= Dependencies.Resolvers.all,
    Keys.fork.in(Test) := false,
    scalacOptions ++= Seq("-feature", "-deprecation"),
    (test in Test) <<= (test in Test).map(Commands.runJsTests),
    testOptions in Test += Tests.Setup{ () =>
      println( scala.Console.BLUE + "-------------> setup " + appName + scala.Console.RESET)
      MongoDbSeederPlugin.seed("mongodb://localhost/api", "conf/seed-data/test", "seed-main", "INFO")
    },
    testOptions in Test += Tests.Cleanup{ () =>
      println( scala.Console.BLUE + "-------------> cleanup " + appName + scala.Console.RESET)
      MongoDbSeederPlugin.unseed("mongodb://localhost/api", "conf/seed-data/test", "seed-main", "INFO")
    }
  ).dependsOn(qti, core % "compile->compile;test->test", commonUtils, commonViews, testLib % "test->compile")
   .aggregate(qti, core, commonUtils, commonViews, testLib )


}
