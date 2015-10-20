package org.corespring.platform.core.models.assessment

import org.corespring.models.assessment.basic.Question
import org.corespring.models.JsonUtil
import play.api.libs.json._
import play.api.libs.json.JsSuccess
import org.bson.types.ObjectId
import com.mongodb.casbah.commons.TypeImports.ObjectId
import org.corespring.models.item.Content
import org.joda.time.DateTime
import org.corespring.models.item.resource.{ Resource, BaseFile, VirtualFile }
import org.corespring.models.item.json.ContentView

case class AssessmentTemplate(var id: ObjectId = AssessmentTemplate.Defaults.id,
  var collectionId: Option[String] = AssessmentTemplate.Defaults.collectionId,
  var orgId: Option[ObjectId] = AssessmentTemplate.Defaults.orgId,
  metadata: Map[String, String] = AssessmentTemplate.Defaults.metadata,
  dateModified: Option[DateTime] = AssessmentTemplate.Defaults.dateModified,
  questions: Seq[Question] = AssessmentTemplate.Defaults.questions) extends Content[ObjectId] {

  import AssessmentTemplate._

  var contentType = AssessmentTemplate.contentType

  /**
   * Represent the AssessmentTemplate as a Resource
   */
  def resource: Resource = Resource(name = nameOfFile, files = Seq(this.file))

  private def file: BaseFile = VirtualFile(
    name = filename,
    contentType = BaseFile.ContentTypes.JSON,
    isMain = true,
    content = (Format.writes(this).asInstanceOf[JsObject] - Keys.id).toString)

  def forSalat = SalatAssessmentTemplate(
    id = id,
    contentType = contentType,
    collectionId = collectionId,
    orgId = orgId,
    dateModified = dateModified,
    data = Option(resource))

  def merge(that: AssessmentTemplate) = this.copy(
    collectionId = if (that.collectionId.nonEmpty) that.collectionId else this.collectionId,
    orgId = if (that.orgId.nonEmpty) that.orgId else this.orgId,
    metadata = if (that.metadata.nonEmpty) that.metadata else this.metadata,
    dateModified = if (that.dateModified.nonEmpty) that.dateModified else this.dateModified,
    questions = if (that.questions.nonEmpty) that.questions else this.questions)

}

case class SalatAssessmentTemplate(var id: ObjectId,
  var contentType: String = "",
  var collectionId: Option[String] = None,
  orgId: Option[ObjectId] = None,
  dateModified: Option[DateTime] = None,
  data: Option[Resource] = None) extends Content[ObjectId] {

  implicit val AssessmentTemplateFormat = AssessmentTemplate.Format

  private def json: JsObject = data match {
    case Some(resource) => resource.files.find(_.isMain) match {
      case Some(jsonFile: VirtualFile) => Json.parse(jsonFile.content).as[JsObject]
      case _ => Json.obj()
    }
    case _ => Json.obj()
  }

  def toAssessmentTemplate: AssessmentTemplate = Json.fromJson[AssessmentTemplate](json) match {
    case success: JsSuccess[AssessmentTemplate] => success.get.copy(id = id)
    case _ => throw new IllegalArgumentException(s"Could not deserialize JSON to AssessmentTemplate:\n$json")
  }

}

object AssessmentTemplate extends JsonUtil {

  val contentType = "assessmentTemplate"
  protected val filename = "template.json"
  protected val nameOfFile = "template"

  object Defaults {
    def id = new ObjectId()
    val collectionId = None
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

  object Format extends Format[AssessmentTemplate] {

    import Keys._
    implicit val QuestionFormat = Question.Format

    def reads(json: JsValue): JsResult[AssessmentTemplate] = {
      JsSuccess(
        AssessmentTemplate(
          id = (json \ id).asOpt[String].map(new ObjectId(_)).getOrElse(new ObjectId()),
          collectionId = (json \ collectionId).asOpt[String],
          orgId = (json \ orgId).asOpt[String].map(new ObjectId(_)),
          metadata = (json \ metadata).asOpt[Map[String, String]].getOrElse(Map.empty),
          questions = (json \ questions).asOpt[Seq[Question]].getOrElse(Seq.empty)))
    }

    def writes(assessmentTemplate: AssessmentTemplate): JsValue = partialObj(
      id -> Some(JsString(assessmentTemplate.id.toString)),
      orgId -> assessmentTemplate.orgId.map(id => JsString(id.toString)),
      collectionId -> assessmentTemplate.collectionId.map(JsString(_)),
      metadata -> (assessmentTemplate.metadata match {
        case nonEmpty: Map[String, String] if nonEmpty.nonEmpty => Some(Json.toJson(nonEmpty))
        case _ => None
      }),
      questions -> (assessmentTemplate.questions match {
        case nonEmpty: Seq[Question] if nonEmpty.nonEmpty => Some(JsArray(nonEmpty.map(Json.toJson(_))))
        case _ => None
      }))
  }

  object ContentViewWrites extends Writes[ContentView[SalatAssessmentTemplate]] {
    implicit val AssessmentTemplateFormat = AssessmentTemplate.Format

    def writes(contentView: ContentView[SalatAssessmentTemplate]) =
      Json.toJson(contentView.content.toAssessmentTemplate)
  }

}