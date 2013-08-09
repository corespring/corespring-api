import play.Project._
import sbt.Keys._
import sbt._
import MongoDbSeederPlugin._


object Build extends sbt.Build {



  import Dependencies._

  val appName = "corespring"
  val appVersion = "1.0"
  val ScalaVersion = "2.10.1"
  val org = "org.corespring"

  //TODO: this isn't working atm :: scalaVersion in ThisBuild := ScalaVersion


  val cred = {

    val f : File =  file( Seq(Path.userHome / ".ivy2"/ ".credentials").mkString )

    def env(k:String) = System.getenv(k)

    if(f.exists()){
      println("using credentials file")
      Credentials(f)
    } else {
      //https://devcenter.heroku.com/articles/labs-user-env-compile
      println("using credentials env vars - you need to have: user-env-compile enabled in heroku")
      Credentials(
        env("ARTIFACTORY_REALM"),
        env("ARTIFACTORY_HOST"),
        env("ARTIFACTORY_USER"),
        env("ARTIFACTORY_PASS") )
    }
  }


  val builders = new Builders(appName, org, appVersion, ScalaVersion)

  val customImports = Seq(
    "import scala.language.reflectiveCalls",
    "se.radley.plugin.salat.Binders._",
    "org.corespring.platform.data.mongo.models.VersionedId",
    "org.bson.types.ObjectId",
    "org.corespring.platform.core.models.versioning.VersionedIdImplicits.Binders._")



  val commonUtils = builders.lib("common-utils").settings(
    libraryDependencies ++= Seq(specs2 % "test", playFramework, salatPlay, playJson % "test"),
      Keys.fork in Test := false
  )

  /** Any shared test helpers in here */
  val testLib = builders.testLib("test-helpers").settings(
    libraryDependencies ++= Seq(specs2 % "test->compile", playFramework, playTest, salatPlay)
  )

  /** The Qti library */
  //TODO: only depends on commonUtils for PackageLogging - remove
  val qti = builders.lib("qti").settings(
    libraryDependencies ++= Seq(specs2 % "test", salatPlay, playJson % "test"),
    Keys.fork in Test := false
  ).dependsOn(commonUtils, testLib % "test->compile")

  val assets = builders.lib("assets").settings(
    libraryDependencies ++= Seq(specs2 % "test", playS3, assetsLoader)
  ).dependsOn(commonUtils)

  /** Core data model */
  //TODO: This needs to be further broken down into smaller well defined libraries
  // -> data-models - only the case class data models
  // -> data-services - data model services (aka the objects that we have now)
  val core = builders.lib("core").settings(
    libraryDependencies ++= Seq(
      salatPlay,
      salatVersioningDao,
      specs2 % "test",
      playS3,
      playFramework,
      mongoDbSeeder,
      securesocial,
      assetsLoader,
      mockito,
      playTest % "test"),
      Keys.fork in Test := true,
      parallelExecution.in(Test) := false,
      credentials += cred
   ).dependsOn(assets,commonUtils, qti, testLib % "test->compile")


  val playerLib = builders.lib("player-lib").settings(
    libraryDependencies ++= Seq(playFramework, specs2 % "test")
  ).dependsOn(core, commonUtils)

  val commonViews = builders.web("common-views").settings(
    libraryDependencies ++= Seq(playJson % "test")
  ).dependsOn(core)

  /** The public play module */
  val public = builders.web("public").settings(
    libraryDependencies ++= Seq(playFramework, securesocial),
    routesImport ++= customImports,
    parallelExecution.in(Test) := false,
    Keys.fork.in(Test) := true
  ).dependsOn(commonViews,core %"compile->compile;test->test", playerLib, testLib % "test->compile")

  val main = play.Project(appName, appVersion, Dependencies.all )
    .settings(
    scalaVersion := ScalaVersion,
    parallelExecution.in(Test) := false,
    routesImport ++= customImports,
    templatesImport ++= Seq("org.bson.types.ObjectId", "org.corespring.platform.data.mongo.models.VersionedId"),
    resolvers ++= Dependencies.Resolvers.all,
    credentials += cred,
    Keys.fork.in(Test) := true,
    scalacOptions ++= Seq("-feature", "-deprecation"),
    (test in Test) <<= (test in Test).map(Commands.runJsTests)
  ).settings( MongoDbSeederPlugin.newSettings  ++ Seq(testUri := "mongodb://localhost/api", testPaths := "conf/seed-data/test") : _* )
   .dependsOn(public, playerLib, qti, core % "compile->compile;test->test", commonUtils, commonViews, testLib % "test->compile")
   .aggregate(public, playerLib, qti, core, commonUtils, commonViews, testLib )

}
