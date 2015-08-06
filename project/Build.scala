import sbt.Keys._
import sbt._
import play.Project._

object Build extends sbt.Build {

  import Dependencies._
  import ComponentsBuilder._

  val appName = "corespring"
  val appVersion = "1.0"
  val ScalaVersion = "2.10.5"
  val org = "org.corespring"

  val builders = new Builders(appName, org, appVersion, ScalaVersion)

  val customImports = Seq(
    "scala.language.reflectiveCalls",
    "se.radley.plugin.salat.Binders._",
    "org.corespring.platform.data.mongo.models.VersionedId",
    "org.bson.types.ObjectId",
    "org.corespring.platform.core.models.versioning.VersionedIdImplicits.Binders._")

  val playJsonSalatUtils = builders.lib("play-json-salat-utils")
    .settings(
      libraryDependencies ++= Seq(playJson, salat, specs2 % "test"))

  val apiUtils = builders.lib("api-utils")
    .settings(
      libraryDependencies ++= Seq(aws, specs2 % "test", playFramework, salatPlay, playJson % "test"),
      Keys.fork in Test := builders.forkInTests)

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
  val core = builders.lib("core")
    .settings(
      libraryDependencies ++= Seq(
        assetsLoader,
        componentLoader,
        corespringCommonUtils,
        elasticsearchPlayWS,
        httpClient,
        jsoup,
        mockito,
        playFramework,
        playS3,
        playTest % "test",
        salatPlay,
        salatVersioningDao,
        scalaFaker,
        securesocial,
        specs2 % "test",
        sprayCaching))
    .dependsOn(assets, testLib % "test->compile", qti, playJsonSalatUtils)

  val playerLib = builders.lib("player-lib")
    .settings(
      libraryDependencies ++= Seq(corespringCommonUtils, playFramework, specs2, playTest % "test", scalaFaker % "test"))
    .dependsOn(core)

  val commonViews = builders.web("common-views")
    .settings(
      BuildInfo.buildInfoTask,
      (packagedArtifacts) <<= (packagedArtifacts) dependsOn BuildInfo.buildInfo,
      libraryDependencies ++= Seq(playJson % "test"))
    .dependsOn(core % "compile->compile;test->test")

  val clientLogging = builders.web("client-logging")
    .settings(
      libraryDependencies ++= Seq(playFramework, scalaz))
    .dependsOn(apiUtils, core % "test->test")

  val scormLib = builders.lib("scorm").settings(
    libraryDependencies ++= Seq(playFramework))
    .dependsOn(core)

  val ltiLib = builders.lib("lti")
    .dependsOn(apiUtils, core % "compile->compile;test->compile;test->test")

  val drafts = builders.lib("drafts")
    .settings(
      libraryDependencies ++= Seq(specs2 % "test", jodaTime, jodaConvert, scalaz))

  val itemDrafts = builders.lib("item-drafts")
    .settings(
      libraryDependencies ++= Seq(containerClientWeb, specs2 % "test"))
    .dependsOn(core, drafts, testLib)
    .aggregate(core, drafts)

  /** Qti -> v2 transformers */
  val qtiToV2 = builders.lib("qti-to-v2")
    .settings(
      libraryDependencies ++= Seq(playJson, rhino % "test"))
    .dependsOn(core, qti, apiUtils, testLib % "test->compile")

  val v1Api = builders.web("v1-api")
    .settings(
      libraryDependencies ++= Seq(casbah),
      templatesImport ++= TemplateImports.Ids,
      routesImport ++= customImports)
    .dependsOn(core % "compile->compile;test->test", playerLib, scormLib, ltiLib, qtiToV2)

  /**
   * Error types
   */
  val v2Errors = builders.lib("v2-errors")
    .settings(
      libraryDependencies ++= Seq(scalaz, playTest))
    .dependsOn(core)

  val v2SessionDb = builders.lib("v2-session-db")
    .settings(
      libraryDependencies ++= Seq(specs2 % "test", mockito, mongoJsonService, scalaz))
    .dependsOn(testLib, v2Errors, core, playerLib, qtiToV2, itemDrafts)

  /**
   * All authentication code for v2 api + player/editor
   */
  val v2Auth = builders.lib("v2-auth")
    .settings(
      libraryDependencies ++= Seq(specs2 % "test", mockito, mongoJsonService, scalaz))
    .dependsOn(testLib, v2Errors, core, playerLib, qtiToV2, itemDrafts, v2SessionDb)

