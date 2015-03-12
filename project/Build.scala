import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import sbt.Keys._
import sbt._
import play.Project._
import MongoDbSeederPlugin._

object Build extends sbt.Build {

  import Dependencies._
  import ComponentsBuilder._

  val appName = "corespring"
  val appVersion = "1.0"
  val ScalaVersion = "2.10.3"
  val org = "org.corespring"

  val forkInTests = false

  def getEnv(prop: String): Option[String] = {
    val env = System.getenv(prop)
    if (env == null) None else Some(env)
  }

  val disableDocsSettings = Seq(
    // disable publishing the main API jar
    publishArtifact in (Compile, packageDoc) := false,
    // disable publishing the main sources jar
    publishArtifact in (Compile, packageSrc) := false,
    sources in doc in Compile := List())

  val cred = {
    val envCredentialsPath = getEnv("CREDENTIALS_PATH")
    val path = envCredentialsPath.getOrElse(Seq(Path.userHome / ".ivy2" / ".credentials").mkString)
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
    scalacOptions ++= Seq("-feature", "-deprecation")) ++ disableDocsSettings

  val builders = new Builders(appName, org, appVersion, ScalaVersion, sharedSettings)

  val customImports = Seq(
    "scala.language.reflectiveCalls",
    "se.radley.plugin.salat.Binders._",
    "org.corespring.platform.data.mongo.models.VersionedId",
    "org.bson.types.ObjectId",
    "org.corespring.platform.core.models.versioning.VersionedIdImplicits.Binders._")

  val playJsonSalatUtils = builders.lib("play-json-salat-utils").settings(
    libraryDependencies ++= Seq(playJson, salat, specs2 % "test"))

  val apiUtils = builders.lib("api-utils")
    .settings(
      libraryDependencies ++= Seq(specs2 % "test", playFramework, salatPlay, playJson % "test"),
      Keys.fork in Test := forkInTests)

  /** Any shared test helpers in here */
  val testLib = builders.testLib("test-helpers")
    .settings(libraryDependencies ++= Seq(specs2 % "test->compile", playFramework, playTest, salatPlay))

  val assets = builders.lib("assets")
    .settings(libraryDependencies ++= Seq(specs2 % "test", playS3, playFramework, assetsLoader, corespringCommonUtils))
    .dependsOn(apiUtils)

