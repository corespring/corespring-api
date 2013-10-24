import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
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
    sources in doc in Compile := List())

  val cred = {
    val envCredentialsPath = System.getenv("CREDENTIALS_PATH")
    val path = if (envCredentialsPath != null) envCredentialsPath else Seq(Path.userHome / ".ivy2" / ".credentials").mkString
    val f: File = file(path)
    println("[credentials] check file: : " + f.getAbsolutePath)
    if (f.exists()) {
      println("[credentials] using credentials file")
      Credentials(f)
    } else {
      //https://devcenter.heroku.com/articles/labs-user-env-compile
      println("[credentials] using credentials env vars - you need to have: user-env-compile enabled in heroku")

      def repoVar(s: String) = System.getenv("ARTIFACTORY_" + s)
      val args = Seq("REALM", "HOST", "USER", "PASS").map(repoVar)
      println("[credentials] args: " + args)
      Credentials(args(0), args(1), args(2), args(3))
    }
  }

  val builders = new Builders(appName, org, appVersion, ScalaVersion)

  val customImports = Seq(
    "scala.language.reflectiveCalls",
    "se.radley.plugin.salat.Binders._",
    "org.corespring.platform.data.mongo.models.VersionedId",
    "org.bson.types.ObjectId",
    "org.corespring.platform.core.models.versioning.VersionedIdImplicits.Binders._")

  val apiUtils = builders.lib("api-utils").settings(
    libraryDependencies ++= Seq(specs2 % "test", playFramework, salatPlay, playJson % "test"),
    Keys.fork in Test := forkInTests).settings(disableDocsSettings: _*)

  /** Any shared test helpers in here */
  val testLib = builders.testLib("test-helpers").settings(
    libraryDependencies ++= Seq(specs2 % "test->compile", playFramework, playTest, salatPlay)).settings(disableDocsSettings: _*)

  val assets = builders.lib("assets")
    .settings(
      libraryDependencies ++= Seq(specs2 % "test", playS3, assetsLoader, corespringCommonUtils),
      credentials += cred)
    .dependsOn(apiUtils)
    .settings(disableDocsSettings: _*)

  /** Core data model */
  val core = builders.lib("core").settings(
    libraryDependencies ++= Seq(
      salatPlay,
      corespringQti,
      rhinos,
      rhino,
      corespringCommonUtils,
      salatVersioningDao,
      specs2 % "test",
      playS3,
      playFramework,
      securesocial,
      assetsLoader,
      mockito,
      playTest % "test",
      scalaFaker),
    Keys.fork in Test := forkInTests,
    parallelExecution.in(Test) := false,
    credentials += cred).dependsOn(assets, testLib % "test->compile").settings(disableDocsSettings: _*)

  val playerLib = builders.lib("player-lib")
    .settings(
      libraryDependencies ++= Seq(corespringCommonUtils, playFramework, specs2, scalaFaker % "test"))
    .dependsOn(core)
    .settings(disableDocsSettings: _*)


  val buildInfo = TaskKey[Unit]("build-client", "runs client installation commands")

  val buildInfoTask = buildInfo <<= (classDirectory in Compile, name, version, streams) map {
    (base, n, v, s) =>
      s.log.info("[buildInfo] ---> write build properties file] on " + base.getAbsolutePath)
      val file = base / "buildInfo.properties"
      val commitHash : String = Process("git rev-parse --short HEAD").!!.trim
      val branch : String = Process("git rev-parse --abbrev-ref HEAD").!!.trim
      val formatter = DateTimeFormat.forPattern("HH:mm dd MMMM yyyy");
      val date =formatter.print(DateTime.now)
      val contents = "commit.hash=%s\nbranch=%s\nversion=%s\ndate=%s".format(commitHash, branch, v, date)
      IO.write(file, contents)
  }

  val commonViews = builders.web("common-views").settings(
    buildInfoTask,
    (packagedArtifacts) <<= (packagedArtifacts) dependsOn buildInfo,
    libraryDependencies ++= Seq(playJson % "test")
  ).dependsOn(core).settings(disableDocsSettings: _*)

  /**client logging*/
  val clientLogging = builders.web("client-logging").settings(
    libraryDependencies ++= Seq(playFramework, scalaz)
  ).dependsOn(apiUtils)

  /** The public play module */
  val public = builders.web("public").settings(
    libraryDependencies ++= Seq(playFramework, securesocial),
    routesImport ++= customImports,
    parallelExecution.in(Test) := false,
    Keys.fork.in(Test) := forkInTests).dependsOn(commonViews, core % "compile->compile;test->test", playerLib, testLib % "test->compile")
    .aggregate(commonViews).settings(disableDocsSettings: _*)

  val main = play.Project(appName, appVersion, Dependencies.all)
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
  ).settings(MongoDbSeederPlugin.newSettings ++ Seq(testUri := "mongodb://localhost/api", testPaths := "conf/seed-data/test"): _*)
    .dependsOn(public, playerLib, core % "compile->compile;test->test", apiUtils, commonViews, testLib % "test->compile", clientLogging % "compile->compile;test->test")
    .aggregate(public, playerLib, core, apiUtils, commonViews, testLib, clientLogging).settings(disableDocsSettings: _*)

}
