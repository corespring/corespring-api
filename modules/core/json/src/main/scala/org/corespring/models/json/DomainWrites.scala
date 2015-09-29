package org.corespring.models.json

import org.corespring.models.{ Standard, StandardDomains, Domain }
import play.api.libs.json._

object StandardDomainsWrites extends Writes[StandardDomains] {

  override def writes(o: StandardDomains): JsValue = {
    Json.obj(
      Standard.Subjects.ELA -> o.ela.map(Json.toJson(_)(DomainWrites)),
      Standard.Subjects.Math -> o.math.map(Json.toJson(_)(DomainWrites)))
  }
}

object DomainWrites extends Writes[Domain] with JsonUtil {

  //  override def reads(json: JsValue): JsResult[Domain] = JsSuccess(Domain(
  //    name = (json \ "name").as[String],
  //    standards = (json \ "standards").asOpt[Seq[String]].getOrElse(Seq.empty[String])
  //  ))

  override def writes(domain: Domain) = partialObj(
    "name" -> Some(JsString(domain.name)),
    "standards" -> Some(JsArray(domain.standards.map(JsString))))

}
