package scorm.models.extractors

import java.io.File
import scala.io.Source
import common.seed.StringUtils

object RemoteItemRunnerTemplate {

  val Name : String = "remote-item-runner.html.template"

  def unapply(pair:(File,String)) = {

    val f = pair._1
    val id = pair._2

    if (f == null || !f.exists() || !f.getName.endsWith(Name)) {
     None
    } else {
      val template : String = Source.fromFile(f).mkString
      val contents = StringUtils.interpolate(template, replaceKey, """\$\{([^}]+)\}""".r)
      Some((Name.replace(".template", ""), contents))
    }
  }

  def replaceKey(s:String) : String = {

    s match {
      case "title" => "processed title"
      case "remoteTestPlayerUrl" => "corespring.org"
      case "remoteEasyXdmSwfUrl" => "swfurl"
    }
  }
}
