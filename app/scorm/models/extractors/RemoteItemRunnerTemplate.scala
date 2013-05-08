package scorm.models.extractors

import common.utils.string
import java.io.File
import scala.io.Source


/**
 * Extractor for the RemoteItem runner html file that gets bundled into the scorm package
 */
object RemoteItemRunnerTemplate {

  val Name: String = "remote-item-runner.html.template"

  def unapply(pair: (File, Map[String,String])) = {

    val (f, tokens) = pair

    if (f == null || !f.exists() || !f.getName.endsWith(Name)) {
      None
    } else {
      val template: String = Source.fromFile(f).mkString
      val contents = string.interpolate(template, string.replaceKey(tokens), string.DollarRegex)
      Some((Name.replace(".template", ""), contents))
    }
  }
}
