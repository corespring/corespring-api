import sbt.Keys._
import com.typesafe.sbt.SbtNativePackager._
import sbt._

object ComponentsBuilder {

  //TODO: use the State api - need to find out how to update it within a task - this will do for now.
  private var componentsBuilt = false

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

  val installComponentsTask = installComponents <<= (baseDirectory, streams, state).map {
    (bd, s, state) =>

      val hasDir = new File("corespring-components").isDirectory
      if(hasDir){
        s.log.info("[install-components] found corespring-components directory - skipping - rm this dir to re-install")
      } else {
        s.log.info("[install-components] running - install components...")
        s.log.info("[install-components] running - git submodule update...")
        runCmd(s.log)("git submodule update --init --recursive", bd)
        s.log.info("[install-components] running - npm install...")
        runCmd(s.log)("npm install", bd / "corespring-components")
      }
  }

  val buildComponents = TaskKey[Unit]("build-components", "Runs compilation for corespring-components assets")

  val buildComponentsTask = buildComponents <<= (baseDirectory, streams) map {
    (baseDir, s) => {

      if(componentsBuilt){
        s.log.info("components already built - skipping")
      } else {
        val componentsRoot: File = baseDir / "corespring-components"

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
        componentsBuilt = true
      }
    }
  }

  import sbt.Keys._

  lazy val settings = Seq(
    buildComponentsTask,
    installComponentsTask,
    buildComponents <<= buildComponents.dependsOn(installComponents),
    (packagedArtifacts) <<= (packagedArtifacts).dependsOn(buildComponents),
    //Ensure that the mappings have been updated
    (mappings in Universal) <<= (mappings in Universal).dependsOn(buildComponents).map[Seq[(File,String)]]{ (mappings) =>
      mappings ++ Tgz.componentsMapping
    }
  )

}