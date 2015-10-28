package org.corespring.models.json.item

import org.corespring.models.item.{ AdditionalCopyright => Model }
import org.corespring.models.json.ValueGetter
import play.api.libs.json._

trait AdditionalCopyrightFormat extends ValueGetter with Format[Model] {

  object Keys {
    val author = "author"
    val owner = "owner"
    val year = "year"
    val licenseType = "licenseType"
    val mediaType = "mediaType"
    val sourceUrl = "sourceUrl"
    val costForResource = "costForResource"
  }

  import Keys._

  override def writes(copyright: Model): JsValue = {

    JsObject(
      Seq(
        copyright.author.map((author -> JsString(_))),
        copyright.owner.map((owner -> JsString(_))),
        copyright.year.map((year -> JsString(_))),
        copyright.licenseType.map((licenseType -> JsString(_))),
        copyright.mediaType.map((mediaType -> JsString(_))),
        copyright.sourceUrl.map((sourceUrl -> JsString(_))),
        copyright.costForResource.map((costForResource -> JsNumber(_)))).flatten)
  }

  override def reads(json: JsValue): JsResult[Model] = {

    val maybeCopyright = get[Model](
      json,
      Seq(author, owner, year, licenseType, mediaType, sourceUrl, costForResource),
      (s: Seq[Option[String]]) => Some(Model(s(0), s(1), s(2), s(3), s(4), s(5), (json \ costForResource).asOpt[Int])))

    JsSuccess(maybeCopyright.get)
  }
}

