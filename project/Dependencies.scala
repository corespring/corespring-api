import sbt.Keys._
import sbt._

object Dependencies {

  //TODO: May need to change this
  val playJson = "com.typesafe.play" %% "play-json" % "2.2.0-M1"

  val playFramework = "play" %% "play" % "2.1.3-RC1"

  val aws = "com.amazonaws" % "aws-java-sdk" % "1.3.10"
  val salatPlay =  "se.radley" %% "play-plugins-salat" % "1.2"
  val casbah = "com.mongodb.casbah" %% "casbah" % "2.0"
  val playPluginUtil = "com.typesafe" %% "play-plugins-util" % "2.1.0"
  val salatVersioningDao = "org.corespring" %% "salat-versioning-dao" % "0.2-d2150fc"
  val playS3 = "org.corespring" %% "play-s3" % "0.1-3d07c18"
  val playPluginMailer = "com.typesafe" %% "play-plugins-mailer" % "2.1.0"
  val jbcrypt = "org.mindrot" % "jbcrypt" % "0.3m"
  val securesocial = "securesocial" %% "securesocial" % "master-SNAPSHOT"
  val playMemcached = "com.github.mumoshu" %% "play2-memcached" % "0.3.0.3"
  val mockito = "org.mockito" % "mockito-all" % "1.9.5" % "test"
  val amapClient = "com.rabbitmq" % "amqp-client" % "3.0.2"
  val scalaz = "org.scalaz" %% "scalaz-core" % "7.0.2"
  val assetsLoader = "com.ee" %% "assets-loader" % "0.10.1-8ab100a"
  val specs2 = "org.specs2" %% "specs2" % "2.1.1" % "test"
  val closureCompiler = ("com.google.javascript" % "closure-compiler" % "rr2079.1" notTransitive())
    .exclude("args4j", "args4j")
    .exclude("com.google.guava", "guava")
    .exclude("org.json", "json")
    .exclude("com.google.protobuf", "protobuf-java")
    .exclude("org.apache.ant", "ant")
    .exclude("com.google.code.findbugs", "jsr305")
    .exclude("com.googlecode.jarjar", "jarjar")
    .exclude("junit", "junit")
  val all = Seq(
    amapClient,
    assetsLoader,
    aws,
    jbcrypt,
    mockito,
    playMemcached,
    playPluginMailer,
    playPluginUtil,
    salatPlay,
    securesocial,
    scalaz,
    closureCompiler)

  object Resolvers {



    //TODO: Remove this - once we have an internal artifactory repo setup

    val localIvy = Resolver.file("Local Ivy Repository", file(Path.userHome.absolutePath+"/.ivy2/local")) (Resolver.ivyStylePatterns)

    val typesafe = "typesafe releases" at "http://repo.typesafe.com/typesafe/releases/"
    val edeustaceReleases= "ed eustace" at "http://edeustace.com/repository/releases/"
    val edeustaceSnapshots = "ed eustace snapshots" at "http://edeustace.com/repository/snapshots/"
    val spy = "Spy Repository" at "http://files.couchbase.com/maven2"
    val sonatypeSnapshots= "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    val sonatypeReleases = "Sonatype OSS" at "https://oss.sonatype.org/content/repositories/releases"
    val sbtPluginSnapshots = Resolver.url("sbt-plugin-releases", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)
    val sbtPluginReleases =  Resolver.url("sbt-plugin-snapshots", new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots/"))(Resolver.ivyStylePatterns)
    val all: Seq[Resolver] = Seq(
      sonatypeSnapshots,
      typesafe,
      localIvy,
      spy,
      edeustaceSnapshots,
      edeustaceReleases,
      sbtPluginSnapshots,
      sbtPluginReleases)
  }

}
