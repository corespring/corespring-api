import sbt._
import sbt.Keys._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleasePlugin.autoImport.{ ReleaseStep }
import sbtrelease.Version.Bump
import sbtrelease.Version
import sbtrelease.ReleaseStateTransformations._
import org.corespring.sbtrelease.ReleaseSteps._
import org.corespring.sbtrelease.ReleaseExtrasPlugin._
import org.corespring.sbtrelease.{ Git, PrefixAndVersion, BranchNameConverter }

object HyphenNameConverter extends BranchNameConverter {
  val pattern = """^([^-]+)-([^-]+)$""".r

  override def fromBranchName(branchName: String): Option[PrefixAndVersion] = try {
    val pattern(prefix, versionString) = branchName
    Version(versionString).map { PrefixAndVersion(prefix, _) }
  } catch {
    case t: Throwable => None
  }

  override def toBranchName(pf: PrefixAndVersion): String = s"${pf.prefix}-${pf.version.withoutQualifier.string}"
}

object CustomRelease {

  def unsupportedBranch(b: String) = ReleaseStep(action = st => {
    sys.error(s"Unsupported branch for releasing: $b, must be 'rc' for releases or 'hotfix' for hotfixes")
  })

  //Run `universal:packageZipTarball`
  lazy val buildTgz = ReleaseStep(action = (st: State) => {
    val extracted = Project.extract(st)
    import com.typesafe.sbt.SbtNativePackager._
    import com.typesafe.sbt.packager.Keys._
    val (newState, _) = extracted.runTask(packageZipTarball in Universal, st)
    newState
  })

  def shared(branchName: String, custom: Seq[ReleaseStep]) = Seq(
    prepareReleaseVersion,
    ensureTagDoesntExist("origin", None),
    checkBranchName(branchName),
    checkSnapshotDependencies,
    runClean,
    runTest,
    runIntegrationTest,
    setReleaseVersion,
    commitReleaseVersion) ++
    custom ++
    Seq(
      pushBranchChanges,
      pushTags,
      publishArtifacts,
      buildTgz)

  lazy val settings = Seq(
    branchNameConverter := HyphenNameConverter,
    releaseVersionBump := Bump.Minor,
    releaseProcess <<= baseDirectory.apply { bd =>

      lazy val regularRelease = shared("rc", Seq(
        mergeCurrentBranchTo("master"),
        tagBranchWithReleaseTag("master")))

      lazy val hotfixRelease = shared("hf", Seq(
        mergeCurrentBranchTo("master"),
	tagBranchWithReleaseTag("master")))

      Git(bd).currentBranch match {
        case "rc" => regularRelease
        case "hf" => hotfixRelease
        case branch => Seq(unsupportedBranch(branch))
      }
    })
}
