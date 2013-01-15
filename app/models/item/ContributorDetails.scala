package models.item

import play.api.libs.json._
import models.FieldValue
import controllers.JsonValidationException


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

  def json(details: ContributorDetails): Seq[(String, JsValue)] = {

    import Keys._

    val s: Seq[Option[(String, JsValue)]] = Seq(
      details.author.map((author -> JsString(_))),
      details.contributor.map((contributor -> JsString(_))),
      details.costForResource.map((costForResource -> JsNumber(_))),
      details.credentials.map((credentials -> JsString(_))),
      details.licenseType.map((licenseType -> JsString(_))),
      details.sourceUrl.map((sourceUrl -> JsString(_)))
    )

    val copyright = details.copyright.map(Copyright.json(_))

    s.flatten ++ copyright.getOrElse(Seq())
  }

  def obj(json: JsValue): Option[ContributorDetails] = {

    import Keys._

    Some(ContributorDetails(
      author = (json \ author).asOpt[String],
      contributor = (json \ contributor).asOpt[String],
      costForResource = (json \ costForResource).asOpt[Int],
      copyright = Copyright.obj(json),
      sourceUrl = (json \ sourceUrl).asOpt[String],
      licenseType = (json \ licenseType).asOpt[String],
      credentials = (json \ credentials).asOpt[String].
        map(v => if (fieldValues.credentials.exists(_.key == v)) v else throw new JsonValidationException(credentials))
    ))
  }
}
