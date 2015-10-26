import sbt.Keys._
import sbt._
import play.Project._

object Build extends sbt.Build {

  import Dependencies._
  import ComponentsBuilder._

  lazy val appName = "corespring"
  lazy val appVersion = "1.0"
  lazy val ScalaVersion = "2.10.5"
  lazy val org = "org.corespring"

  lazy val builders = new Builders(appName, org, appVersion, ScalaVersion)

  lazy val customImports = Seq(
    "scala.language.reflectiveCalls",
    "se.radley.plugin.salat.Binders._",
    "org.corespring.platform.data.mongo.models.VersionedId",
    "org.bson.types.ObjectId",
    "org.corespring.web.pathbind.VersionedIdPathBind._")

  lazy val playJsonSalatUtils = builders.lib("play-json-salat-utils")
    .settings(
      libraryDependencies ++= Seq(playJson, salat, specs2 % "test"))

  lazy val apiUtils = builders.lib("api-utils")
    .settings(
      libraryDependencies ++= Seq(aws, specs2 % "test", playFramework, salatPlay, playJson % "test"),
      Keys.fork in Test := builders.forkInTests)

  /** Any shared test helpers in here */
  lazy val testLib = builders.testLib("test-helpers")
    .settings(libraryDependencies ++= Seq(specs2 % "test->compile", playFramework, playTest, salatPlay))
    .dependsOn(apiUtils)

  lazy val assets = builders.lib("assets")
    .settings(libraryDependencies ++= Seq(specs2 % "test", playS3, playFramework, assetsLoader, corespringCommonUtils))
    .dependsOn(apiUtils)

  lazy val coreModels = builders.lib("models", "core").settings(
    libraryDependencies ++= Seq(casbah, salatVersioningDao, playJson, commonsLang, specs2 % "test"))

  lazy val coreJson = builders.lib("json", "core").dependsOn(coreModels)
    .settings(libraryDependencies ++= Seq(specs2 % "test"))

  lazy val coreServices = builders.lib("services", "core")
    .settings(
      libraryDependencies ++= Seq(specs2 % "test"))
    .dependsOn(coreModels)

  lazy val coreUtils = builders.lib("utils", "core")

  lazy val coreLegacy = builders.lib("legacy", "core")
    .settings(libraryDependencies ++= Seq(securesocial, playFramework, specs2 % "test", playS3))
    .dependsOn(coreServices, coreModels, coreJson, qtiToV2)

  lazy val coreWeb = builders.lib("web", "core")
    .settings(libraryDependencies ++= Seq(securesocial, playFramework))
    .dependsOn(coreModels, coreServices)

  lazy val coreSalatConfig = builders.lib("salat-config", "core").settings(
    libraryDependencies ++= Seq(salat))

  lazy val coreServicesSalat = builders.lib("services-salat", "core")
    .settings(
      libraryDependencies ++= Seq(salat, salatVersioningDao, grizzledLog, logbackClassic, aws))
    .configs(IntegrationTest)
    .settings(Defaults.itSettings: _*)
    .settings(
      Keys.parallelExecution in IntegrationTest := false,
      Keys.fork in IntegrationTest := false,
      Keys.logBuffered := false,
      testOptions in IntegrationTest += Tests.Setup((loader: java.lang.ClassLoader) => {
        loader.loadClass("org.corespring.services.salat.it.Setup").newInstance
      }),
      testOptions in IntegrationTest += Tests.Cleanup((loader: java.lang.ClassLoader) => {
        loader.loadClass("org.corespring.services.salat.it.Cleanup").newInstance
      }),
      testOptions in IntegrationTest += Tests.Setup(() => println("---------> Setup Integration Test")),
      testOptions in IntegrationTest += Tests.Cleanup(() => println("-----------> Cleanup Integration Test")))
    .settings(libraryDependencies ++= Seq(macWireMacro, macWireRuntime, specs2 % "it,test", aws))
    .dependsOn(coreSalatConfig, coreServices, coreUtils)

  lazy val encryption = builders.lib("encryption", "core")
    .settings(libraryDependencies ++= Seq(casbah, commonsCodec, macWireMacro, jbcrypt, specs2 % "test"))
    .dependsOn(coreServices, coreModels)

