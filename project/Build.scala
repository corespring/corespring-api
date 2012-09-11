import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "corespring-api"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "se.radley" %% "play-plugins-salat" % "1.0.7"
      //"se.radley" %% "play-plugins-salat" % "1.1-SNAPSHOT"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      routesImport += "se.radley.plugin.salat.Binders._",
      templatesImport += "org.bson.types.ObjectId",
      resolvers += "Sonatype" at "https://oss.sonatype.org/content/repositories/snapshots/"
    )

}