  val qti = builders.lib("qti")
    .settings(libraryDependencies ++= Seq(specs2 % "test", playTest % "test", corespringCommonUtils, playFramework, salatPlay, playJson, salat, rhino, rhinos))
    .dependsOn(apiUtils)

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
      sprayCaching,
      playTest % "test",
      scalaFaker))
    .dependsOn(assets, testLib % "test->compile", qti, playJsonSalatUtils)

  val playerLib = builders.lib("player-lib")
    .settings(
      libraryDependencies ++= Seq(corespringCommonUtils, playFramework, specs2, playTest % "test", scalaFaker % "test"))
    .dependsOn(core)
    .settings(disableDocsSettings: _*)

  val buildInfo = TaskKey[Unit]("build-client", "runs client installation commands")

  val buildInfoTask = buildInfo <<= (classDirectory in Compile, name, version, streams) map {
    (base, n, v, s) =>
      s.log.info("[buildInfo] ---> write build properties file] on " + base.getAbsolutePath)
      val file = base / "buildInfo.properties"
      val commitHash: String = Process("git rev-parse --short HEAD").!!.trim
      val branch: String = Process("git rev-parse --abbrev-ref HEAD").!!.trim
      val formatter = DateTimeFormat.forPattern("HH:mm dd MMMM yyyy");
      val date = formatter.print(DateTime.now)
      val contents = "commit.hash=%s\nbranch=%s\nversion=%s\ndate=%s".format(commitHash, branch, v, date)
      IO.write(file, contents)
  }

  val commonViews = builders.web("common-views").settings(
    buildInfoTask,
    (packagedArtifacts) <<= (packagedArtifacts) dependsOn buildInfo,
    libraryDependencies ++= Seq(playJson % "test")).dependsOn(core % "compile->compile;test->test")

  val clientLogging = builders.web("client-logging").settings(
    libraryDependencies ++= Seq(playFramework, scalaz)).dependsOn(apiUtils, core % "test->test")

  val scormLib = builders.lib("scorm").settings(
    libraryDependencies ++= Seq(playFramework)).dependsOn(core)

  val ltiLib = builders.lib("lti")
    .dependsOn(apiUtils, core % "compile->compile;test->compile;test->test")

  val drafts = builders.lib("drafts").settings(
    libraryDependencies ++= Seq(specs2 % "test", jodaTime, jodaConvert, scalaz))

  val itemDrafts = builders.lib("item-drafts")
    .settings(
      libraryDependencies ++= Seq(specs2 % "test"))
    .dependsOn(core, drafts)
    .aggregate(core, drafts)

  /** Qti -> v2 transformers */
  val qtiToV2 = builders.lib("qti-to-v2").settings(
    libraryDependencies ++= Seq(playJson, rhino % "test")).dependsOn(core, qti, apiUtils, testLib % "test->compile")

  val v1Api = builders.web("v1-api").settings(
    libraryDependencies ++= Seq(casbah),
    templatesImport ++= TemplateImports.Ids,
    routesImport ++= customImports)
    .settings(MongoDbSeederPlugin.newSettings ++ Seq(
      MongoDbSeederPlugin.logLevel := "DEBUG",
      testUri := "mongodb://localhost/api",
      testPaths := "conf/seed-data/test,conf/seed-data/static"): _*)
    .dependsOn(core % "compile->compile;test->test", playerLib, scormLib, ltiLib, qtiToV2)

  /**
   * Error types
   */
  val v2Errors = builders.lib("v2-errors").settings(
    libraryDependencies ++= Seq(scalaz)).dependsOn(core)
  /**
   * All authentication code for v2 api + player/editor
   */
  val v2Auth = builders.lib("v2-auth").settings(
    libraryDependencies ++= Seq(specs2 % "test", mockito, mongoJsonService, scalaz))
    .dependsOn(testLib, v2Errors, core, playerLib, qtiToV2)

  val apiTracking = builders.lib("api-tracking")
    .settings(
      libraryDependencies ++= Seq(playFramework)).dependsOn(v2Auth)
    .dependsOn(v2Errors, core, playerLib, testLib % "test->compile")

  val itemImport = builders.web("item-import")
    .settings(libraryDependencies ++= Seq(playJson, jsonValidator, salatVersioningDao, mockito))
    .dependsOn(v2Auth, testLib % "test->compile", core % "test->compile;test->test", core)

  val draftsApi = builders.web("v2-api-drafts").dependsOn(itemDrafts)

  val v2Api = builders.web("v2-api")
    .settings(
      libraryDependencies ++= Seq(
        scalaz,
        mongoJsonService,
        salatVersioningDao,
        componentModel),
      routesImport ++= customImports)
    .dependsOn(
      v2Auth % "test->test;compile->compile",
      qtiToV2,
      core % "test->test;compile->compile",
      draftsApi)

  object TemplateImports {
    val Ids = Seq("org.bson.types.ObjectId", "org.corespring.platform.data.mongo.models.VersionedId")
  }

  val v1Player = builders.web("v1-player")
    .settings(
      templatesImport ++= TemplateImports.Ids,
      routesImport ++= customImports)
    .aggregate(qti, playerLib, v1Api, apiUtils, testLib, core, commonViews)
    .dependsOn(qti, playerLib, v1Api, apiUtils, testLib % "test->compile", core % "test->compile;test->test", commonViews)

  val devTools = builders.web("dev-tools").settings(
    routesImport ++= customImports,
    libraryDependencies ++= Seq(containerClientWeb, mongoJsonService)).dependsOn(v1Player, playerLib, core, v2Auth)

  /** Implementation of corespring container hooks */
  val v2PlayerIntegration = builders.lib("v2-player-integration").settings(
    libraryDependencies ++= Seq(
      containerClientWeb,
      componentLoader,
      componentModel,
      scalaz,
      mongoJsonService,
      playS3)).dependsOn(qtiToV2, v2Auth, core % "test->test;compile->compile", playerLib, devTools)

  val ltiWeb = builders.web("lti-web").settings(
    templatesImport ++= TemplateImports.Ids,
    routesImport ++= customImports)
    .aggregate(core, ltiLib, playerLib, v1Player)
    .dependsOn(ltiLib, playerLib, v1Player, testLib % "test->compile", core % "test->compile;test->test")

  val public = builders.web("public").settings(
    libraryDependencies ++= Seq(playFramework, securesocial),
    routesImport ++= customImports)
    .dependsOn(commonViews, core % "compile->compile;test->test", playerLib, v1Player, testLib % "test->compile")
    .aggregate(commonViews).settings(disableDocsSettings: _*)

  val reports = builders.web("reports")
    .settings(
      libraryDependencies ++= Seq(simplecsv))
    .dependsOn(commonViews, core % "compile->compile;test->test")

  val scormWeb = builders.web("scorm-web").settings(
    routesImport ++= customImports).dependsOn(core, scormLib, v1Player)

  val alwaysRunInTestOnly: String = " *TestOnlyPreRunTest*"

  lazy val integrationTestSettings = Seq(
    scalaSource in IntegrationTest <<= baseDirectory / "it",
    Keys.parallelExecution in IntegrationTest := false,
    Keys.fork in IntegrationTest := false,
    Keys.logBuffered := false,
    /**
     * Note: Adding qtiToV2 resources so they can be reused in the integration tests
     *
     */
    unmanagedResourceDirectories in IntegrationTest += baseDirectory.value / "modules/lib/qti-to-v2/src/test/resources",
    testOptions in IntegrationTest += Tests.Setup(() => println("Setup Integration Test")),
    testOptions in IntegrationTest += Tests.Cleanup(() => println("Cleanup Integration Test")),

    /**
     * Note: when running test-only for IT, the tests fail if the app isn't booted properly.
     * This is a workaround that *always* calls an empty Integration test first.
     * see: https://www.pivotaltracker.com/s/projects/880382/stories/65191542
     */
    testOnly in IntegrationTest := {
      (testOnly in IntegrationTest).partialInput(alwaysRunInTestOnly).evaluated
    })

  def safeSeed(clear: Boolean)(paths: String, name: String, logLevel: String, s: TaskStreams): Unit = {
    lazy val isRemoteSeedingAllowed = System.getProperty("allow.remote.seeding", "false") == "true"
    lazy val overrideClear = System.getProperty("clear.before.seeding", "false") == "true"
    s.log.info(s"[safeSeed] $paths - Allow remote seeding? $isRemoteSeedingAllowed - Clear collection before seed? $clear")
    val uri = getEnv("ENV_MONGO_URI").getOrElse("mongodb://localhost/api")
    val host = new URI(uri).getHost.toLowerCase
    if (host == "127.0.0.1" || host == "localhost" || isRemoteSeedingAllowed) {
      MongoDbSeederPlugin.seed(uri, paths, name, logLevel, clear || overrideClear)
      s.log.info(s"[safeSeed] $paths - seeding complete")
    } else {
      s.log.error(s"[safeSeed] $paths - Not allowed to seed a remote db. Add -Dallow.remote.seeding=true to override.")
    }
  }

  val devData = SettingKey[String]("dev-data")
  val demoData = SettingKey[String]("demo-data")
  val debugData = SettingKey[String]("debug-data")
  val sampleData = SettingKey[String]("sample-data")
  val staticData = SettingKey[String]("static-data")

  lazy val seederSettings = Seq(
    devData := Seq(
      "conf/seed-data/common",
      "conf/seed-data/dev",
      "conf/seed-data/exemplar-content").mkString(","),
    demoData := "conf/seed-data/demo",
    debugData := "conf/seed-data/debug",
    sampleData := "conf/seed-data/sample",
    staticData := "conf/seed-data/static")

  val seedDevData = TaskKey[Unit]("seed-dev-data")
  val seedDevDataTask = seedDevData <<= (devData, name, MongoDbSeederPlugin.logLevel, streams) map safeSeed(false)

  val seedDemoData = TaskKey[Unit]("seed-demo-data")
  val seedDemoDataTask = seedDemoData <<= (demoData, name, MongoDbSeederPlugin.logLevel, streams) map safeSeed(false)

  val seedDebugData = TaskKey[Unit]("seed-debug-data")
  val seedDebugDataTask = seedDebugData <<= (debugData, name, MongoDbSeederPlugin.logLevel, streams) map safeSeed(false)

  val seedSampleData = TaskKey[Unit]("seed-sample-data")
  val seedSampleDataTask = seedSampleData <<= (sampleData, name, MongoDbSeederPlugin.logLevel, streams) map safeSeed(false)

  val seedStaticData = TaskKey[Unit]("seed-static-data")
  val seedStaticDataTask = seedStaticData <<= (staticData, name, MongoDbSeederPlugin.logLevel, streams) map safeSeed(true)

  val seedDev = TaskKey[Unit]("seed-dev")
  val seedDevTask = seedDev := {
    (seedStaticData.value,
      seedDevData.value,
      seedDemoData.value,
      seedDebugData.value,
      seedSampleData.value)
  }

  val seedProd = TaskKey[Unit]("seed-prod")
  val seedProdTask = seedProd := {
    (seedStaticData.value,
      seedSampleData.value)
  }

  val main = builders.web(appName, Some(file(".")))
    .settings(sbt.Keys.fork in Test := false)
    .settings(
      routesImport ++= customImports,
      templatesImport ++= TemplateImports.Ids,
      libraryDependencies ++= Dependencies.all,
      templatesImport ++= Seq("org.bson.types.ObjectId", "org.corespring.platform.data.mongo.models.VersionedId"),
      resolvers ++= Dependencies.Resolvers.all,
      credentials += cred,
      Keys.fork.in(Test) := forkInTests,
      scalacOptions ++= Seq("-feature", "-deprecation"),
      (test in Test) <<= (test in Test).map(Commands.runJsTests))
    .settings(MongoDbSeederPlugin.newSettings ++ Seq(
      MongoDbSeederPlugin.logLevel := "INFO",
      testUri := "mongodb://localhost/api",
      testPaths := "conf/seed-data/test,conf/seed-data/static") ++ seederSettings: _*)
    .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
    .settings(disableDocsSettings: _*)
    .configs(IntegrationTest)
    .settings(Defaults.itSettings: _*)
    .settings(integrationTestSettings: _*)
    .settings(buildComponentsTask, (packagedArtifacts) <<= (packagedArtifacts) dependsOn buildComponents)
    .settings(seedDebugDataTask)
    .settings(seedDemoDataTask)
    .settings(seedDevDataTask)
    .settings(seedSampleDataTask)
    .settings(seedStaticDataTask)
    .settings(seedDevTask)
    .settings(seedProdTask)
    .dependsOn(scormWeb,
      reports,
      public,
      ltiWeb,
      v1Api,
      v1Player,
      playerLib,
      core % "it->test;compile->compile",
      apiUtils,
      commonViews,
      testLib % "test->compile;test->test;it->test",
      v2PlayerIntegration,
      v2Api,
      apiTracking,
      clientLogging % "compile->compile;test->test",
      qtiToV2,
      itemImport,
      itemDrafts)
    .aggregate(
      scormWeb,
      reports,
      public,
      ltiWeb,
      v1Api,
      v1Player,
      playerLib,
      core,
      apiUtils,
      commonViews,
      testLib,
      v2PlayerIntegration,
      v2Api,
      apiTracking,
      v2Auth,
      clientLogging,
      qtiToV2,
      itemImport,
      itemDrafts)
  addCommandAlias("gen-idea-project", ";update-classifiers;idea")
}
