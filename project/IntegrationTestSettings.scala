import sbt._
import sbt.Keys._

object IntegrationTestSettings {

  val alwaysRunInTestOnly: String = " *TestOnlyPreRunTest*"

  lazy val settings = Defaults.itSettings ++ Seq(
    libraryDependencies ++= Seq(Dependencies.scalaFaker % "it"),
    scalaSource in IntegrationTest <<= baseDirectory / "it",
    scalacOptions in IntegrationTest ++= Seq("-Yrangepos"),
    Keys.parallelExecution in IntegrationTest := false,
    Keys.fork in IntegrationTest := false,
    Keys.logBuffered := false,
    /**
     * Note: Adding qtiToV2 resources so they can be reused in the integration tests
     *
     */
    unmanagedResourceDirectories in IntegrationTest += baseDirectory.value / "modules/lib/qti-to-v2/src/test/resources",
    testOptions in IntegrationTest += Tests.Setup((loader: java.lang.ClassLoader) => {
      println("-------------> Corespring Api Integration Test::setup")
      loader.loadClass("org.corespring.it.Setup").newInstance
    }),
    testOptions in IntegrationTest += Tests.Cleanup((loader: java.lang.ClassLoader) => {
      println("-------------> Corespring Api Integration Test::cleanup")
      loader.loadClass("org.corespring.it.Cleanup").newInstance
    }),

    /**
     * Note: when running test-only for IT, the tests fail if the app isn't booted properly.
     * This is a workaround that *always* calls an empty Integration test first.
     * see: https://www.pivotaltracker.com/s/projects/880382/stories/65191542
     */
    testOnly in IntegrationTest := {
      (testOnly in IntegrationTest).partialInput(alwaysRunInTestOnly).evaluated
    })
}