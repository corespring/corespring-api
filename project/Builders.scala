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

  val moduleConfigEmpty = Seq.empty

  val moduleConfig = Seq(Dependencies.ModuleConfigurations.snapshots,
    Dependencies.ModuleConfigurations.releases,
    Dependencies.ModuleConfigurations.localSnapshots)
}

class Builders[T](root: String, rootSettings: Seq[Setting[T]]) {

  val forkInTests = false

  //TODO: This is not useful at the moment - when it works however it'll be amazing:
  // updateOptions := updateOptions.value.withConsolidatedResolution(true),
  // see: https://github.com/sbt/sbt/issues/2105
  val sharedSettings = Seq(
    moduleConfigurations ++= Builders.moduleConfig,
    aggregate in update := false,
    parallelExecution.in(Test) := false,
    resolvers ++= Dependencies.Resolvers.all,
    credentials += LoadCredentials.cred,
    shellPrompt := ShellPrompt.buildShellPrompt,
    Keys.fork.in(Test) := forkInTests,
    scalacOptions ++=
      Seq("-feature", "-deprecation")) ++
    Builders.disableDocsSettings ++ rootSettings

  def lib(name: String, folder: String = "lib", deps: Seq[sbt.ClasspathDep[sbt.ProjectReference]] = Seq.empty) =

    sbt.Project(
      makeName(name),
      file("modules/" + folder + "/" + name),
      dependencies = deps)
      .settings(Defaults.defaultSettings: _*)
      .settings(sharedSettings: _*)

  def testLib(name: String) = lib(name, "test-lib")

  def web(name: String, root: Option[sbt.File] = None) = {

    val rootFile = root.getOrElse(file(s"modules/web/$name"))

    //Note until we use an updated play we have to to this:
    play.Project(makeName(name), "NOT-USED", path = rootFile)
      .settings(version := (version in ThisBuild).value)
      .settings(sharedSettings: _*)
  }

  private def makeName(s: String): String = if (s == root) root else Seq(root, s).mkString("-")
}