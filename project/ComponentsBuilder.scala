import sbt.Keys._
import sbt._

object ComponentsBuilder {

  def isWindows = System.getProperty("os.name").toLowerCase().contains("windows")

  val gruntCmd = "node ./node_modules/grunt-cli/bin/grunt"
  val npmCmd = if (isWindows) "npm.cmd" else "npm"
  val bowerCmd = "node ./node_modules/bower/bin/bower"

  private def runCmd(log: Logger)(cmd: String, root: sbt.File) = {

    log.info(s"[runCmd]: cmd: $cmd, root: ${root.getPath}")
    val exitCode = Process(cmd, root).!

    if (exitCode != 0) {
      sys.error(s"The following process failed: $cmd, folder: ${root.getPath}")
    }
  }

  val installComponents = TaskKey[Unit]("install-components", "install the corespring-components submodule and run `npm install`")

  val installComponentsTask = installComponents <<= (baseDirectory, streams).map {
    (bd, s) =>
      s.log.info("[install-components] running - install components...")
      s.log.info("[install-components] running - git submodule update...")
      runCmd(s.log)("git submodule update --init --recursive", bd)
      s.log.info("[install-components] running - npm install...")
      runCmd(s.log)("npm install", bd / "corespring-components")
  }

  val buildComponents = TaskKey[Unit]("build-components", "Runs compilation for corespring-components assets")

  val buildComponentsTask = buildComponents <<= (baseDirectory, streams) map {
    (baseDir, s) => {
      val clientRoot: File = baseDir
      val componentsRoot: File = new File(clientRoot.getAbsolutePath + "/corespring-components/")

      val commands = Seq(
        // This should be done with -g, but can't assume root access on all machines
        (npmCmd, "install grunt-cli"),
        (npmCmd, "install"),
        (bowerCmd, "install"),
        (gruntCmd, "build"))

      commands.foreach {
        c => {
          val (cmd, args) = c
          runCmd(s.log)(s"$cmd $args", componentsRoot)
        }
      }
    }
  }

  import sbt.Keys._

  lazy val settings = Seq(
    buildComponentsTask,
    installComponentsTask,
    buildComponents <<= buildComponents.dependsOn(installComponents),
    (packagedArtifacts) <<= (packagedArtifacts).dependsOn(buildComponents)
  )

}