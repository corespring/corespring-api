package web.views.helpers
import play.api.templates.Html
import models.Organization
import play.api.libs.json.{JsValue, Json}

object MainHelper {

  def htmlSafe(s:String) = {
    Html{
      s.replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replace("\"", "\\\"")
        .replace("\\\"", "\"")
    }
  }

  def toFullJson(orgs:Seq[Organization]) : Html = {
    val jsonOrgs : Seq[JsValue]= orgs.map( Organization.FullWrites.writes(_))
    htmlSafe(Json.stringify(Json.toJson(jsonOrgs)))
  }
}
