import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import sbt._
import sbt.Keys._

object BuildInfo {

  val buildInfo = TaskKey[Unit]("build-client", "runs client installation commands")

  val buildInfoTask = buildInfo <<= (classDirectory in Compile, name, version, streams) map {
    (base, n, v, s) =>
      s.log.info("[buildInfo] ---> write build properties file] on " + base.getAbsolutePath)
      val file = base / "buildInfo.properties"
      val commitHash: String = Process("git rev-parse --short HEAD").!!.trim
      val branch: String = Process("git rev-parse --abbrev-ref HEAD").!!.trim
      val formatter = DateTimeFormat.forPattern("HH:mm dd MMMM yyyy");
      val date = formatter.print(DateTime.now)
      val contents = s"""
      |commit.hash=$commitHash
      |branch=$branch
      |version=$v
      |date=$date""".stripMargin.trim

      IO.write(file, contents)
  }
}