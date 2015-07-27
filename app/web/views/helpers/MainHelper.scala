package web.views.helpers

import org.apache.commons.lang3.StringEscapeUtils
import org.corespring.models.Organization
import play.api.templates.Html
import play.api.libs.json.{ JsValue, Json }

object MainHelper {

  def safeXml(s: String) = {
    import StringEscapeUtils._
    Html { escapeEcmaScript(s) }
  }

  def toFullJson(org: Organization): Html = {
    implicit val writeOrg =  developer.ServiceLookup.jsonFormatting.writeOrg
    val jsonOrg: JsValue = Json.toJson(org)
    Html(Json.stringify(jsonOrg))
  }
}
