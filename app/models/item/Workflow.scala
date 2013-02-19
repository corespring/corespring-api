package models.item

import play.api.libs.json._

case class Workflow(var setup: Boolean = false,
                    var tagged: Boolean = false,
                    var standardsAligned: Boolean = false,
                    var qaReview: Boolean = false)

object Workflow {
  val setup: String = "setup"
  val tagged: String = "tagged"
  val standardsAligned: String = "standardsAligned"
  val qaReview: String = "qaReview"

  implicit object WorkflowWrites extends Writes[Workflow] {

    def writes(workflow: Workflow) = {

      JsObject(Seq(
        setup -> JsBoolean(workflow.setup),
        tagged -> JsBoolean(workflow.tagged),
        standardsAligned -> JsBoolean(workflow.standardsAligned),
        qaReview -> JsBoolean(workflow.qaReview)
      ))
    }
  }

  implicit object WorkflowReads extends Reads[Workflow] {
    def reads(json: JsValue): Workflow = {

      Workflow(
        setup = (json \ setup).asOpt[Boolean].getOrElse(false),
        tagged = (json \ tagged).asOpt[Boolean].getOrElse(false),
        standardsAligned = (json \ standardsAligned).asOpt[Boolean].getOrElse(false),
        qaReview = (json \ qaReview).asOpt[Boolean].getOrElse(false)
      )
    }
  }

}

