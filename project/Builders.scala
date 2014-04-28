import sbt._
import Keys._
import play.Project._

class Builders(root:String, org:String, appVersion:String, rootScalaVersion:String, sharedSettings : Seq[Setting[_]]) {

  def lib(name: String, folder:String = "lib", deps: Seq[sbt.ClasspathDep[sbt.ProjectReference]] = Seq.empty) =

    sbt.Project(
      makeName(name),
      file("modules/" + folder + "/" + name),
      dependencies = deps)
      .settings( Defaults.defaultSettings ++ intellijCommandSettings : _* )
      .settings(
        organization := org,
        version := appVersion,
        scalaVersion := rootScalaVersion,
        resolvers ++= Dependencies.Resolvers.all)
      .settings(sharedSettings : _*)

  def testLib(name:String) = lib(name, "test-lib")

  def web(name:String, root : Option[sbt.File] = None) = {

    val rootFile = root.getOrElse(file(s"modules/web/$name"))

    play.Project( makeName(name), appVersion, path = rootFile)
      .settings(
        organization := org,
        scalaVersion := rootScalaVersion,
        resolvers ++= Dependencies.Resolvers.all
    )
    .settings(sharedSettings: _*)
  }

  private def makeName(s:String) : String = if(s == root) root else Seq(root, s).mkString("-")
}