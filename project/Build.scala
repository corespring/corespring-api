import Dependencies._
import sbt._
import Keys._
import PlayProject._

object Build extends sbt.Build {

  val appName = "corespring-api"
  val appVersion = "1.0-SNAPSHOT"

  val main = PlayProject(appName, appVersion, Dependencies.all, mainLang = SCALA).settings(
    parallelExecution.in(Test) := false,
    routesImport += "se.radley.plugin.salat.Binders._",
    templatesImport += "org.bson.types.ObjectId",
    resolvers ++= Resolvers.all,
    (test in Test) <<= (test in Test).map(Commands.runJsTests)
  )
}
