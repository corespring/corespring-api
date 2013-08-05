import sbt._
import Keys._

import play.Project._

object Build extends sbt.Build {

  val appName = "corespring-api"
  val appVersion = "1.0-SNAPSHOT"
  val ScalaVersion    = "2.10.1"

  val customImports = Seq("se.radley.plugin.salat.Binders._",
    "org.corespring.platform.data.mongo.models.VersionedId",
    "org.bson.types.ObjectId",
    "org.corespring.platform.core.models.versioning.VersionedIdImplicits.Binders._")

  val main = play.Project(appName, appVersion, Dependencies.all ).settings(
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
