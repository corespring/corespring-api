package models.item

import play.api.libs.json._

case class Copyright(owner: Option[String] = None,
                     year: Option[String] = None,
                     expirationDate: Option[String] = None,
                     imageName: Option[String] = None)

object Copyright extends ValueGetter {

  object Keys {
    val copyrightOwner = "copyrightOwner"
    val copyrightYear = "copyrightYear"
    val copyrightExpirationDate = "copyrightExpirationDate"
    val copyrightImageName = "copyrightImageName"
  }

  implicit object Reads extends Reads[Copyright] {

    import Keys._

    def reads(json: JsValue): Copyright = {

      val maybeCopyright = get[Copyright](
        json,
        Seq(copyrightOwner, copyrightYear, copyrightExpirationDate, copyrightImageName),
        (s: Seq[Option[String]]) => Some(Copyright(s(0), s(1), s(2), s(3))))

      maybeCopyright.get
    }
  }

  implicit object Writes extends Writes[Copyright] {
    def writes(copyright: Copyright): JsValue = {

      import Keys._

      JsObject(
        Seq(
          copyright.owner.map((copyrightOwner -> JsString(_))),
          copyright.year.map((copyrightYear -> JsString(_))),
          copyright.expirationDate.map((copyrightExpirationDate -> JsString(_))),
          copyright.imageName.map((copyrightImageName -> JsString(_)))
        ).flatten)
    }
  }
}
