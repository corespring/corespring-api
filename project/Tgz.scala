import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.MappingsHelper.directory

/** Settings on what to package into the tarball */
object Tgz{
  val settings = Seq(
    topLevelDirectory := None,
    mappings in Universal += file("Procfile") -> "Procfile",
    mappings in Universal ++= directory("corespring-components/components").map({case (f,p) => f -> s"corespring-components/$p"}))
}