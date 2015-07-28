import sbt._
import Keys._
import play.Project._

object Builders {

  val disableDocsSettings = Seq(
    // disable publishing the main API jar
    publishArtifact in (Compile, packageDoc) := false,
    // disable publishing the main sources jar
    publishArtifact in (Compile, packageSrc) := false,
    sources in doc in Compile := List())

}
class Builders(root: String, org: String, appVersion: String, rootScalaVersion: String) {

  val forkInTests = false

  //TODO: This is not useful at the moment - when it works however it'll be amazing:
  // updateOptions := updateOptions.value.withConsolidatedResolution(true),
  // see: https://github.com/sbt/sbt/issues/2105
  val sharedSettings = Seq(
    moduleConfigurations ++= Seq( /*Dependencies.ModuleConfigurations.snapshots,*/ Dependencies.ModuleConfigurations.releases, Dependencies.ModuleConfigurations.localSnapshots),
    aggregate in update := false,
    scalaVersion := rootScalaVersion,
    parallelExecution.in(Test) := false,
    resolvers ++= Dependencies.Resolvers.all,
    credentials += LoadCredentials.cred,
    Keys.fork.in(Test) := forkInTests,
    scalacOptions ++= Seq("-feature", "-deprecation")) ++ Builders.disableDocsSettings

  def lib(name: String, folder: String = "lib", deps: Seq[sbt.ClasspathDep[sbt.ProjectReference]] = Seq.empty) =

    sbt.Project(
      makeName(name),
      file("modules/" + folder + "/" + name),
      dependencies = deps)
      .settings(Defaults.defaultSettings ++ intellijCommandSettings: _*)
      .settings(
        organization := org,
        version := appVersion,
        scalaVersion := rootScalaVersion,
        resolvers ++= Dependencies.Resolvers.all)
      .settings(sharedSettings: _*)

  def testLib(name: String) = lib(name, "test-lib")

  def web(name: String, root: Option[sbt.File] = None) = {

    val rootFile = root.getOrElse(file(s"modules/web/$name"))

    play.Project(makeName(name), appVersion, path = rootFile)
      .settings(
        organization := org,
        scalaVersion := rootScalaVersion,
        resolvers ++= Dependencies.Resolvers.all)
      .settings(sharedSettings: _*)
  }

  private def makeName(s: String): String = if (s == root) root else Seq(root, s).mkString("-")
}