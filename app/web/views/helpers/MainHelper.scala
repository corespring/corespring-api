package web.views.helpers
import play.api.templates.Html
import play.api.libs.json.{JsValue, Json}
import org.corespring.platform.core.models.Organization

object MainHelper {

  def safeXml(s:String) = {
    Html{
      s.replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replace("\"", "\\\"")
    }
  }

  def toFullJson(org:Organization) : Html = {
    val jsonOrg : JsValue=  Organization.FullWrites.writes(org)
    Html(Json.stringify(Json.toJson(jsonOrg)))
  }
}
