import sbt._
import sbt.Keys._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleasePlugin.autoImport.{ ReleaseStep }
import sbtrelease.Version.Bump
import sbtrelease.Version
import sbtrelease.ReleaseStateTransformations._
import org.corespring.sbtrelease.ReleaseSteps._
import org.corespring.sbtrelease.ReleaseExtrasPlugin._
import org.corespring.sbtrelease.{ PrefixAndVersion, BranchNameConverter, FolderStyleConverter }

/**
 * Whilst testing happens map hotfix and release branches to cr-hotfix/0.0.0 or cr-release/0.0.0
 */
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

  //every develop change should go to Devt
  //every release-XXX change should go to QA
  //every master change should go to Staging
  //deploy to prod is manual

  /**
   * What will the ci have to do:
   * for releases update the release var to X.X.X,
   * - checkout that branch and do it's builds etc
   * - when ready run `release --with-defaults`
   *
   * for hotfixes - create a new hotfix var - checkout the branch and do as above.
   */
  lazy val settings = Seq(
    validHotfixParents := Seq("master"),
    validReleaseParents := Seq("develop"),
    branchNameConverter := HyphenNameConverter,
    releaseVersionBump := Bump.Minor,
    releaseProcess <<= thisProjectRef.apply { ref =>
      Seq(
        checkBranchVersion,
        checkSnapshotDependencies,
        runClean,
        runTest,
        runIntegrationTest,
        prepareReleaseVersion,
        setReleaseVersion,
        commitReleaseVersion,
        mergeCurrentBranchTo("master"),
        tagBranchWithReleaseTag("master"),
        //Note: this requires that you run release with defaults `--with-defaults`
        pushChanges,
        publishArtifacts,
        runStage)
    })

}
