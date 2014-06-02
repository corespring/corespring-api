package org.corespring.platform.core.models.item

import play.api.libs.json._
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import scala.Some

case class AdditionalCopyright(author: Option[String] = None,
  owner: Option[String] = None,
  year: Option[String] = None,
  licenseType: Option[String] = None,
  mediaType: Option[String] = None,
  sourceUrl: Option[String] = None)

object AdditionalCopyright extends ValueGetter {

  object Keys {
    val author = "author"
    val owner = "owner"
    val year = "year"
    val licenseType = "licenseType"
    val mediaType = "mediaType"
    val sourceUrl = "sourceUrl"
  }

  implicit object Format extends Format[AdditionalCopyright] {
    import Keys._

    override def writes(copyright: AdditionalCopyright): JsValue = {

      JsObject(
        Seq(
          copyright.author.map((author -> JsString(_))),
          copyright.owner.map((owner -> JsString(_))),
          copyright.year.map((year -> JsString(_))),
          copyright.licenseType.map((licenseType -> JsString(_))),
          copyright.mediaType.map((mediaType -> JsString(_))),
          copyright.sourceUrl.map((sourceUrl -> JsString(_)))).flatten)
    }

    override def reads(json: JsValue): JsResult[AdditionalCopyright] = {

      val maybeCopyright = get[AdditionalCopyright](
        json,
        Seq(author, owner, year, licenseType, mediaType, sourceUrl),
        (s: Seq[Option[String]]) => Some(AdditionalCopyright(s(0), s(1), s(2), s(3), s(4), s(5))))

      JsSuccess(maybeCopyright.get)
    }
  }

}