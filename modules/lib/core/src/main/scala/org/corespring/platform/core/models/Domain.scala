package org.corespring.platform.core.models

import play.api.libs.json._

case class Domain(name: String, standards: Seq[String])

object Domain extends JsonUtil {

  implicit object Format extends play.api.libs.json.Format[Domain] {

    override def reads(json: JsValue): JsResult[Domain] = JsSuccess(Domain(
      name = (json \ "name").as[String],
      standards = (json \ "standards").asOpt[Seq[String]].getOrElse(Seq.empty[String])
    ))

    override def writes(domain: Domain) = partialObj(
      "name" -> Some(JsString(domain.name)),
      "standards" -> Some(JsArray(domain.standards.map(JsString)))
    )

  }

}
