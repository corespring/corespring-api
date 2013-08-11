import sbt.Keys._
import sbt._

object Dependencies {

  val aws = "com.amazonaws" % "aws-java-sdk" % "1.3.10"
  val salatPlay =  "se.radley" %% "play-plugins-salat" % "1.2"
  val playPluginUtil = "com.typesafe" %% "play-plugins-util" % "2.1.0"
  val playPluginMailer = "com.typesafe" %% "play-plugins-mailer" % "2.1.0"
  val jbcrypt = "org.mindrot" % "jbcrypt" % "0.3m"
  val securesocial = "securesocial" %% "securesocial" % "master-SNAPSHOT"
  val playMemcached = "com.github.mumoshu" %% "play2-memcached" % "0.3.0.3"
  val mockito = "org.mockito" % "mockito-all" % "1.9.5" % "test"
  val amapClient = "com.rabbitmq" % "amqp-client" % "3.0.2"
  val scalaz = "org.scalaz" %% "scalaz-core" % "7.0.2"
  val assetsLoader = "com.ee" %% "assets-loader" % "0.10.1-8ab100a"
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

    case class Repo(name: String, releases: Option[Resolver] = None, snapshot: Option[Resolver] = None) {
      def repos: Seq[Resolver] = Seq(releases, snapshot).flatten
    }

    object Repo {
      def make(name: String, baseUrl: String, pattern: Patterns = Resolver.mavenStylePatterns): Repo = {
        Repo(name,
          Some(Resolver.url(name, url(baseUrl + "releases/"))(pattern)),
          Some(Resolver.url((name + " snapshots"), url(baseUrl + "snapshots/"))(pattern))
        )
      }
    }

    val edeustaceReleases= "ed eustace" at "http://edeustace.com/repository/releases/"
    val edeustaceSnapshots = "ed eustace snapshots" at "http://edeustace.com/repository/snapshots/"
    val securesocial = Repo.make("securesocial", "http://securesocial.ws/repository/", Resolver.ivyStylePatterns)
    val sonatype = Repo.make("Sonatype", "https://oss.sonatype.org/content/repositories/")
    //val sonatypeReleases = "Sonatype releases" at "http://oss.sonatype.org/content/repositories/releases/"
    val spy = "Spy Repository" at "http://files.couchbase.com/maven2"
    val sonatypeSnapshots= "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    val sonatypeReleases = "Sonatype OSS" at "https://oss.sonatype.org/content/repositories/releases"
    val sbtPluginReleases = Resolver.url("sbt-plugin-releases", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)
    val sbtPluginSnapshots =  Resolver.url("sbt-plugin-snapshots", new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots/"))(Resolver.ivyStylePatterns)
    val all: Seq[Resolver] = Seq(
      spy,
      edeustaceSnapshots,
      edeustaceReleases,
      sonatypeSnapshots,
      sonatypeSnapshots,
      sbtPluginSnapshots,
      sbtPluginReleases)
  }

}
