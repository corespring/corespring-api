import sbt._

object Dependencies {

  val playVersion = "2.1.3"

  //TODO: May need to change this
  val playJson = "com.typesafe.play" %% "play-json" % "2.2.0-RC2"

  val playFramework = "play" %% "play" % playVersion
  val playTest = "play" %% "play-test" % playVersion

  val corespringCommonUtils = "org.corespring" %% "corespring-common-utils" % "0.1-95301ae"
  val corespringQti = "org.corespring" %% "corespring-qti" % "0.1-5aecaa8"
  val mongoDbSeeder = "com.ee" %% "mongo-db-seeder-lib" % "0.5-3f2edcd"
  val aws = "com.amazonaws" % "aws-java-sdk" % "1.3.10"
  val salatPlay = "se.radley" %% "play-plugins-salat" % "1.3.0"
  val casbah = "com.mongodb.casbah" %% "casbah" % "2.0"
  val playPluginUtil = "com.typesafe" %% "play-plugins-util" % "2.1.0"
  val salatVersioningDao = "org.corespring" %% "salat-versioning-dao" % "0.2-b185ac1"
  val playS3 = "org.corespring" %% "play-s3" % "0.1-46515f2"
  val playPluginMailer = "com.typesafe" %% "play-plugins-mailer" % "2.1.0"
  val jbcrypt = "org.mindrot" % "jbcrypt" % "0.3m"
  val securesocial = "org.corespring" %% "securesocial" % "master-c376674"
  val playMemcached = "com.github.mumoshu" %% "play2-memcached" % "0.3.0.3"
  val mockito = "org.mockito" % "mockito-all" % "1.9.5" % "test"
  val amapClient = "com.rabbitmq" % "amqp-client" % "3.0.2"
  val scalaz = "org.scalaz" %% "scalaz-core" % "7.0.2"

  // TODO: temporary solution: taking out yui which is used for css minification as it breaks
  // dependencies for corespring-qti. This needs to be sorted out properly!

  val assetsLoader = ("com.ee" %% "assets-loader" % "0.10.1-8ab100a")
    .exclude("com.yahoo.platform.yui", "yuicompressor")

  val specs2 = "org.specs2" %% "specs2" % "2.1.1"
  val slf4j = "org.slf4j" % "slf4j-api" % "1.7.5"
  val scalaFaker = "it.justwrote" %% "scala-faker" % "0.3-SNAPSHOT"

  val closureCompiler = ("com.google.javascript" % "closure-compiler" % "rr2079.1" notTransitive ())
    .exclude("args4j", "args4j")
    .exclude("com.google.guava", "guava")
    .exclude("org.json", "json")
    .exclude("com.google.protobuf", "protobuf-java")
    .exclude("org.apache.ant", "ant")
    .exclude("com.google.code.findbugs", "jsr305")
    .exclude("com.googlecode.jarjar", "jarjar")
    .exclude("junit", "junit")
  val all = Seq(
    playS3,
    slf4j,
    salatVersioningDao,
    amapClient,
    assetsLoader,
    aws,
    mongoDbSeeder,
    jbcrypt,
    mockito,
    scalaFaker,
    playMemcached,
    playPluginMailer,
    playPluginUtil,
    salatPlay,
    securesocial,
    scalaz,
    closureCompiler)

  object Resolvers {

    //val localIvy = Resolver.file("local ivy", file(Path.userHome.absolutePath+"/.ivy2/local/"))//(Resolver.ivyStylePatterns)
    //val localIvy = "Local Ivy Repository" at file(Path.userHome.absolutePath+"/.ivy2/local/") (Resolver.ivyStylePatterns)
    val localIvy = Resolver.file("Local Ivy Repository", file(Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns)

    val corespringSnapshots = "Corespring Artifactory Snapshots" at "http://repository.corespring.org/artifactory/ivy-snapshots"
    val corespringReleases = "Corespring Artifactory Releases" at "http://repository.corespring.org/artifactory/ivy-releases"
    val typesafe = "typesafe releases" at "http://repo.typesafe.com/typesafe/releases/"
    val edeustaceReleases = "ed eustace" at "http://edeustace.com/repository/releases/"
    val edeustaceSnapshots = "ed eustace snapshots" at "http://edeustace.com/repository/snapshots/"
    val spy = "Spy Repository" at "http://files.couchbase.com/maven2"
    val sonatypeSnapshots = "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    val sonatypeReleases = "Sonatype OSS" at "https://oss.sonatype.org/content/repositories/releases"
    val sbtPluginSnapshots = Resolver.url("sbt-plugin-releases", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)
    val sbtPluginReleases = Resolver.url("sbt-plugin-snapshots", new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots/"))(Resolver.ivyStylePatterns)

    // TODO: Publish to repository.corespring.org!!!!!!!
    val justWroteSnapshots = "justwrote" at "http://repo.justwrote.it/snapshots/"

    val all: Seq[Resolver] = Seq(
      localIvy,
      sonatypeSnapshots,
      typesafe,
      corespringSnapshots,
      corespringReleases,
      spy,
      edeustaceSnapshots,
      edeustaceReleases,
      sbtPluginSnapshots,
      sbtPluginReleases,
      justWroteSnapshots)
  }

}
