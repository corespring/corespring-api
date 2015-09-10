package org.corespring.models.json.item

import org.corespring.models.item.{ ContributorDetails, FieldValue }
import org.corespring.models.json.UnacceptableJsonValueException
import org.corespring.models.{ item => model }
import play.api.libs.json._

trait ContributorDetailsFormat extends Format[model.ContributorDetails] {

  implicit def ac: Format[model.AdditionalCopyright]

  implicit def c: Format[model.Copyright]

  def fieldValue: FieldValue

  object Keys {
    val additionalCopyrights = "additionalCopyrights"
    val author = "author"
    val contributor = "contributor"
    val copyright = "copyright"
    val costForResource = "costForResource"
    val credentials = "credentials"
    val credentialsOther = "credentialsOther"
    val licenseType = "licenseType"
    val sourceUrl = "sourceUrl"
  }

  def reads(json: JsValue): JsResult[model.ContributorDetails] = {
    JsSuccess(ContributorDetails(
      additionalCopyrights = (json \ Keys.additionalCopyrights).asOpt[Seq[model.AdditionalCopyright]].getOrElse(Seq()),
      author = (json \ Keys.author).asOpt[String],
      contributor = (json \ Keys.contributor).asOpt[String],
      costForResource = (json \ Keys.costForResource).asOpt[Int],
      copyright = json.asOpt[model.Copyright],
      sourceUrl = (json \ Keys.sourceUrl).asOpt[String],
      licenseType = (json \ Keys.licenseType).asOpt[String],
      credentials = (json \ Keys.credentials).asOpt[String].map { v =>
        if (fieldValue.credentials.exists(_.key == v)) {
          v
        } else {
          throw new UnacceptableJsonValueException(Keys.credentials, v, fieldValue.credentials.map(_.key))
        }
      },
      credentialsOther = (json \ Keys.credentialsOther).asOpt[String]))
  }

  import Keys._

  def writes(details: model.ContributorDetails): JsValue = {

    val s: Seq[Option[(String, JsValue)]] = Seq(
      Some(additionalCopyrights -> Json.toJson(details.additionalCopyrights)),
      details.author.map((author -> JsString(_))),
      details.contributor.map((contributor -> JsString(_))),
      details.costForResource.map((costForResource -> JsNumber(_))),
      details.credentials.map((credentials -> JsString(_))),
      details.credentialsOther.map((credentialsOther -> JsString(_))),
      details.licenseType.map((licenseType -> JsString(_))),
      details.sourceUrl.map((sourceUrl -> JsString(_))))

    val copyrightJson = Json.toJson(details.copyright)

    val detailsJson = JsObject(s.flatten)

    val objects =
      Seq(detailsJson, copyrightJson)
        .filter(_.isInstanceOf[JsObject])
        .map(_.asInstanceOf[JsObject])

    objects.tail.foldRight(objects.head)(_ ++ _)
  }
}
