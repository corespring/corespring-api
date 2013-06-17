package web.views.helpers
import play.api.templates.Html
import models.Organization
import play.api.libs.json.{JsValue, Json}

object MainHelper {

  def safeXml(s:String) = {
    Html{
      s.replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replace("\"", "\\\"")
    }
  }

  def toFullJson(orgs:Seq[Organization]) : Html = {
    val jsonOrgs : Seq[JsValue]= orgs.map( Organization.FullWrites.writes(_))
    Html(Json.stringify(Json.toJson(jsonOrgs)))
  }
}