  val apiTracking = builders.lib("api-tracking")
    .settings(
      libraryDependencies ++= Seq(playFramework)).dependsOn(v2Auth)
    .dependsOn(v2Errors, core, playerLib, testLib % "test->compile")

  val itemImport = builders.web("item-import")
    .settings(libraryDependencies ++= Seq(playJson, jsonValidator, salatVersioningDao, mockito))
    .dependsOn(v2Auth, testLib % "test->compile", core % "test->compile;test->test", core)

  val draftsApi = builders.web("v2-api-drafts")
    .dependsOn(itemDrafts, testLib % "test->test")

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
      v2SessionDb % "test->test;compile->compile",
      qtiToV2,
      v1Api,
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

  val devTools = builders.web("dev-tools")
    .settings(
      routesImport ++= customImports,
      libraryDependencies ++= Seq(containerClientWeb, mongoJsonService))
    .dependsOn(v1Player, playerLib, core, v2Auth)

  /** Implementation of corespring container hooks */
  val v2PlayerIntegration = builders.lib("v2-player-integration")
    .settings(
      libraryDependencies ++= Seq(
        containerClientWeb,
        componentLoader,
        componentModel,
        scalaz,
        mongoJsonService,
        playS3,
        httpClient))
    .dependsOn(
      qtiToV2,
      testLib,
      v2Auth % "test->test;compile->compile",
      core % "test->test;compile->compile",
      playerLib,
      devTools,
      itemDrafts)
    .dependsOn(v2Api)

  val ltiWeb = builders.web("lti-web")
    .settings(
      templatesImport ++= TemplateImports.Ids,
      routesImport ++= customImports)
    .aggregate(core, ltiLib, playerLib, v1Player)
    .dependsOn(ltiLib, playerLib, v1Player, testLib % "test->compile", core % "test->compile;test->test")

  val public = builders.web("public")
    .settings(
      libraryDependencies ++= Seq(playFramework, securesocial),
      routesImport ++= customImports)
    .dependsOn(commonViews, core % "compile->compile;test->test", playerLib, v1Player, testLib % "test->compile")
    .aggregate(commonViews)

  val reports = builders.web("reports")
    .settings(
      libraryDependencies ++= Seq(simplecsv))
    .dependsOn(commonViews, core % "compile->compile;test->test")

  val scormWeb = builders.web("scorm-web")
    .settings(
      routesImport ++= customImports)
    .dependsOn(core, scormLib, v1Player)

  val main = builders.web(appName, Some(file(".")))
    .settings(sbt.Keys.fork in Test := false)
    .settings(NewRelic.settings: _*)
    .settings(
      libraryDependencies ++= Seq(playMemcached),
      (javacOptions in Compile) ++= Seq("-source", "1.7", "-target", "1.7"),
      routesImport ++= customImports,
      templatesImport ++= TemplateImports.Ids,
      moduleConfigurations ++= Seq(Dependencies.ModuleConfigurations.snapshots, Dependencies.ModuleConfigurations.releases),
      //updateOptions := updateOptions.value.withConsolidatedResolution(true),
      templatesImport ++= Seq("org.bson.types.ObjectId", "org.corespring.platform.data.mongo.models.VersionedId"),
      resolvers ++= Dependencies.Resolvers.all,
      credentials += LoadCredentials.cred,
      Keys.fork.in(Test) := builders.forkInTests,
      scalacOptions ++= Seq("-feature", "-deprecation"),
      (test in Test) <<= (test in Test).map(Commands.runJsTests))
    .settings(Seeding.settings: _*)
    .configs(IntegrationTest)
    .settings(IntegrationTestSettings.settings: _*)
    .settings(buildComponentsTask, (packagedArtifacts) <<= (packagedArtifacts) dependsOn buildComponents)
    .settings(Indexing.indexTask)
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
      v2SessionDb,
      apiTracking,
      clientLogging % "compile->compile;test->test",
      qtiToV2,
      itemImport,
      itemDrafts % "compile->compile;test->test;it->test")
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
      v2SessionDb,
      clientLogging,
      qtiToV2,
      itemImport,
      itemDrafts)

  addCommandAlias("gen-idea-project", ";update-classifiers;idea")
}