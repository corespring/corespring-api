import sbt._
import sbt.Keys._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleasePlugin.autoImport.{ ReleaseStep }
import sbtrelease.Version.Bump
import sbtrelease.Version
import sbtrelease.ReleaseStateTransformations._
import org.corespring.sbtrelease.ReleaseSteps._
import org.corespring.sbtrelease.ReleaseExtrasPlugin._
import org.corespring.sbtrelease.{Git, PrefixAndVersion, BranchNameConverter, FolderStyleConverter}

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

  //Run stage
  val runStage = ReleaseStep(action = st => {
    val extracted = Project.extract(st)
    import com.typesafe.sbt.packager.universal.Keys.stage
    val (newState, _) = extracted.runTask(stage, st)
    newState
  })

  def unsupportedBranch(b:String) = ReleaseStep(action = st => {
    sys.error(s"Unsupported branch for releasing: $b, must be 'rc' for releases or 'hotfix' for hotfixes")
  })

  /**
    * main releases are run from 'rc'.
    * hotfixes from 'hf'
    */
  lazy val settings = Seq(
    branchNameConverter := HyphenNameConverter,
    releaseVersionBump := Bump.Minor,
    releaseProcess <<= baseDirectory.apply { bd =>

      def shared(branchName:String) = Seq(
        checkBranchName(branchName),
        checkSnapshotDependencies,
        runClean,
        runTest,
        runIntegrationTest,
        prepareReleaseVersion,
        setReleaseVersion,
        commitReleaseVersion)

      val regularRelease = shared("rc") ++ Seq(
        pushBranchChanges,
        mergeCurrentBranchTo("master"),
        tagBranchWithReleaseTag("master"),
        pushBranchChanges,
        pushTags,
        publishArtifacts)

      val hotfixRelease = shared("hf") ++ Seq(
        tagBranchWithReleaseTag("hf"),
        pushBranchChanges,
        pushTags,
        publishArtifacts)

      Git(bd).currentBranch match {
        case "rc" => regularRelease
        case "hf" => hotfixRelease
        case branch => Seq(unsupportedBranch(branch))
      }
    })
}
