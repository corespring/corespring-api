import sbt._
import sbt.Keys._

object IntegrationTestSettings {

  val alwaysRunInTestOnly: String = " *TestOnlyPreRunTest*"

  lazy val settings = Defaults.itSettings ++ Seq(
    scalaSource in IntegrationTest <<= baseDirectory / "it",
    Keys.parallelExecution in IntegrationTest := false,
    Keys.fork in IntegrationTest := false,
    Keys.logBuffered := false,
    /**
     * Note: Adding qtiToV2 resources so they can be reused in the integration tests
     *
     */
    unmanagedResourceDirectories in IntegrationTest += baseDirectory.value / "modules/lib/qti-to-v2/src/test/resources",
    testOptions in IntegrationTest += Tests.Setup(() => println("Setup Integration Test")),
    testOptions in IntegrationTest += Tests.Cleanup(() => println("Cleanup Integration Test")),

    /**
     * Note: when running test-only for IT, the tests fail if the app isn't booted properly.
     * This is a workaround that *always* calls an empty Integration test first.
     * see: https://www.pivotaltracker.com/s/projects/880382/stories/65191542
     */
    testOnly in IntegrationTest := {
      (testOnly in IntegrationTest).partialInput(alwaysRunInTestOnly).evaluated
    })
}