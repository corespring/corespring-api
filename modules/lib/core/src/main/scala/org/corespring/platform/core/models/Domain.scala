package org.corespring.platform.core.models

import play.api.libs.json._

case class Domain(name: String, fromStandard: Boolean) {
  override def toString() = s"name = $name, fromStandard = $fromStandard"
}

object Domain {

  implicit object Format extends play.api.libs.json.Format[Domain] {

    override def reads(json: JsValue): JsResult[Domain] = JsSuccess(Domain(
      name = (json \ "name").as[String],
      fromStandard = (json \ "fromStandard").asOpt[Boolean].getOrElse(false)
    ))

    override def writes(domain: Domain) = Json.obj(
      "name" -> domain.name,
      "fromStandard" -> domain.fromStandard
    )

  }

}
