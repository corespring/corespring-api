package org.corespring.models.assessment

import org.bson.types.ObjectId
import org.joda.time.DateTime

case class AssessmentTemplate(id: ObjectId = AssessmentTemplate.Defaults.id,
    collectionId: Option[String] = AssessmentTemplate.Defaults.collectionId,
    orgId: Option[ObjectId] = AssessmentTemplate.Defaults.orgId,
    metadata: Map[String, String] = AssessmentTemplate.Defaults.metadata,
    dateModified: Option[DateTime] = AssessmentTemplate.Defaults.dateModified,
    questions: Seq[Question] = AssessmentTemplate.Defaults.questions,
    contentType: String = AssessmentTemplate.contentType) {

  def merge(that: AssessmentTemplate) = this.copy(
    collectionId = if (that.collectionId.nonEmpty) that.collectionId else this.collectionId,
    orgId = if (that.orgId.nonEmpty) that.orgId else this.orgId,
    metadata = if (that.metadata.nonEmpty) that.metadata else this.metadata,
    dateModified = if (that.dateModified.nonEmpty) that.dateModified else this.dateModified,
    questions = if (that.questions.nonEmpty) that.questions else this.questions)

}

object AssessmentTemplate {

  val contentType = "assessmentTemplate"
  protected val filename = "template.json"
  protected val nameOfFile = "template"

  object Defaults {
    def id = new ObjectId()

    val collectionId: Option[String] = None
    val orgId = None
    val metadata = Map.empty[String, String]

    def dateModified = Some(new DateTime())

    val questions = Seq.empty[Question]
  }

  object Keys {
    val contentType = "contentType"
    val orgId = "orgId"
    val collectionId = "collectionId"
    val metadata = "metadata"
    val questions = "questions"
    val id = "id"
    val data = "data"
    val files = "files"
  }
}
