import sbt._
import sbt.Keys._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleasePlugin.autoImport.{ ReleaseStep }
import sbtrelease.Version.Bump
import sbtrelease.ReleaseStateTransformations._
import org.corespring.sbtrelease.ReleaseSteps._
import org.corespring.sbtrelease.ReleaseExtrasPlugin._
import org.corespring.sbtrelease.{ PrefixAndVersion, BranchNameConverter, FolderStyleConverter }

/**
 * Whilst testing happens map hotfix and release branches to cr-hotfix/0.0.0 or cr-release/0.0.0
 */
object TestBranchNameConverter extends BranchNameConverter {

  def fromBranchName(branchName: String): Option[PrefixAndVersion] = {
    FolderStyleConverter.fromBranchName(branchName).map { pv =>
      pv.copy(prefix = pv.prefix.replace("cr-", ""))
    }
  }

  def toBranchName(pf: PrefixAndVersion): String = s"cr-${FolderStyleConverter.toBranchName(pf)}"
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
    validHotfixParents := Seq("cr-master"),
    validReleaseParents := Seq("cr-develop"),
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
        mergeCurrentBranchTo("cr-master"),
        //Always tag after merge so that HEAD will have the latest tag.
        tagBranchReleaseTagTo("cr-master")
    })

}
