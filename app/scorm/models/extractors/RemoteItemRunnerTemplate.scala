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
    case "corespringDomain" => "http://192.168.2.103:9000"
    //use the mock token for now
    case "scormPlayerUrl" => "/scorm-player/" + id + "/run?access_token=" + MockToken
    case _ => "?"
  }
}
