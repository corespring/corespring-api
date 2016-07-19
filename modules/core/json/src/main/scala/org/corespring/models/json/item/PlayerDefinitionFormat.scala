package org.corespring.models.json.item

import org.corespring.models.item.PlayerDefinition
import org.corespring.models.item.resource.BaseFile
import org.corespring.models.json.item.resource.BaseFileFormat
import play.api.libs.json._

object PlayerDefinitionFormat extends Format[PlayerDefinition] {

  implicit val bf: Format[BaseFile] = BaseFileFormat

  override def writes(o: PlayerDefinition): JsValue = {
    Json.obj(
      "xhtml" -> o.xhtml,
      "files" -> o.files.map(f => Json.toJson(f)),
      "components" -> o.components,
      "config" -> o.config,
      "summaryFeedback" -> o.summaryFeedback) ++
      o.customScoring.map { cs => Json.obj("customScoring" -> cs) }.getOrElse(Json.obj())
  }

  override def reads(json: JsValue): JsResult[PlayerDefinition] = json match {
    case obj: JsObject => {
      JsSuccess(new PlayerDefinition(
        (json \ "files").asOpt[Seq[BaseFile]].getOrElse(Seq.empty),
        (json \ "xhtml").as[String],
        (json \ "components").asOpt[JsValue].getOrElse(Json.obj()),
        (json \ "summaryFeedback").asOpt[String].getOrElse(""),
        (json \ "customScoring").asOpt[String],
        (json \ "config").asOpt[JsObject].getOrElse(Json.obj())))
    }
    case _ => JsError("empty object")
  }
}
