import sbt._
object Dependencies {

  val playVersion = "2.2.6"

  //V2 Player
  val containerVersion = "5.6.2"

  val qtiVersion = "0.16"

  def toModule(name: String) = "org.corespring" %% name % containerVersion

  object ModuleConfigurations {

    import org.corespring.sbt.repo.RepoAuthPlugin

    val chainedSnapshots = ChainedResolver("chained", Seq(Resolver.defaultLocal, RepoAuthPlugin.snapshots))
    val snapshots = ModuleConfiguration("org.corespring", "*", "^.*?-SNAPSHOT$", chainedSnapshots)
    val releases = ModuleConfiguration("org.corespring", "*", "^0\\.\\d\\d$", RepoAuthPlugin.releases)
  }

  val containerClientWeb = toModule("container-client-web")
  val containerJsProcessing = toModule("js-processing")
  val componentModel = toModule("component-model")
  val componentLoader = toModule("component-loader")
  val mongoJsonService = toModule("mongo-json-service")

  val amapClient = "com.rabbitmq" % "amqp-client" % "3.0.2"
  val assetsLoader = ("com.ee" %% "assets-loader" % "0.12.5").exclude("com.yahoo.platform.yui", "yuicompressor")
  val aws = "com.amazonaws" % "aws-java-sdk" % "1.10.0"
  val casbah = "org.mongodb" %% "casbah" % "2.6.3"
  val closureCompiler = ("com.google.javascript" % "closure-compiler" % "rr2079.1" notTransitive ()).exclude("args4j", "args4j").exclude("com.google.guava", "guava").exclude("org.json", "json").exclude("com.google.protobuf", "protobuf-java").exclude("org.apache.ant", "ant").exclude("com.google.code.findbugs", "jsr305").exclude("com.googlecode.jarjar", "jarjar").exclude("junit", "junit")
  val commonsCodec = "commons-codec" % "commons-codec" % "1.10"
  val commonsIo = "commons-io" % "commons-io" % "2.4"
  val commonsLang = "org.apache.commons" % "commons-lang3" % "3.2.1"
  val corespringCommonUtils = "org.corespring" %% "corespring-common-utils" % "0.1-95301ae"
  val corespringMacros = "org.corespring" %% "macros" % "1.1.0"
  val elasticsearchPlayWS = ("org.corespring" %% "elasticsearch-play-ws" % "4.2.1-PLAY22").exclude("org.mongodb", "mongo-java-driver")
  val externalCommonUtils = "org.corespring" %% "corespring-common-utils" % "0.1-d6b09c5"
  val grizzledLog = "org.clapper" %% "grizzled-slf4j" % "1.0.2"
  val httpClient = "commons-httpclient" % "commons-httpclient" % "3.1"
  val jbcrypt = "org.mindrot" % "jbcrypt" % "0.3m"
  val jodaConvert = "org.joda" % "joda-convert" % "1.2"
  val jodaTime = "joda-time" % "joda-time" % "2.2"
  val jsonValidator = "com.github.fge" % "json-schema-validator" % "2.2.4"
  val jsoup = "org.jsoup" % "jsoup" % "1.8.1"
  val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.1.3"
  val macWireMacro = "com.softwaremill.macwire" %% "macros" % "0.7.3"
  val macWireRuntime = "com.softwaremill.macwire" %% "runtime" % "0.7.3"
  val mockito = "org.mockito" % "mockito-all" % "1.9.5" % "test"
  val mongoDbSeeder = "org.corespring" %% "mongo-db-seeder-lib" % "0.9-17eb3a8"
  val playCache = "com.typesafe.play" %% "play-cache" % playVersion //exclude("org.scala-stm", "scala-stm_2.10.0")
  val playFramework = "com.typesafe.play" %% "play" % playVersion
  val playJson = "com.typesafe.play" %% "play-json" % playVersion //exclude("org.scala-stm", "scala-stm_2.10.0")
  val playMemcached = "com.github.mumoshu" %% "play2-memcached" % "0.4.0"
  val playPluginMailer = "com.typesafe" %% "play-plugins-mailer" % "2.2.0"
  val playPluginUtil = "com.typesafe" %% "play-plugins-util" % "2.2.0"
  val playS3 = "org.corespring" %% "s3-play-plugin" % "1.2.1"
  val playTest = "com.typesafe.play" %% "play-test" % playVersion
  val qti = "org.corespring" %% "corespring-qti" % qtiVersion
  val qtiConverter = "org.corespring" %% "qti-corespring-converter" % qtiVersion
  val rhino = "org.mozilla" % "rhino" % "1.7R4"
  val rhinos = "org.corespring.forks.scalapeno" %% "rhinos" % "0.6.1"
  val salat = "com.novus" %% "salat" % "1.9.4"
  val salatPlay = "se.radley" %% "play-plugins-salat" % "1.4.0"
  val salatVersioningDao = "org.corespring" %% "salat-versioning-dao" % "0.21.0"
  val scalaFaker = "it.justwrote" %% "scala-faker" % "0.2"
  val scalaz = "org.scalaz" %% "scalaz-core" % "7.0.6"
  val scalazContrib = "org.typelevel" %% "scalaz-contrib-210" % "0.1.5"
  val securesocial = "org.corespring" %% "securesocial" % "master-22044d6"
  val sessionServiceClient = "org.corespring" %% "session-service-client" % "0.4"
  val simplecsv = "net.quux00.simplecsv" % "simplecsv" % "1.0"
  val slf4j = "org.slf4j" % "slf4j-api" % "1.7.5"
  val specs2 = "org.specs2" %% "specs2" % "2.2.2" // "3.6.2"
  val sprayCaching = "io.spray" %% "spray-caching" % "1.3.1"
  val ztZip = "org.zeroturnaround" % "zt-zip" % "1.8" % "it"

  object Resolvers {

    val corespringPublicSnapshots = "Corespring Public Artifactory Snapshots" at "http://repository.corespring.org/artifactory/public-ivy-snapshots"
    val edeustaceReleases = "ed eustace" at "http://edeustace.com/repository/releases/"
    val edeustaceSnapshots = "ed eustace snapshots" at "http://edeustace.com/repository/snapshots/"
    val ivyLocal = Resolver.file("ivyLocal", file(Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns)
    val justWrote = "justwrote" at "http://repo.justwrote.it/releases/"
    val sbtPluginReleases = Resolver.url("sbt-plugin-snapshots", new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots/"))(Resolver.ivyStylePatterns)
    val sbtPluginSnapshots = Resolver.url("sbt-plugin-releases", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)
    val scalazBintray = "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
    val sonatypeReleases = "Sonatype OSS" at "https://oss.sonatype.org/content/repositories/releases"
    val sonatypeSnapshots = "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    val spy = "Spy Repository" at "http://files.couchbase.com/maven2"
    val typesafe = "typesafe releases" at "http://repo.typesafe.com/typesafe/releases/"

    val all: Seq[Resolver] = Seq(
      scalazBintray,
      sonatypeSnapshots,
      typesafe,
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
