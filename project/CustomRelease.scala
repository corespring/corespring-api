import sbt._
import sbt.Keys._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleasePlugin.autoImport.{ ReleaseStep }
import sbtrelease.Version.Bump

object CustomRelease {

  import sbtrelease.ReleaseStateTransformations._
  import org.corespring.sbtrelease.SbtReleaseExtrasSteps._
  import org.corespring.sbtrelease.SbtReleaseExtrasPlugin._

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
