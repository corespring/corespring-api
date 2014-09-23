package org.corespring.scorm.models.extractors

import java.io.File
import org.corespring.common.utils.string
import scala.io.Source

/**
 * Extractor for the RemoteItem runner html file that gets bundled into the scorm package
 */
object RemoteItemRunnerTemplate {

  lazy val FinalName = Name.replace(".template", "")
  val Name: String = "remote-item-runner.html.template"

  def unapply(pair: (File, Map[String, String])) = {

    val (f, tokens) = pair

    if (f == null || !f.exists() || !f.getName.endsWith(Name)) {
      None
    } else {
      val d = Source.fromFile(f)
      val template: String = d.mkString
      d.close()
      val contents = string.interpolate(template, string.replaceKey(tokens), string.DollarRegex)
      Some(FinalName, contents)
    }
  }
}
