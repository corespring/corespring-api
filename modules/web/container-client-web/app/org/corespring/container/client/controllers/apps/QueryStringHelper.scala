package org.corespring.container.client.controllers.apps

import java.net.URLEncoder

import play.api.libs.json.JsObject
import play.api.mvc.RequestHeader

object StrippableParams {
  val params = Seq(
    /**
     * dev|prod - dev loads expanded js/css, prod loads minified
     * mode=prod|dev (default: whichever way the app is run)
     * - dev mode loads all the js as separate files
     * - prod mode loads minified + concatenated js/css
     */
    "mode",
    /**
     * allow logging in the player
     * loggingEnabled=true|false (default: false)
     * - implemented in the jade - whether to allow ng logging.
     */
    "loggingEnabled",
    /** if set log the category defined */
    "logCategory")
}

trait QueryStringHelper {

  def toMap(s: String): Map[String, String] = {

    val arr = s.split("\\&")
    val out: Map[String, String] = arr.foldRight(Map.empty[String, String])((s, acc) => {
      val a = s.split("\\=")
      if (a.length == 2) {
        val Array(key, value) = a
        acc + (key -> value)
      } else {
        acc
      }
    })
    out
  }

  def paramsToStrip: Seq[String] = StrippableParams.params

  def mapToParamString(m: Map[String, String]): String = m.toSeq.map { t =>
    val (key, value) = t
    val encodedValue = URLEncoder.encode(value, "utf-8")
    s"$key=$encodedValue"
  }.mkString("&")

  def mapToJson(m: Map[String, String]): JsObject = {
    import play.api.libs.json._
    Json.toJson(m).asInstanceOf[JsObject]
  }

  protected def mkQueryParams[A](build: (Map[String, String] => A) = mapToParamString _)(implicit rh: RequestHeader): A = {
    val trimmed: Map[String, String] = (rh.queryString -- paramsToStrip).mapValues(s => s.mkString(""))
    build(trimmed)
  }

}
