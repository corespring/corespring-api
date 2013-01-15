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

  def json(c: Copyright): Seq[(String, JsValue)] = {

    import Keys._
    Seq(
      c.owner.map((copyrightOwner -> JsString(_))),
      c.year.map((copyrightYear -> JsString(_))),
      c.expirationDate.map((copyrightExpirationDate -> JsString(_))),
      c.imageName.map((copyrightImageName -> JsString(_)))
    ).flatten
  }

  def obj(json: JsValue): Option[Copyright] = {
    import Keys._
    get[Copyright](
      json,
      Seq(copyrightOwner, copyrightYear, copyrightExpirationDate, copyrightImageName),
      (s: Seq[Option[String]]) => Some(Copyright(s(0), s(1), s(2), s(3))))
  }
}
