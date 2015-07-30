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
    "org.corespring.web.pathbind.VersionedIdPathBind._")

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

  val coreModels = builders.lib("models", "core").settings(
    libraryDependencies ++= Seq(casbah, salatVersioningDao, playJson, commonsLang, specs2 % "test"))

  val coreJson = builders.lib("json", "core").dependsOn(coreModels)
    .settings(libraryDependencies ++= Seq(specs2 % "test"))

  val coreServices = builders.lib("services", "core").dependsOn(coreModels)

  val coreUtils = builders.lib("utils", "core")

  val coreWeb = builders.lib("web", "core")
    .settings(libraryDependencies ++= Seq(securesocial, playFramework))
    .dependsOn(coreModels, coreServices)

  val coreServicesSalat = builders.lib("services-salat", "core")
    .settings(
      libraryDependencies ++= Seq(grizzledLog, logbackClassic, aws))
    .configs(IntegrationTest)
    .settings(Defaults.itSettings: _*)
    .settings(
      Keys.parallelExecution in IntegrationTest := false,
      Keys.fork in IntegrationTest := false,
      Keys.logBuffered := false,
      testOptions in IntegrationTest += Tests.Setup(() => println("---------> Setup Integration Test")),
      testOptions in IntegrationTest += Tests.Cleanup(() => println("-----------> Cleanup Integration Test")),
      testOptions in IntegrationTest += Tests.Setup((loader: java.lang.ClassLoader) => {
        loader.loadClass("org.corespring.it.mongo.Setup").newInstance
      }),
      testOptions in IntegrationTest += Tests.Cleanup((loader: java.lang.ClassLoader) => {
        loader.loadClass("org.corespring.it.mongo.Cleanup").newInstance
      }))
    .settings(libraryDependencies ++= Seq(macWireMacro, macWireRuntime, specs2 % "it,test", specs2Mock % "it,test", aws))
    .dependsOn(coreServices, coreUtils)

  val encryption = builders.lib("encryption", "core")
    .settings(libraryDependencies ++= Seq(casbah, commonsCodec, macWireMacro))
    .dependsOn(coreServices, coreModels)

  val coreLeftovers = builders.lib("leftovers", "core")

  /**
   * Core data model
   * val core = builders.lib("core")
   * .settings(
   * libraryDependencies ++= Seq(
   * assetsLoader,
   * componentLoader,
   * corespringCommonUtils,
   * elasticsearchPlayWS,
   * httpClient,
   * jsoup,
   * mockito,
   * playFramework,
   * playS3,
   * playTest % "test",
   * salatPlay,
   * salatVersioningDao,
   * scalaFaker,
   * securesocial,
   * specs2 % "test",
   * sprayCaching))
   * .dependsOn(assets, testLib % "test->compile", qti, playJsonSalatUtils)
   */

  val itemSearch = builders.lib("item-search")
    .settings(
      libraryDependencies ++= Seq(salatVersioningDao, playJson, elasticsearchPlayWS, commonsCodec, grizzledLog, macWireMacro))
    .dependsOn(coreModels, coreJson)

  val commonViews = builders.web("common-views")
    .settings(
      BuildInfo.buildInfoTask,
      (packagedArtifacts) <<= (packagedArtifacts) dependsOn BuildInfo.buildInfo,
      libraryDependencies ++= Seq(playJson % "test", assetsLoader, aws))
    .dependsOn(assets, itemSearch)

  val drafts = builders.lib("drafts")
    .settings(
      libraryDependencies ++= Seq(specs2 % "test", jodaTime, jodaConvert, scalaz))

  val itemDrafts = builders.lib("item-drafts")
    .settings(
      libraryDependencies ++= Seq(containerClientWeb, specs2 % "test", salatVersioningDao, macWireMacro))
    .dependsOn(coreModels, coreServices, drafts, testLib)
    .aggregate(coreModels, drafts)

  /** Qti -> v2 transformers */
  val qtiToV2 = builders.lib("qti-to-v2")
    .settings(
      libraryDependencies ++= Seq(playJson, rhino % "test"))
    .dependsOn(coreModels, coreServices, coreUtils, coreJson, qti, apiUtils, testLib % "test->compile")

  /*val v1Api = builders.web("v1-api")
    .settings(
      libraryDependencies ++= Seq(casbah),
      templatesImport ++= TemplateImports.Ids,
      routesImport ++= customImports)
    .dependsOn(core % "compile->compile;test->test", playerLib, scormLib, ltiLib, qtiToV2)*/

  /**
   * Error types
   */
  val v2Errors = builders.lib("v2-errors")
    .settings(
      libraryDependencies ++= Seq(scalaz, playTest, casbah, salatVersioningDao))
    .dependsOn(coreModels)

  val v2SessionDb = builders.lib("v2-session-db")
    .settings(
      libraryDependencies ++= Seq(specs2 % "test", mockito, mongoJsonService, scalaz))
    .dependsOn(testLib, v2Errors, qtiToV2, itemDrafts)

  /**
   * All authentication code for v2 api + player/editor
   */
  val v2Auth = builders.lib("v2-auth")
    .settings(
      libraryDependencies ++= Seq(specs2 % "test", mockito, mongoJsonService, scalaz, sprayCaching, grizzledLog))
    .dependsOn(coreModels, coreServices, coreWeb, coreJson, testLib, v2Errors, qtiToV2, itemDrafts, v2SessionDb, encryption)

  val apiTracking = builders.lib("api-tracking")
    .settings(
      libraryDependencies ++= Seq(playFramework)).dependsOn(v2Auth)
    .dependsOn(coreServices, v2Errors, testLib % "test->compile")

  val itemImport = builders.web("item-import")
    .settings(libraryDependencies ++= Seq(playJson, jsonValidator, salatVersioningDao, mockito))
    .dependsOn(coreJson, coreServices, v2Auth, testLib % "test->compile")

  val draftsApi = builders.web("v2-api-drafts")
    .dependsOn(coreJson, itemDrafts, testLib % "test->test")

  val v2Api = builders.web("v2-api")
    .settings(
      libraryDependencies ++= Seq(
        scalaz,
        mongoJsonService,
        salatVersioningDao,
        componentModel,
        macWireMacro),
      routesImport ++= customImports)
    .dependsOn(
      v2Auth % "test->test;compile->compile",
      v2SessionDb % "test->test;compile->compile",
      coreModels,
      coreServices,
      encryption,
      itemSearch,
      coreJson,
      qtiToV2,
      draftsApi)

  object TemplateImports {
    val Ids = Seq("org.bson.types.ObjectId", "org.corespring.platform.data.mongo.models.VersionedId")
  }

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
        httpClient,
        macWireMacro))
    .dependsOn(
      qtiToV2,
      testLib,
      v2Auth % "test->test;compile->compile",
      coreModels,
      coreServices,
      itemDrafts)
    .dependsOn(v2Api)

  /*val reports = builders.web("reports")
    .settings(
      libraryDependencies ++= Seq(simplecsv, casbah, playCache))
    .dependsOn(coreModels, coreServices, commonViews)*/

  val main = builders.web(appName, Some(file(".")))
    .settings(sbt.Keys.fork in Test := false)
    .settings(NewRelic.settings: _*)
    .settings(
      libraryDependencies ++= Seq(playMemcached, assetsLoader),
      (javacOptions in Compile) ++= Seq("-source", "1.7", "-target", "1.7"),
      routesImport ++= customImports,
      templatesImport ++= TemplateImports.Ids,
      moduleConfigurations ++= Seq( /*Dependencies.ModuleConfigurations.snapshots, */ Dependencies.ModuleConfigurations.releases, Dependencies.ModuleConfigurations.localSnapshots),
      //updateOptions := updateOptions.value.withCachedResolution(true),
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
    .dependsOn(
      coreModels,
      coreServices,
      coreServicesSalat,
      coreWeb,
      coreJson,
      apiUtils,
      commonViews,
      testLib % "test->compile;test->test;it->test",
      v2PlayerIntegration,
      v2Api,
      v2SessionDb,
      apiTracking,
      qtiToV2,
      itemImport,
      itemDrafts % "compile->compile;test->test;it->test")
    .aggregate(
      coreModels,
      coreServices,
      coreServicesSalat,
      coreWeb,
      coreJson,
      apiUtils,
      commonViews,
      testLib,
      v2PlayerIntegration,
      v2Api,
      apiTracking,
      v2Auth,
      v2SessionDb,
      qtiToV2,
      itemImport,
      itemDrafts)

  addCommandAlias("gen-idea-project", ";update-classifiers;idea")
}
