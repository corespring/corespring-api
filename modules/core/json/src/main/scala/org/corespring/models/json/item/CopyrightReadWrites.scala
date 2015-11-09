package org.corespring.models.json.item

import org.corespring.models.item.Copyright
import org.corespring.models.json.ValueGetter
import org.corespring.models.{ item => model }
import play.api.libs.json._

private[json] case class JsonCopyright(
  copyrightOwner: Option[String],
  copyrightYear: Option[String],
  copyrightExpirationDate: Option[String],
  copyrightImageName: Option[String]) {

  def toCopyright = Copyright(copyrightOwner, copyrightYear, copyrightExpirationDate, copyrightImageName)
}

object JsonCopyright {
  def fromCopyright(c: Copyright): JsonCopyright = JsonCopyright(c.owner, c.year, c.expirationDate, c.imageName)
}

trait CopyrightFormat extends Format[Copyright] with ValueGetter {

  def reads(json: JsValue) = {
    Json.reads[JsonCopyright].reads(json).map(_.toCopyright)
  }

  def writes(copyright: model.Copyright): JsValue = {
    Json.writes[JsonCopyright].writes(JsonCopyright.fromCopyright(copyright))
  }

}
