package org.corespring.platform.core.models.item

import org.slf4j.LoggerFactory
import play.api.libs.json._
import org.corespring.platform.core.models.json.JsonValidationException
import play.api.Logger

case class ContributorDetails(
  var additionalCopyrights: Seq[AdditionalCopyright] = Seq(),
  var author: Option[String] = None,
  var contributor: Option[String] = None,
  var copyright: Option[Copyright] = None,
  var costForResource: Option[Int] = None,
  var credentials: Option[String] = None,
  var credentialsOther: Option[String] = None,
  var licenseType: Option[String] = None,
  var sourceUrl: Option[String] = None)

object ContributorDetails extends ValueGetter {

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

  //implicit val Formats = Json.format[ContributorDetails]
  implicit object Reads extends Reads[ContributorDetails] {

    import Keys._

    def reads(json: JsValue): JsResult[ContributorDetails] = {
      JsSuccess(ContributorDetails(
        additionalCopyrights = (json \ additionalCopyrights).asOpt[Seq[AdditionalCopyright]].getOrElse(Seq()),
        author = (json \ author).asOpt[String],
        contributor = (json \ contributor).asOpt[String],
        costForResource = (json \ costForResource).asOpt[Int],
        copyright = json.asOpt[Copyright],
        sourceUrl = (json \ sourceUrl).asOpt[String],
        licenseType = (json \ licenseType).asOpt[String],
        credentials = (json \ credentials).asOpt[String].
          map(v => if (fieldValues.credentials.exists(_.key == v)) v else throw new JsonValidationException(credentials)),
        credentialsOther = (json \ credentialsOther).asOpt[String]))
    }
  }

  implicit object Writes extends Writes[ContributorDetails] {

    import Keys._

    def writes(details: ContributorDetails): JsValue = {

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
}
