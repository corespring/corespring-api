package web.views.helpers

import org.apache.commons.lang3.StringEscapeUtils
import org.corespring.models.Organization
import play.api.templates.Html
import play.api.libs.json.{ Writes }
import play.api.libs.json.Json._

trait MainHelper {

  implicit def writeOrg: Writes[Organization]

  def safeXml(s: String) = Html(StringEscapeUtils.escapeEcmaScript(s))

  def toFullJson(org: Organization) = Html(stringify(toJson(org)))
}
