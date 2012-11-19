package scorm.models.extractors

import java.io.File
import scala.io.Source
import common.seed.StringUtils
import common.mock._
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
      val contents = StringUtils.interpolate(template, replaceKey(tokens), """\$\{([^}]+)\}""".r)
      Some((Name.replace(".template", ""), contents))
    }
  }

  def replaceKey(tokens: Map[String,String])(s: String): String = tokens.getOrElse(s,"?")
}
