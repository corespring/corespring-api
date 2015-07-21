package org.corespring.models.json

import org.corespring.models.Domain
import play.api.libs.json._

object DomainFormat extends Format[Domain] with JsonUtil {

  override def reads(json: JsValue): JsResult[Domain] = JsSuccess(Domain(
    name = (json \ "name").as[String],
    standards = (json \ "standards").asOpt[Seq[String]].getOrElse(Seq.empty[String])
  ))

  override def writes(domain: Domain) = partialObj(
    "name" -> Some(JsString(domain.name)),
    "standards" -> Some(JsArray(domain.standards.map(JsString)))
  )

}
