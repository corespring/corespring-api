import sbt._
import Keys._

class Builders(root:String, org:String, appVersion:String, rootScalaVersion:String) {

  def lib(name: String, folder:String = "lib", deps: Seq[sbt.ClasspathDep[sbt.ProjectReference]] = Seq.empty) =

    sbt.Project(
      makeName(name),
      file("modules/" + folder + "/" + name),
      dependencies = deps,
      settings = Seq(
        organization := org,
        version := appVersion,
        scalaVersion := rootScalaVersion,
        resolvers ++= Dependencies.Resolvers.all
      ) ++ Defaults.defaultSettings ++ play.Project.intellijCommandSettings("SCALA")
    )

  def testLib(name:String) = lib(name, "test-lib")

  def web(name:String, deps: Seq[ClasspathDep[ProjectReference]] = Seq.empty) = {
    play.Project( makeName(name), appVersion, path = file("modules/web/" + name))
      .settings(organization := org,
      resolvers ++= Dependencies.Resolvers.all
    )
  }

  private def makeName(s:String) : String = Seq(root, s).mkString("-")
}