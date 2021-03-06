import sbt._
import Keys._
import org.corespring.sbt.repo.RepoAuthPlugin.Keys._

object Builders {

  val disableDocsSettings = Seq(
    // disable publishing the main API jar
    publishArtifact in (Compile, packageDoc) := false,
    // disable publishing the main sources jar
    publishArtifact in (Compile, packageSrc) := false,
    sources in doc in Compile := List())

  //Note: This is disabled at the moment due to: https://github.com/sbt/sbt/issues/2282
  val moduleConfig = Nil /*Seq(
    Dependencies.ModuleConfigurations.snapshots,
    Dependencies.ModuleConfigurations.releases)*/
}

class Builders[T](root: String, rootSettings: Seq[Setting[T]]) {

  val forkInTests = false

  val skipPublishSettings = Seq(
    /** Note: publishArtifact := false seems to mess with routes */
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo"))))

  val sharedSettings = Seq(
    //TODO: This is not useful at the moment - when it works however it'll be amazing:
    // see: https://github.com/sbt/sbt/issues/2105 - update: fixed in 13.9 - but we're having issues using 13.9
    //updateOptions := updateOptions.value.withConsolidatedResolution(true),
    moduleConfigurations ++= Builders.moduleConfig,
    aggregate in update := false,
    parallelExecution.in(Test) := false,
    resolvers ++= Dependencies.Resolvers.all,
    shellPrompt := ShellPrompt.buildShellPrompt,
    Keys.fork.in(Test) := forkInTests,
    scalacOptions ++=
      Seq("-feature", "-deprecation")) ++
    Builders.disableDocsSettings ++ rootSettings

  def lib(name: String, folder: String = "lib", deps: Seq[sbt.ClasspathDep[sbt.ProjectReference]] = Seq.empty, publish: Boolean = false) = {

    val p = sbt.Project(
      makeName(name),
      file("modules/" + folder + "/" + name),
      dependencies = deps)
      .settings(Defaults.defaultSettings: _*)
      .settings(sharedSettings: _*)

    if (publish) {
      p.settings(publishTo := authPublishTo.value)
    } else {
      p.settings(skipPublishSettings: _*)
    }
  }

  def testLib(name: String) = lib(name, "test-lib")

  def web(name: String, root: Option[sbt.File] = None, disablePackaging: Boolean = true) = {

    import com.typesafe.sbt.SbtNativePackager._

    val rootFile = root.getOrElse(file(s"modules/web/$name"))

    //Note until we use an updated play we have to to this:
    val project = play.Project(makeName(name), "NOT-USED", path = rootFile)
      .settings(version := (version in ThisBuild).value)
      .settings(sharedSettings: _*)
      .settings(skipPublishSettings: _*)

    if (disablePackaging) {
      //For now just remove all the jars - an empty seq throws an error.
      //Once we upgrade this should be easier.
      project.settings(mappings in Universal := {
        val universalMappings = (mappings in Universal).value
        universalMappings.filterNot {
          case (file, name) => name.endsWith(".jar")
        }
      })

    } else project
  }

  private def makeName(s: String): String = if (s == root) root else Seq(root, s).mkString("-")
}
