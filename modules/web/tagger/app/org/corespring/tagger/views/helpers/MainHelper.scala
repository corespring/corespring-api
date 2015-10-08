package web.views.helpers

import org.apache.commons.lang3.StringEscapeUtils
import play.api.templates.Html
import play.api.libs.json.{ JsValue, Json }
import org.corespring.platform.core.models.Organization

object MainHelper {

  def safeXml(s: String) = {
    import StringEscapeUtils._
    Html { escapeEcmaScript(s) }
  }

  def toFullJson(org: Organization): Html = {
    val jsonOrg: JsValue = Organization.FullWrites.writes(org)
    Html(Json.stringify(Json.toJson(jsonOrg)))
  }
}
