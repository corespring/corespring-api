import sbt.Keys._
import sbt._

object Build extends sbt.Build {

  val appName = "cs-api-docker-util"
  val appVersion = "1.0"
  val ScalaVersion = "2.10.3"
  val org = "org.corespring"

  def getEnv(prop: String): Option[String] = {
    val env = System.getenv(prop)
    if (env == null) None else Some(env)
  }

  object Dependencies {
    val mongoDbSeeder = "org.corespring" %% "mongo-db-seeder-lib" % "0.9-69e5abf"
    val elasticsearch = "org.corespring" %% "elasticsearch-play-ws" % "0.0.9-PLAY22"
    val scallop = "org.rogach" %% "scallop" % "0.9.5"
    val scalaz = "org.scalaz" %% "scalaz-core" % "7.0.6"
    val jsoup = "org.jsoup" % "jsoup" % "1.8.1"

    //dumb play - only want WS + json
    val play = ("com.typesafe.play" %% "play" % "2.2.1")
      .exclude("com.typesafe.play", "sbt-plugin")
      .exclude("com.typesafe.play", "sbt-link")
      .exclude("commons-logging", "commons-logging")

  }

  object Resolvers {
    val corespringSnapshots = "Corespring Artifactory Snapshots" at "http://repository.corespring.org/artifactory/ivy-snapshots"
    val corespringReleases = "Corespring Artifactory Releases" at "http://repository.corespring.org/artifactory/ivy-releases"
    val all: Seq[Resolver] = Seq(corespringSnapshots, corespringReleases)
  }

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

  import Dependencies._

  val main = sbt.Project(appName, file("."))
    .settings(
      (javacOptions in Compile) ++= Seq("-source", "1.7", "-target", "1.7"),
      credentials += cred,
      libraryDependencies ++= Seq(mongoDbSeeder, elasticsearch, scallop, play, scalaz, jsoup),
      resolvers ++= Resolvers.all,
      scalacOptions ++= Seq("-feature", "-deprecation"))
}
