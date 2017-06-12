import org.apache.commons.io.FileUtils
import sbt._
import sbt.Keys._

import scala.collection.mutable
import scala.sys.process.ProcessLogger

private class JsTestLogger(s: Keys.TaskStreams) {

  val stdout: StringBuilder = new StringBuilder()
  val stderr: StringBuilder = new StringBuilder()

  def onStdout(line: String) = if (!line.isEmpty) stdout.append(s"$line\n")
  def onStdErr(line: String) = if (!line.isEmpty) stderr.append(s"$line\n")

  def out = stdout.toString
  def err = stderr.toString

  lazy val logger = ProcessLogger(onStdout, onStdErr)
}

object JsTest {

  lazy val removeNodeModules = taskKey[Unit]("remove the js-test node modules dir")

  val removeNodeModulesTask = removeNodeModules := {
    val s = streams.value
    val bd = baseDirectory.value
    val nm = bd / "js-test" / "node_modules"
    FileUtils.deleteDirectory(nm)
  }

  lazy val runJsTests = taskKey[Unit]("Run the js test suite")

  val runJsTestsTask = runJsTests := {

    val s = streams.value
    import scala.sys.process._

    val bd = baseDirectory.value

    val jsTest = bd / "js-test"
    val nm = jsTest / "node_modules"

    val logger = ProcessLogger(line => s.log.debug(line), line => s.log.error(line))

    if (!nm.exists()) {
      s.log.info("[js-test] running npm install...")
      sys.process.Process(Seq("npm", "install"), new java.io.File("js-test")) ! logger
      s.log.info("wait 1 second...")
      Thread.sleep(1000)
      s.log.info("done")
    }

    val cmd = Seq(
      "./node_modules/.bin/karma",
      "start",
      "--single-run",
      "--no-colors")

    s.log.info(s"run test cmd: ${cmd.mkString(" ")}")

    val testLogger = new JsTestLogger(s)

    val exitCode = sys.process.Process(cmd, jsTest) ! testLogger.logger

    exitCode match {
      case 0 => s.log.success("js tests ran successfully")
      case _ => {
        s.log.error(s"bad exit code: $exitCode")
        s.log.info(testLogger.out)
        s.log.error(testLogger.err)
        s.log.info(s"to run the tests, cd to js-test and run: `./node_modules/.bin/karma start --single-run")
      }
    }

  }
}