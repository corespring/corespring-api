import sbt._
import PlayProject._
import sbt.Keys._

object Build extends sbt.Build {

  val appName = "corespring-api"
  val appVersion = "1.0-SNAPSHOT"
  val ScalaVersion    = "2.10.1"

  val customImports = Seq("se.radley.plugin.salat.Binders._",
    "org.corespring.platform.data.mongo.models.VersionedId",
    "org.bson.types.ObjectId",
    "models.versioning.VersionedIdImplicits.Binders._")

  val cred = {

    val f : File =  file( Seq(Path.userHome / ".ivy2"/ ".credentials").mkString )

    def env(k:String) = System.getenv(k)

    if(f.exists()){
      println("using credentials file")
      Credentials(f)
    } else {
      //https://devcenter.heroku.com/articles/labs-user-env-compile
      println("using credentials env vars - you need to have: user-env-compile enabled in heroku")
      Credentials(
        env("ARTIFACTORY_REALM"),
        env("ARTIFACTORY_HOST"),
        env("ARTIFACTORY_USER"),
        env("ARTIFACTORY_PASS")
        )

    }
  }

  val main = play.Project(appName, appVersion, Dependencies.all).settings(
    credentials += cred,
    scalaVersion := ScalaVersion,
    parallelExecution.in(Test) := false,
    routesImport ++= customImports,
    templatesImport ++= Seq("org.bson.types.ObjectId", "org.corespring.platform.data.mongo.models.VersionedId"),
    resolvers ++= Dependencies.Resolvers.all,
    Keys.fork.in(Test) := false,
    scalacOptions ++= Seq("-feature", "-deprecation"),
    (test in Test) <<= (test in Test).map(Commands.runJsTests)
  )
}
