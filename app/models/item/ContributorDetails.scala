package models.item

import play.api.libs.json._
import controllers.JsonValidationException
import models.item.FieldValue


case class ContributorDetails(
                               var contributor: Option[String] = None,
                               var credentials: Option[String] = None,
                               var copyright: Option[Copyright] = None,
                               var author: Option[String] = None,
                               var sourceUrl: Option[String] = None,
                               var licenseType: Option[String] = None,
                               var costForResource: Option[Int] = None
                               )

object ContributorDetails extends ValueGetter {

  object Keys {
    val author = "author"
    val credentials = "credentials"
    val contributor = "contributor"
    val copyright = "copyright"
    val sourceUrl = "sourceUrl"
    val licenseType = "licenseType"
    val costForResource = "costForResource"
  }


  implicit object Reads extends Reads[ContributorDetails] {

    import Keys._

    def reads(json: JsValue): ContributorDetails = {
      ContributorDetails(
        author = (json \ author).asOpt[String],
        contributor = (json \ contributor).asOpt[String],
        costForResource = (json \ costForResource).asOpt[Int],
        copyright = json.asOpt[Copyright],
        sourceUrl = (json \ sourceUrl).asOpt[String],
        licenseType = (json \ licenseType).asOpt[String],
        credentials = (json \ credentials).asOpt[String].
          map(v => if (fieldValues.credentials.exists(_.key == v)) v else throw new JsonValidationException(credentials))
      )
    }
  }

  implicit object Writes extends Writes[ContributorDetails] {

    import Keys._

    def writes(details: ContributorDetails): JsValue = {

      val s: Seq[Option[(String, JsValue)]] = Seq(
        details.author.map((author -> JsString(_))),
        details.contributor.map((contributor -> JsString(_))),
        details.costForResource.map((costForResource -> JsNumber(_))),
        details.credentials.map((credentials -> JsString(_))),
        details.licenseType.map((licenseType -> JsString(_))),
        details.sourceUrl.map((sourceUrl -> JsString(_)))
      )

      val copyrightJson = Json.toJson(details.copyright)
      val detailsJson = JsObject(s.flatten)

      val objects =
        Seq(detailsJson,copyrightJson)
        .filter(_.isInstanceOf[JsObject])
        .map(_.asInstanceOf[JsObject])

      objects.tail.foldRight(objects.head)(_ ++ _)
    }
  }
}
