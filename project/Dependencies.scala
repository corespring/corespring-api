import sbt._

object Dependencies {

  val playVersion = "2.2.1"

  //V2 Player
  val containerVersion = "0.25.0-make-launcher-extendable-SNAPSHOT"
  val containerClientWeb = "org.corespring" %% "container-client-web" % containerVersion
  val containerJsProcessing = "org.corespring" %% "js-processing" % containerVersion
  val componentModel = "org.corespring" %% "component-model" % containerVersion
  val componentLoader = "org.corespring" %% "component-loader" % containerVersion
  val mongoJsonService = "org.corespring" %% "mongo-json-service" % containerVersion

  val amapClient = "com.rabbitmq" % "amqp-client" % "3.0.2"
  val assetsLoader = ("com.ee" %% "assets-loader" % "0.12.5")
    .exclude("com.yahoo.platform.yui", "yuicompressor")
  val aws = "com.amazonaws" % "aws-java-sdk" % "1.3.10"
  val casbah = "org.mongodb" %% "casbah" % "2.6.3"
  val closureCompiler = ("com.google.javascript" % "closure-compiler" % "rr2079.1" notTransitive ())
    .exclude("args4j", "args4j")
    .exclude("com.google.guava", "guava")
    .exclude("org.json", "json")
    .exclude("com.google.protobuf", "protobuf-java")
    .exclude("org.apache.ant", "ant")
    .exclude("com.google.code.findbugs", "jsr305")
    .exclude("com.googlecode.jarjar", "jarjar")
    .exclude("junit", "junit")
  val commonsIo = "commons-io" % "commons-io" % "2.4"
  val commonsLang = "org.apache.commons" % "commons-lang3" % "3.2.1"
  val corespringCommonUtils = "org.corespring" %% "corespring-common-utils" % "0.1-95301ae"
  val externalCommonUtils = "org.corespring" %% "corespring-common-utils" % "0.1-d6b09c5"
  val jbcrypt = "org.mindrot" % "jbcrypt" % "0.3m"
  val jodaTime = "joda-time" % "joda-time" % "2.2"
  val jodaConvert = "org.joda" % "joda-convert" % "1.2"
  val mockito = "org.mockito" % "mockito-all" % "1.9.5" % "test"
  val mongoDbSeeder = "org.corespring" %% "mongo-db-seeder-lib" % "0.9-17eb3a8"
  val playFramework = "com.typesafe.play" %% "play" % playVersion
  val playJson = "com.typesafe.play" %% "play-json" % playVersion //exclude("org.scala-stm", "scala-stm_2.10.0")
  val playMemcached = "com.github.mumoshu" %% "play2-memcached" % "0.4.0"
  val playPluginMailer = "com.typesafe" %% "play-plugins-mailer" % "2.2.0"
  val playPluginUtil = "com.typesafe" %% "play-plugins-util" % "2.2.0"
  val playS3 = "org.corespring" %% "s3-play-plugin" % "0.3"
  val playTest = "com.typesafe.play" %% "play-test" % playVersion
  val rhinos = "org.corespring.forks.scalapeno" %% "rhinos" % "0.6.1"
  val rhino = "org.mozilla" % "rhino" % "1.7R4"
  val salat = "com.novus" %% "salat" % "1.9.4"
  val salatPlay = "se.radley" %% "play-plugins-salat" % "1.4.0"
  val salatVersioningDao = "org.corespring" %% "salat-versioning-dao" % "0.17.0"
  val scalaFaker = "it.justwrote" %% "scala-faker" % "0.2"
  val scalaz = "org.scalaz" %% "scalaz-core" % "7.0.6"
  val securesocial = "org.corespring" %% "securesocial" % "master-22044d6"
  val slf4j = "org.slf4j" % "slf4j-api" % "1.7.5"
  val specs2 = "org.specs2" %% "specs2" % "2.1.1"
  val sprayCaching = "io.spray" %% "spray-caching" % "1.3.1"
  val simplecsv = "net.thornydev.simplecsv" % "simplecsv" % "1.0"
  val jsonValidator = "com.github.fge" % "json-schema-validator" % "2.2.4"
  //TODO: remove: see: https://thesib.atlassian.net/browse/CA-2210
  val newRelic = "com.newrelic.agent.java" % "newrelic-agent" % "3.10.0"

  val all = Seq(
    amapClient,
    assetsLoader,
    aws,
    closureCompiler,
    commonsIo,
    commonsLang,
    componentLoader,
    componentModel,
    containerClientWeb,
    jbcrypt,
    mockito,
    mongoDbSeeder,
    newRelic,
    play.Keys.cache,
    playMemcached,
    playPluginMailer,
    playPluginUtil,
    playS3,
    salatPlay,
    salatVersioningDao,
    scalaFaker,
    scalaz,
    securesocial,
    slf4j,
    playMemcached,
    playPluginMailer,
    playPluginUtil,
    salatPlay,
    scalaz,
    closureCompiler,
    containerClientWeb,
    componentLoader,
    componentModel,
    newRelic,
    slf4j,
    jsonValidator)

  object Resolvers {

    val corespringSnapshots = "Corespring Artifactory Snapshots" at "http://repository.corespring.org/artifactory/ivy-snapshots"
    val corespringReleases = "Corespring Artifactory Releases" at "http://repository.corespring.org/artifactory/ivy-releases"
    val corespringPublicSnapshots = "Corespring Public Artifactory Snapshots" at "http://repository.corespring.org/artifactory/public-ivy-snapshots"
    val typesafe = "typesafe releases" at "http://repo.typesafe.com/typesafe/releases/"
    val spy = "Spy Repository" at "http://files.couchbase.com/maven2"
    val sonatypeSnapshots = "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    val sonatypeReleases = "Sonatype OSS" at "https://oss.sonatype.org/content/repositories/releases"
    val sbtPluginSnapshots = Resolver.url("sbt-plugin-releases", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)
    val sbtPluginReleases = Resolver.url("sbt-plugin-snapshots", new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots/"))(Resolver.ivyStylePatterns)
    val edeustaceReleases = "ed eustace" at "http://edeustace.com/repository/releases/"
    val edeustaceSnapshots = "ed eustace snapshots" at "http://edeustace.com/repository/snapshots/"
    val justWrote = "justwrote" at "http://repo.justwrote.it/releases/"
    val ivyLocal = Resolver.file("ivyLocal", file(Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns)

    val all: Seq[Resolver] = Seq(
      sonatypeSnapshots,
      typesafe,
      corespringSnapshots,
      corespringReleases,
      corespringPublicSnapshots,
      spy,
      sbtPluginSnapshots,
      sbtPluginReleases,
      edeustaceReleases,
      edeustaceSnapshots,
      justWrote,
      ivyLocal)
  }

}
