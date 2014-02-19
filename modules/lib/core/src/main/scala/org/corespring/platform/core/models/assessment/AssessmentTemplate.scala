package org.corespring.platform.core.models.assessment

import org.corespring.platform.core.models.assessment.basic.Question
import org.corespring.platform.core.models.JsonUtil
import play.api.libs.json._
import play.api.libs.json.JsSuccess
import org.corespring.platform.core.models.versioning.VersionedIdImplicits
import org.corespring.platform.data.mongo.models.{EntityWithVersionedId, VersionedId}
import org.bson.types.ObjectId
import com.mongodb.casbah.commons.TypeImports.ObjectId
import org.corespring.platform.core.models.item.Content
import org.joda.time.DateTime
import org.corespring.platform.core.models.json.JsonValidationException
import org.corespring.platform.core.models.item.resource.{Resource, BaseFile, VirtualFile}

case class AssessmentTemplate(var id: VersionedId[ObjectId] = VersionedId(ObjectId.get()),
                              var collectionId: Option[String] = None,
                              orgId: Option[ObjectId] = None,
                              metadata: Map[String, String] = Map(),
                              dateModified: Option[DateTime] = Some(new DateTime()),
                              questions: Seq[Question] = Seq()) extends Content with EntityWithVersionedId[ObjectId] {

  import AssessmentTemplate._

  var contentType = "assessmentTemplate"

  /**
   * Represent the AssessmentTemplate as a Resource
   */
  def resource: Resource = Resource(name = nameOfFile, files = Seq(this.file))

  private def file: BaseFile = VirtualFile(
    name = filename,
    contentType = BaseFile.ContentTypes.JSON,
    isMain = true,
    content = (Format.writes(this).asInstanceOf[JsObject] - Keys.id).toString
  )

  def forSalat = SalatAssessmentTemplate(
    id = id,
    contentType = contentType,
    collectionId = collectionId,
    orgId = orgId,
    dateModified = dateModified,
    data = Option(resource)
  )

}

case class SalatAssessmentTemplate(var id: VersionedId[ObjectId],
                                 var contentType: String = "",
                                 var collectionId: Option[String] = None,
                                 orgId: Option[ObjectId] = None,
                                 dateModified: Option[DateTime] = None,
                                 data: Option[Resource] = None) extends Content with EntityWithVersionedId[ObjectId]

object AssessmentTemplate extends JsonUtil {

  protected val filename = "template.json"
  protected val nameOfFile = "template"

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
    implicit val VersionedIdWrites = VersionedIdImplicits.Writes
    implicit val VersionedIdReads = VersionedIdImplicits.Reads
    implicit val QuestionFormat = Question.Format

    def reads(json: JsValue): JsResult[AssessmentTemplate] = {
      JsSuccess(
        AssessmentTemplate(
          id = (try {
            import VersionedIdImplicits.{ Reads => IdReads }
            (json \ id).asOpt[VersionedId[ObjectId]](IdReads).getOrElse(VersionedId(new ObjectId()))
          } catch {
            case e: Throwable => throw new JsonValidationException(id)
          }),
          collectionId = (json \ collectionId).asOpt[String],
          orgId = (json \ orgId).asOpt[String].map(new ObjectId(_)),
          metadata = (json \ metadata).asOpt[Map[String, String]].getOrElse(Map.empty),
          questions = (json \ questions).asOpt[Seq[Question]].getOrElse(Seq.empty)
        )
      )
    }

    def writes(assessmentTemplate: AssessmentTemplate): JsValue = partialObj(
      id -> Some(JsString(assessmentTemplate.id.toString)),
      metadata -> (assessmentTemplate.metadata match {
        case nonEmpty: Map[String, String] if nonEmpty.nonEmpty => Some(Json.toJson(nonEmpty))
        case _ => None
      }),
      questions -> (assessmentTemplate.questions match {
        case nonEmpty: Seq[Question] if nonEmpty.nonEmpty => Some(JsArray(nonEmpty.map(Json.toJson(_))))
        case _ => None
      })
    )

  }

}