package org.corespring.models.json

import org.corespring.models.{ Domain, Standard, StandardDomains }
import play.api.libs.json._

object StandardDomainsWrites extends Writes[StandardDomains] {

  override def writes(o: StandardDomains): JsValue = {
    Json.obj(
      Standard.Subjects.ELA -> o.ela.map(Json.toJson(_)(DomainWrites)),
      Standard.Subjects.Math -> o.math.map(Json.toJson(_)(DomainWrites)))
  }
}

object DomainWrites extends Writes[Domain] with JsonUtil {

  override def writes(domain: Domain) = partialObj(
    "name" -> Some(JsString(domain.name)),
    "standards" -> Some(JsArray(domain.standards.map(JsString))))
}
