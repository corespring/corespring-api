import Dependencies._
import sbt._
import Keys._
import PlayProject._

object Build extends sbt.Build {

  val appName = "corespring-api"
  val appVersion = "1.0-SNAPSHOT"

  val customImports = Seq("se.radley.plugin.salat.Binders._",
    "org.corespring.platform.data.mongo.models.VersionedId",
    "org.bson.types.ObjectId",
    "models.versioning.VersionedIdImplicits.Binders._")

  val main = PlayProject(appName, appVersion, Dependencies.all, mainLang = SCALA).settings(
    parallelExecution.in(Test) := false,
    routesImport ++= customImports,
    templatesImport ++= Seq("org.bson.types.ObjectId", "org.corespring.platform.data.mongo.models.VersionedId"),
    resolvers ++= Resolvers.all,
    (test in Test) <<= (test in Test).map(Commands.runJsTests)
  )
}
