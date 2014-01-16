import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import sbt.Keys._
import sbt._
import play.Project._
import MongoDbSeederPlugin._

object Build extends sbt.Build {

  import Dependencies._

  val appName = "corespring"
  val appVersion = "1.0"
  val ScalaVersion = "2.10.3"
  val org = "org.corespring"

  val forkInTests = false

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

  val sharedSettings = Seq(
    scalaVersion := ScalaVersion,
    parallelExecution.in(Test) := false,
    resolvers ++= Dependencies.Resolvers.all,
    credentials += cred,
    Keys.fork.in(Test) := forkInTests,
    scalacOptions ++= Seq("-feature", "-deprecation")
  ) ++ disableDocsSettings

  val builders = new Builders(appName, org, appVersion, ScalaVersion, sharedSettings)

  val customImports = Seq(
    "scala.language.reflectiveCalls",
    "se.radley.plugin.salat.Binders._",
    "org.corespring.platform.data.mongo.models.VersionedId",
    "org.bson.types.ObjectId",
    "org.corespring.platform.core.models.versioning.VersionedIdImplicits.Binders._")

  val apiUtils = builders.lib("api-utils")
    .settings(
      libraryDependencies ++= Seq(specs2 % "test", playFramework, salatPlay, playJson % "test"),
      Keys.fork in Test := forkInTests
    )

  /** Any shared test helpers in here */
  val testLib = builders.testLib("test-helpers")
    .settings(libraryDependencies ++= Seq(specs2 % "test->compile", playFramework, playTest, salatPlay))

  val assets = builders.lib("assets")
    .settings( libraryDependencies ++= Seq(specs2 % "test", playS3, playFramework, assetsLoader, corespringCommonUtils))
    .dependsOn(apiUtils)

  val qti = builders.lib("qti").settings(libraryDependencies ++= Seq(corespringCommonUtils, playFramework, playJson, salat, rhino, rhinos))

  /** Core data model */
  val core = builders.lib("core").settings(
    libraryDependencies ++= Seq(
      salatPlay,
      corespringCommonUtils,
      salatVersioningDao,
      specs2 % "test",
      playS3,
      playFramework,
      securesocial,
      assetsLoader,
      mockito,
      playTest % "test",
      scalaFaker))
    .dependsOn(assets, testLib % "test->compile", qti)

  val playerLib = builders.lib("player-lib")
    .settings(
      libraryDependencies ++= Seq(corespringCommonUtils, playFramework, specs2, scalaFaker % "test"))
    .dependsOn(core)
    .settings(disableDocsSettings: _*)

  val v2PlayerIntegration = builders.lib("v2-player-integration").settings(
    libraryDependencies ++= Seq(
      containerClientWeb,
      componentLoader,
      componentModel,
      mongoJsonService)
  ).dependsOn(core % "test->test;compile->compile", playerLib)



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
  ).dependsOn(core % "compile->compile;test->test")

  val clientLogging = builders.web("client-logging").settings(
    libraryDependencies ++= Seq(playFramework, scalaz)
  ).dependsOn(apiUtils,  core % "test->test" )

  val scormLib = builders.lib("scorm").settings(
    libraryDependencies ++= Seq(playFramework)
  ).dependsOn(core)

  val ltiLib = builders.lib("lti")
    .dependsOn(apiUtils, core % "compile->compile;test->compile;test->test")

  val v1Api = builders.web("v1-api").settings(
    libraryDependencies ++= Seq(casbah),
    templatesImport ++= TemplateImports.Ids,
    routesImport ++= customImports
  )
  .settings(MongoDbSeederPlugin.newSettings ++ Seq(MongoDbSeederPlugin.logLevel := "DEBUG", testUri := "mongodb://localhost/api", testPaths := "conf/seed-data/test"): _*)
  .dependsOn(core % "compile->compile;test->test", playerLib, scormLib, ltiLib)

  object TemplateImports{
    val Ids = Seq("org.bson.types.ObjectId", "org.corespring.platform.data.mongo.models.VersionedId")
  }


  val v1Player = builders.web("v1-player")
    .settings(
      templatesImport ++= TemplateImports.Ids,
      routesImport ++= customImports
    )
    .aggregate(qti, playerLib, v1Api, apiUtils, testLib ,core, commonViews)
    .dependsOn(qti, playerLib, v1Api, apiUtils, testLib % "test->compile", core % "test->compile;test->test", commonViews)

  val ltiWeb = builders.web("lti-web").settings(
    templatesImport ++= TemplateImports.Ids,
    routesImport ++= customImports)
    .aggregate(core, ltiLib, playerLib, v1Player)
    .dependsOn(ltiLib, playerLib, v1Player, testLib % "test->compile", core % "test->compile;test->test" )

  val public = builders.web("public").settings(
    libraryDependencies ++= Seq(playFramework, securesocial),
    routesImport ++= customImports)
    .dependsOn(commonViews, core % "compile->compile;test->test", playerLib, v1Player, testLib % "test->compile")
    .aggregate(commonViews).settings(disableDocsSettings: _*)

  val reports = builders.web("reports")
    .settings()
    .dependsOn(commonViews, core % "compile->compile;test->test")

  val scormWeb = builders.web("scorm-web").settings(
    routesImport ++= customImports
  ).dependsOn(core, scormLib, v1Player)

  lazy val integrationTestSettings = Seq(
    scalaSource in IntegrationTest <<= baseDirectory / "it",
    Keys.parallelExecution in IntegrationTest := false,
    Keys.fork in IntegrationTest := false,
    testOptions in IntegrationTest += Tests.Setup( () => println("Setup Integration Test yoohoo") ),
    testOptions in IntegrationTest += Tests.Cleanup( () => println("Cleanup Integration Test yoohoo") )
  )

  val main = builders.web(appName, Some(file(".")))
    .settings(
      routesImport ++= customImports,
      templatesImport ++= TemplateImports.Ids,
      libraryDependencies ++= Dependencies.all,
      templatesImport ++= Seq("org.bson.types.ObjectId", "org.corespring.platform.data.mongo.models.VersionedId"),
      resolvers ++= Dependencies.Resolvers.all,
      credentials += cred,
      Keys.fork.in(Test) := forkInTests,
      scalacOptions ++= Seq("-feature", "-deprecation"),
      (test in Test) <<= (test in Test).map(Commands.runJsTests)
     )
    .settings(MongoDbSeederPlugin.newSettings ++ Seq(MongoDbSeederPlugin.logLevel := "INFO", testUri := "mongodb://localhost/api", testPaths := "conf/seed-data/test"): _*)
    .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
    .settings(disableDocsSettings: _*)
    .configs( IntegrationTest )
    .settings( Defaults.itSettings : _*)
    .settings( integrationTestSettings : _* )
    .dependsOn(scormWeb, reports, public, ltiWeb, v1Api, v1Player, playerLib, core % "it->test;compile->compile", apiUtils, commonViews, testLib % "test->compile;test->test;it->test", v2PlayerIntegration, clientLogging % "compile->compile;test->test" )
    .aggregate(scormWeb, reports, public, ltiWeb, v1Api, v1Player, playerLib, core, apiUtils, commonViews, testLib, v2PlayerIntegration, clientLogging)
    addCommandAlias("gen-idea-project", ";update-classifiers;idea")
}
