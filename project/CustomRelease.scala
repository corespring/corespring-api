import sbt._
import sbt.Keys._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleasePlugin.autoImport.{ ReleaseStep }
import sbtrelease.Version.Bump
import sbtrelease.ReleaseStateTransformations._
import org.corespring.sbtrelease.SbtReleaseExtrasSteps._
import org.corespring.sbtrelease.SbtReleaseExtrasPlugin._
import org.corespring.sbtrelease.{ PrefixAndVersion, BranchNameConverter, FolderStyleBranchNameConverter }

/**
 * Whilst testing happens map hotfix and release branches to cr-hotfix/0.0.0 or cr-release/0.0.0
 */
object TestBranchNameConverter extends BranchNameConverter {

  def fromBranchName(branchName: String): Option[PrefixAndVersion] = {
    FolderStyleConverter.fromBranchName(branchName).map { pv =>
      pv.copy(prefix = pv.replace("cr-", ""))
    }
  }

  def toBranchName(pf: PrefixAndVersion): String = {
    FolderStyleConverter.toBranchName(pf).copy(prefix = s"cs-${pf.prefix}")
  }
}

object CustomRelease {

  //Run stage
  val runStage = ReleaseStep(action = st => {
    val extracted = Project.extract(st)
    import com.typesafe.sbt.packager.universal.Keys.stage
    val (newState, _) = extracted.runTask(stage, st)
    newState
  })

  lazy val settings = Seq(
    validHotfixParents := Seq("feature/with-custom-releasing"),
    validReleaseParents := Seq("feature/with-custom-releasing"),
    branchNameConverter := TestBranchNameConverter,
    releaseVersionBump := Bump.Minor,

    //    releaseProcess <<= thisProjectRef.apply { ref =>
    //      Seq(
    //        checkBranchVersion,
    //        checkSnapshotDependencies,
    //        runClean,
    //        runTest,
    //        runIntegrationTest,
    //        prepareReleaseVersion,
    //        setReleaseVersion,
    //        commitReleaseVersion,
    //        tagRelease,
    //        mergeReleaseTagTo("master"),
    //        publishArtifacts,
    //        runStage)
    //    })

    //Trim down release steps for testin
    releaseProcess <<= thisProjectRef.apply { ref =>
      Seq(
        checkBranchVersion,
        runClean,
        prepareReleaseVersion,
        setReleaseVersion,
        commitReleaseVersion,
        tagRelease,
        mergeReleaseTagTo("master"))
    })

}
