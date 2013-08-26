import play.Project._
import sbt.Keys._
import sbt._
import MongoDbSeederPlugin._


/**
 * Note: We are getting cross-versioning errors - they don't have an impact on the build
 * and will be picked up when we migrate to a newer version of play. So the choice is to ignore them for now.
 * @see: https://groups.google.com/forum/#!topic/play-framework-dev/vXbkCEvJrkQ
 */
object Build extends sbt.Build {

  import Dependencies._

  val appName = "corespring"
  val appVersion = "1.0"
  val ScalaVersion = "2.10.2"
  val org = "org.corespring"

  val forkInTests = true


  val disableDocsSettings = Seq(
    // disable publishing the main API jar
    publishArtifact in (Compile, packageDoc) := false,
    // disable publishing the main sources jar
    publishArtifact in (Compile, packageSrc) := false,
    sources in doc in Compile := List()
  )

  //TODO: this isn't working atm :: scalaVersion in ThisBuild := ScalaVersion


  val cred = {

    val envCredentialsPath = System.getenv("CREDENTIALS_PATH")
    val path = if(envCredentialsPath != null) envCredentialsPath else Seq(Path.userHome / ".ivy2"/ ".credentials").mkString
    val f : File =  file(path)
    println("[credentials] check file: : " + f.getAbsolutePath)
    if(f.exists()){
      println("[credentials] using credentials file")
      Credentials(f)
    } else {
      //https://devcenter.heroku.com/articles/labs-user-env-compile
      println("[credentials] using credentials env vars - you need to have: user-env-compile enabled in heroku")

      def repoVar(s:String) = System.getenv("ARTIFACTORY_" + s)
      val args = Seq("REALM", "HOST", "USER", "PASS").map(repoVar)
      println("[credentials] args: " + args)
      Credentials( args(0), args(1), args(2), args(3) )
    }
  }


  val builders = new Builders(appName, org, appVersion, ScalaVersion)

  val customImports = Seq(
    "scala.language.reflectiveCalls",
    "se.radley.plugin.salat.Binders._",
    "org.corespring.platform.data.mongo.models.VersionedId",
    "org.bson.types.ObjectId",
    "org.corespring.platform.core.models.versioning.VersionedIdImplicits.Binders._")



  val commonUtils = builders.lib("common-utils").settings(
    libraryDependencies ++= Seq(specs2 % "test", playFramework, salatPlay, playJson % "test"),
      Keys.fork in Test := forkInTests
  ).settings(disableDocsSettings : _*)

  /** Any shared test helpers in here */
  val testLib = builders.testLib("test-helpers").settings(
    libraryDependencies ++= Seq(specs2 % "test->compile", playFramework, playTest, salatPlay)
  ).settings(disableDocsSettings : _*)

  /** The Qti library */
  //TODO: only depends on commonUtils for PackageLogging - remove
  val qti = builders.lib("qti").settings(
    libraryDependencies ++= Seq(specs2 % "test", salatPlay, playJson % "test"),
    Keys.fork in Test := forkInTests
  ).dependsOn(commonUtils, testLib % "test->compile").settings(disableDocsSettings : _*)

  val assets = builders.lib("assets").settings(
    libraryDependencies ++= Seq(specs2 % "test", playS3, assetsLoader),
    credentials += cred
  ).dependsOn(commonUtils).settings(disableDocsSettings : _*)

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
      Keys.fork in Test := forkInTests,
      parallelExecution.in(Test) := false,
      credentials += cred
   ).dependsOn(assets,commonUtils, qti, testLib % "test->compile").settings(disableDocsSettings : _*)


  val playerLib = builders.lib("player-lib").settings(
    libraryDependencies ++= Seq(playFramework, specs2 % "test")
  ).dependsOn(core, commonUtils).settings(disableDocsSettings : _*)

  val commonViews = builders.web("common-views").settings(
    libraryDependencies ++= Seq(playJson % "test")
  ).dependsOn(core).settings(disableDocsSettings : _*)

  /** The public play module */
  val public = builders.web("public").settings(
    libraryDependencies ++= Seq(playFramework, securesocial),
    routesImport ++= customImports,
    parallelExecution.in(Test) := false,
    Keys.fork.in(Test) := forkInTests
  ).dependsOn(commonViews,core %"compile->compile;test->test", playerLib, testLib % "test->compile")
  .aggregate(commonViews).settings(disableDocsSettings : _*)

  val main = play.Project(appName, appVersion, Dependencies.all )
    .settings(
    scalaVersion := ScalaVersion,
    parallelExecution.in(Test) := false,
    routesImport ++= customImports,
    templatesImport ++= Seq("org.bson.types.ObjectId", "org.corespring.platform.data.mongo.models.VersionedId"),
    resolvers ++= Dependencies.Resolvers.all,
    credentials += cred,
    Keys.fork.in(Test) := forkInTests,
    scalacOptions ++= Seq("-feature", "-deprecation"),
    (test in Test) <<= (test in Test).map(Commands.runJsTests)
  ).settings( MongoDbSeederPlugin.newSettings  ++ Seq(testUri := "mongodb://localhost/api", testPaths := "conf/seed-data/test") : _* )
   .dependsOn(public, playerLib, qti, core % "compile->compile;test->test", commonUtils, commonViews, testLib % "test->compile")
   .aggregate(public, playerLib, qti, core, commonUtils, commonViews, testLib ).settings(disableDocsSettings : _*)

}
