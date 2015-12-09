import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtNativePackager._

object Tgz {

  /** return a Seq of mappings which effect is to add a whole directory in the generated package */
  def directory(sourceDir: File, parent: File): Seq[(File, String)] = {
    val parentFile = sourceDir.getParentFile
    if (parentFile != null)
      sourceDir.*** pair relativeTo(parent)
    else throw new RuntimeException("parent is null")
  }

  val settings = Seq(
    //topLevelDirectory := None, - not available in 0.6.2 sbt-native-packager
    mappings in Universal += file("Procfile") -> "Procfile",
    mappings in Universal ++= directory(file("corespring-components/components"), file(".")))

}