  /**
   * Core data model
   * lazy val core = builders.lib("core")
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

  lazy val itemSearch = builders.lib("item-search")
    .settings(
      libraryDependencies ++= Seq(
        salatVersioningDao,
        playJson,
        playFramework,
        elasticsearchPlayWS,
        jsoup,
        commonsCodec,
        grizzledLog,
        macWireMacro))
    .dependsOn(coreModels, coreJson)

  lazy val commonViews = builders.web("common-views")
    .settings(
      BuildInfo.buildInfoTask,
      (packagedArtifacts) <<= (packagedArtifacts) dependsOn BuildInfo.buildInfo,
      libraryDependencies ++= Seq(playJson % "test", assetsLoader, aws))
    .dependsOn(assets, itemSearch)

  lazy val drafts = builders.lib("drafts")
    .settings(
      libraryDependencies ++= Seq(specs2 % "test", jodaTime, jodaConvert, scalaz))

  lazy val itemDrafts = builders.lib("item-drafts")
    .settings(
      libraryDependencies ++= Seq(containerClientWeb, specs2 % "test", salatVersioningDao, macWireMacro))
    .dependsOn(coreSalatConfig % "compile->test", coreModels, coreServices, drafts, testLib)
    .aggregate(coreModels, drafts)

  lazy val qtiToV2 = builders.lib("qti-to-v2")
    .settings(
      libraryDependencies ++= Seq(playJson, rhino % "test", qti, qtiConverter))
    .dependsOn(coreModels, coreServices, coreUtils, coreJson, apiUtils, testLib % "test->compile")

  /**
   * Error types
   */
  lazy val v2Errors = builders.lib("v2-errors")
    .settings(
      libraryDependencies ++= Seq(scalaz, playTest, casbah, salatVersioningDao))
    .dependsOn(coreModels)

  lazy val v2SessionDb = builders.lib("v2-session-db")
    .settings(
      libraryDependencies ++= Seq(specs2 % "test", mockito, mongoJsonService, scalaz))
    .dependsOn(testLib, v2Errors, qtiToV2, itemDrafts)

  /**
   * All authentication code for v2 api + player/editor
   */
  lazy val v2Auth = builders.lib("v2-auth")
    .settings(
      libraryDependencies ++= Seq(specs2 % "test", mockito, mongoJsonService, scalaz, sprayCaching, grizzledLog))
    .dependsOn(coreModels, coreServices, coreWeb, coreJson, testLib, v2Errors, qtiToV2, itemDrafts, v2SessionDb, encryption)

  lazy val apiTracking = builders.lib("api-tracking")
    .settings(
      libraryDependencies ++= Seq(playFramework)).dependsOn(v2Auth)
    .dependsOn(coreServices, v2Errors, testLib % "test->compile")

  lazy val itemImport = builders.web("item-import")
    .settings(libraryDependencies ++= Seq(playJson, jsonValidator, salatVersioningDao, mockito))
    .dependsOn(coreJson, coreServices, v2Auth, testLib % "test->compile")

  lazy val draftsApi = builders.web("v2-api-drafts")
    .dependsOn(coreJson, itemDrafts, testLib % "test->test")

  lazy val v2Api = builders.web("v2-api")
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

  lazy val v1Api = builders.web("v1-api")
    .settings(
      libraryDependencies ++= Seq(casbah, playS3),
      templatesImport ++= TemplateImports.Ids,
      routesImport ++= customImports)
    .dependsOn(v2Api, coreWeb, coreModels, coreServices, coreJson, coreLegacy, qtiToV2, assets, v2SessionDb)

  object TemplateImports {
    lazy val Ids = Seq("org.bson.types.ObjectId", "org.corespring.platform.data.mongo.models.VersionedId")
  }

  /** Implementation of corespring container hooks */
  lazy val v2PlayerIntegration = builders.lib("v2-player-integration")
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
      coreServicesSalat,
      itemDrafts)
    .dependsOn(v2Api)

  /*lazy val reports = builders.web("reports")
    .settings(
      libraryDependencies ++= Seq(simplecsv, casbah, playCache))
    .dependsOn(coreModels, coreServices, commonViews)*/

  lazy val main = builders.web(appName, Some(file(".")))
    .settings(sbt.Keys.fork in Test := false)
    .settings(NewRelic.settings: _*)
    .settings(
      //disable publishing of the root project
      packagedArtifacts := Map.empty,
      libraryDependencies ++= Seq(playMemcached, assetsLoader),
      (javacOptions in Compile) ++= Seq("-source", "1.7", "-target", "1.7"),
      routesImport ++= customImports,
      templatesImport ++= TemplateImports.Ids,
      moduleConfigurations ++= Builders.moduleConfig,

      /**
       * Warning: Don't enable this for now:
       * //updateOptions := updateOptions.value.withCachedResolution(true),
       * It's causing an unusual rhino error.
       * See: https://bitbucket.org/corespring/sbt-rhino-issue
       * It's a project where I'm isolating the issue so I can raise it with sbt team.
       */
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
      apiUtils,
      coreModels,
      coreServices,
      coreServicesSalat,
      coreSalatConfig,
      coreWeb,
      coreJson,
      apiUtils,
      commonViews,
      testLib % "test->compile;test->test;it->test",
      v2PlayerIntegration,
      v1Api,
      v2Api,
      v2SessionDb,
      apiTracking,
      qtiToV2,
      itemImport,
      itemDrafts % "compile->compile;test->test;it->test",
      v2SessionDb)
    .aggregate(
      apiUtils,
      coreModels,
      coreServices,
      coreServicesSalat,
      coreWeb,
      coreJson,
      apiUtils,
      commonViews,
      testLib,
      v2PlayerIntegration,
      v1Api,
      v2Api,
      apiTracking,
      v2Auth,
      v2SessionDb,
      qtiToV2,
      itemImport,
      itemDrafts)

  addCommandAlias("gen-idea-project", ";update-classifiers;idea")
}