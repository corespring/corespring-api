import sbt.Keys._
import sbt._

object ComponentsBuilder {

  def isWindows = System.getProperty("os.name").toLowerCase().contains("windows")

  val gruntCmd = "node ./node_modules/grunt-cli/bin/grunt"
  val npmCmd = if (isWindows) "npm.cmd" else "npm"
  val bowerCmd = "node ./node_modules/bower/bin/bower"

  val buildComponents = TaskKey[Unit]("build-components", "Runs compilation for corespring-components assets")

  val buildComponentsTask = buildComponents <<= (baseDirectory, streams) map {
    (baseDir, s) =>
      {
        val clientRoot: File = baseDir
        val componentsRoot: File = new File(clientRoot.getAbsolutePath + "/corespring-components/")

        val commands = Seq(
          // This should be done with -g, but can't assume root access on all machines
          (npmCmd, "install grunt-cli"),
          (npmCmd, "install"),
          (bowerCmd, "install"),
          (gruntCmd, "build"))

        commands.foreach {
          c =>
            {
              s.log.info(s"[>> $c] on " + componentsRoot)
              val (cmd, args) = c
              val exitCode = sbt.Process(s"$cmd $args", componentsRoot).!
              if (exitCode != 0) {
                throw new RuntimeException(s"The following commands failed: $c")
              }
            }
        }
      }
  }

}