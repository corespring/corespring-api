import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtNativePackager._

object Tgz {

  /** return a Seq of mappings which effect is to add a whole directory in the generated package */
  def directory(sourceDir: File): Seq[(File, String)] = {
    val parentFile = sourceDir.getParentFile
    if (parentFile != null)
      sourceDir.*** pair relativeTo(sourceDir.getParentFile)
    else
      sourceDir.*** pair basic
  }

  /** It lightens the build file if one wants to give a string instead of file. */
  def directory(sourceDir: String): Seq[(File, String)] = {
    directory(file(sourceDir))
  }

  val settings = Seq(
    //topLevelDirectory := None, - not available in 0.6.2 sbt-native-packager
    mappings in Universal += file("Procfile") -> "Procfile",
    mappings in Universal ++= directory("corespring-components/components") //.map{ t => t._1.get.head -> t._2}
    )

}