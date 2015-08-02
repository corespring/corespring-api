package web.views.helpers

import org.apache.commons.lang3.StringEscapeUtils
import org.corespring.models.Organization
import play.api.templates.Html
import play.api.libs.json.{ Writes, JsValue, Json }

object MainHelper {

  def safeXml(s: String) = {
    import StringEscapeUtils._
    Html { escapeEcmaScript(s) }
  }

  def toFullJson(orgIn: Organization): Html = {
    implicit val writeOrg: Writes[Organization] = org.corespring.legacy.ServiceLookup.jsonFormatting.writeOrg
    val jsonOrg: JsValue = Json.toJson(orgIn)
    Html(Json.stringify(jsonOrg))
  }
}
