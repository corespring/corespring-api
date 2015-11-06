package org.corespring.models.json.item

import org.corespring.models.item.Copyright
import org.corespring.models.json.ValueGetter
import org.corespring.models.{ item => model }
import play.api.libs.json._

trait CopyrightFormat extends Format[Copyright] with ValueGetter {


    object Keys {
      val copyrightOwner = "copyrightOwner"
      val copyrightYear = "copyrightYear"
      val copyrightExpirationDate = "copyrightExpirationDate"
      val copyrightImageName = "copyrightImageName"
      val owner = "owner"
      val year = "year"
      val expirationDate = "expirationDate"
      val imageName = "imageName"
    }

    import Keys._

    def reads(json: JsValue): JsResult[model.Copyright] = {
      val maybeCopyright = get[model.Copyright](
        json,
        Seq(copyrightOwner, copyrightYear, copyrightExpirationDate, copyrightImageName),
        (s: Seq[Option[String]]) => Some(model.Copyright(s(0), s(1), s(2), s(3))))

      JsSuccess(maybeCopyright.get)
    }

    def writes(copyright: model.Copyright): JsValue = {
      JsObject(
        Seq(
          copyright.owner.map((copyrightOwner -> JsString(_))),
          copyright.year.map((copyrightYear -> JsString(_))),
          copyright.expirationDate.map((copyrightExpirationDate -> JsString(_))),
          copyright.imageName.map((copyrightImageName -> JsString(_)))).flatten)
    }


}
