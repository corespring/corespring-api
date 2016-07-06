package buildInfo

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import sbt._
import sbt.Keys._
import sbtbuildinfo.{ BuildInfoKey, BuildInfoOption, BuildInfoPlugin }
import sbtbuildinfo.BuildInfoKeys._
object Implicits {
  implicit class BuildInfoExtension(p: Project) {

    def addBuildInfo(): Project = {
      p.enablePlugins(BuildInfoPlugin)
        .settings(
          buildInfoOptions += BuildInfoOption.ToJson,
          buildInfoKeys := Seq[BuildInfoKey](name, version),
          buildInfoKeys ++= Seq[BuildInfoKey](
            BuildInfoKey.action("commitHash") {
              Process("git rev-parse --short HEAD").!!.trim
            },
            BuildInfoKey.action("branch") {
              Process("git rev-parse --abbrev-ref HEAD").!!.trim
            },
            BuildInfoKey.action("date") {
              val formatter = DateTimeFormat.forPattern("HH:mm dd MMMM yyyy");
              val date = formatter.print(DateTime.now)
              date.toString
            }),
          buildInfoPackage := "org.corespring.csApi.buildInfo")
    }
  }

}
