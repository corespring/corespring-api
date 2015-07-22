package org.corespring.models.json

import org.bson.types.ObjectId
import org.corespring.models.assessment.{ AssessmentTemplate, Answer, Assessment }
import org.corespring.models.json.assessment.{ AssessmentTemplateFormat, AnswerFormat, AssessmentFormat }
import org.corespring.models.{ ContentCollection, Standard, Subject }
import org.corespring.models.item._
import org.corespring.models.item.resource.Resource
import org.corespring.models.json.item._
import org.corespring.models.json.item.resource.ResourceFormat
import play.api.libs.json.{ Writes, Json, Format }

/**
 * Usage:
 * val formatting = new JsonFormatting(???)
 * implicit val formats : Format[TaskInfo] = formatting.info
 * Ok(toJson(info))
 */
trait JsonFormatting {

  def fieldValue: FieldValue
  def findSubjectById: ObjectId => Option[Subject]
  def findStandardByDotNotation: String => Option[Standard]

  implicit lazy val writesFieldValue: Writes[FieldValue] = FieldValueWrites

  implicit lazy val formatAssessmentTemplate: Format[AssessmentTemplate] = AssessmentTemplateFormat

  implicit lazy val formatAssessment: Format[Assessment] = AssessmentFormat
  implicit lazy val formatAnswer: Format[Answer] = AnswerFormat

  implicit lazy val formatWorkflow: Format[Workflow] = Json.format[Workflow]

  implicit lazy val formatCopyright: Format[Copyright] = new CopyrightFormat {
    override def fieldValues: FieldValue = JsonFormatting.this.fieldValue
  }

  implicit lazy val formatPlayerDefinition: Format[PlayerDefinition] = PlayerDefinitionFormat

  implicit lazy val writeContentCollection: Writes[ContentCollection] = ContentCollectionWrites

  implicit lazy val formatResource: Format[Resource] = ResourceFormat

  implicit lazy val formatStandard: Format[Standard] = StandardFormat

  implicit lazy val formatContributorDetails: Format[ContributorDetails] = new ContributorDetailsFormat {
    override implicit def ac: Format[AdditionalCopyright] = JsonFormatting.this.formatAdditionalCopyright

    override implicit def c: Format[Copyright] = JsonFormatting.this.formatCopyright
  }

  implicit lazy val writeSubject: Writes[Subject] = SubjectWrites

  implicit lazy val formatSubjects: Format[Subjects] = new SubjectsFormat {
    override def findOneById(id: ObjectId): Option[Subject] = findSubjectById(id)

    override implicit def sf: Writes[Subject] = JsonFormatting.this.writeSubject

    override def fieldValues: FieldValue = JsonFormatting.this.fieldValue
  }

  implicit lazy val formatTaskInfo: Format[TaskInfo] = new TaskInfoFormat {

    override implicit def sf: Format[Subjects] = JsonFormatting.this.formatSubjects

    override def fieldValues: FieldValue = JsonFormatting.this.fieldValue
  }

  implicit lazy val formatAlignments: Format[Alignments] = new AlignmentsFormat {
    override def fieldValues: FieldValue = JsonFormatting.this.fieldValue
  }

  implicit lazy val formatAdditionalCopyright: Format[AdditionalCopyright] = new AdditionalCopyrightFormat {
    override def fieldValues: FieldValue = JsonFormatting.this.fieldValue
  }

  implicit lazy val item: Format[Item] = new ItemFormat {
    override implicit def ac: Format[AdditionalCopyright] = JsonFormatting.this.formatAdditionalCopyright

    override implicit def cd: Format[ContributorDetails] = JsonFormatting.this.formatContributorDetails

    override implicit def wf: Format[Workflow] = JsonFormatting.this.formatWorkflow

    override implicit def ti: Format[TaskInfo] = JsonFormatting.this.formatTaskInfo

    override implicit def pd: Format[PlayerDefinition] = JsonFormatting.this.formatPlayerDefinition

    override implicit def a: Format[Alignments] = JsonFormatting.this.formatAlignments

    override implicit def res: Format[Resource] = JsonFormatting.this.formatResource

    override implicit def st: Format[Standard] = JsonFormatting.this.formatStandard

    override def findOneByDotNotation(dotNotation: String): Option[Standard] = findStandardByDotNotation(dotNotation)

    override def fieldValues: FieldValue = JsonFormatting.this.fieldValue
  }
}
