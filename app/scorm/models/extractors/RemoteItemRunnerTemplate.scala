package scorm.models.extractors

import java.io.File
import scala.io.Source
import common.seed.StringUtils

/**
 * Extractor for the RemoteItem runner html file that gets bundled into the scorm package
 */
object RemoteItemRunnerTemplate {

  val Name: String = "remote-item-runner.html.template"

  def unapply(pair: (File, String, String)) = {

    val (f, id, token) = pair

    if (f == null || !f.exists() || !f.getName.endsWith(Name)) {
      None
    } else {
      val template: String = Source.fromFile(f).mkString
      val contents = StringUtils.interpolate(template, replaceKey(id, token), """\$\{([^}]+)\}""".r)
      Some((Name.replace(".template", ""), contents))
    }
  }

  def replaceKey(id: String, token: String)(s: String): String = s match {
    case "corespringDomain" => "http://localhost:9000"
    case "scormPlayerUrl" => "/scorm-player/" + id + "/run?access_token=" + token
    case _ => "?"
  }
}
