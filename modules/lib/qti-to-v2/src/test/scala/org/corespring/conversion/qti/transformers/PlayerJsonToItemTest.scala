package org.corespring.conversion.qti.transformers

import org.bson.types.ObjectId
import org.corespring.models.item._
import org.corespring.models.json.JsonFormatting
import org.corespring.models.{ Standard, Subject }
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.mutable.Specification
import play.api.libs.json.Json._
import play.api.libs.json.{ JsArray, JsObject, JsString }

class PlayerJsonToItemTest extends Specification {

  val collectionId = "543edd2fa399191672bedea9"
  val itemId = new ObjectId()
  val item = Item(collectionId = ObjectId.get.toString, id = VersionedId(itemId, Some(0)))

  val jsonFormatting = new JsonFormatting {
    override def findStandardByDotNotation: (String) => Option[Standard] = _ => None

    override def rootOrgId: ObjectId = ObjectId.get

    def sk(s: String) = StringKeyValue(s, s)

    override def fieldValue: FieldValue = FieldValue(
      bloomsTaxonomy = Seq(sk("Analyzing")),
      gradeLevels = Seq(sk("03"), sk("04")),
      keySkills = Seq(ListKeyValue("dummy", Seq("Define", "Discuss", "Distinguish", "Choose", "Analyze", "Examine"))))

    override def findSubjectById: (ObjectId) => Option[Subject] = _ => None
  }

  val playerJsonToItem = new PlayerJsonToItem(jsonFormatting)

  object contributorDetails {
    val author = "State of New Jersey Department of Education"
    val contributor = "State of New Jersey Department of Education"

    object copyright {
      val owner = "State of New Jersey Department of Education"
      val year = "2012"
      val expirationDate = "2015"
      val imageName = "image.png"
    }

    val credentials = "State Department of Education"
    val licenseType = "CC BY"
    val sourceUrl = "http://www.state.nj.us/education/modelcurriculum/ela/10u1.shtml"
  }

  object otherAlignments {
    val bloomsTaxonomy = "Analyzing"
    val depthOfKnowledge = "3"
    val keySkills = Seq("Define", "Discuss", "Distinguish", "Choose", "Analyze", "Examine")
    val relatedCurriculum = "New Jersey Model Curriculum"
  }

  val lexile = "24"
  val pValue = "0"
  val priorUse = "Summative"
  val priorGradeLevels = Seq("03", "04")
  val reviewsPassed = Seq("Bias")
  val reviewsPassedOther = "Other"
  val standards = Seq("RL.2.6")
  def standardsJson = JsArray(standards.map(dn => obj("dotNotation" -> dn)))
  object supportingMaterial {
    val name = "Rubric"
    val files = Seq("files/rubric.pdf")
  }

  case class Id(id: String)

  object taskInfo {
    val gradeLevel = Seq("03")
    val title = "Read the passage <i>What's the Scoop on Soil?</i>. Which aspects of soil does the passage not explain? What can you tell about soil by squeezing it?"
    val description = "Item about Soil"
    object subjects {
      object primary {
        val id = ObjectId.get.toString
      }
      val related = Seq(Id(ObjectId.get.toString))
    }
  }

  object playerDefinition {
    val xhtml = "<div></div>"
    val components = obj(
      "1" -> obj(
        "componentType" -> "comp-type"))
    val customScoring = "customScoring"
    val summaryFeedback = "summaryFeedback"
    val files = Seq("dot array.png")
  }
  val itemJson: JsObject = obj(
    "components" -> playerDefinition.components,
    "files" -> playerDefinition.files,
    "xhtml" -> playerDefinition.xhtml,
    "customScoring" -> playerDefinition.customScoring,
    "summaryFeedback" -> playerDefinition.summaryFeedback,
    "profile" -> obj(
      "contributorDetails" -> obj(
        "additionalCopyright" -> arr(obj("owner" -> "ed")),
        "author" -> contributorDetails.author,
        "contributor" -> contributorDetails.contributor,
        "copyrightOwner" -> contributorDetails.copyright.owner,
        "copyrightYear" -> contributorDetails.copyright.year,
        "copyrightImageName" -> contributorDetails.copyright.imageName,
        "credentials" -> contributorDetails.credentials,
        "licenseType" -> contributorDetails.licenseType,
        "sourceUrl" -> contributorDetails.sourceUrl),
      "lexile" -> lexile,
      "otherAlignments" -> obj(
        "bloomsTaxonomy" -> otherAlignments.bloomsTaxonomy,
        "keySkills" -> JsArray(otherAlignments.keySkills.map(JsString(_))),
        "relatedCurriculum" -> otherAlignments.relatedCurriculum,
        "depthOfKnowledge" -> otherAlignments.depthOfKnowledge),
      "pValue" -> pValue,
      "priorUse" -> priorUse,
      "priorGradeLevel" -> JsArray(priorGradeLevels.map(JsString(_))),
      "reviewsPassed" -> JsArray(reviewsPassed.map(JsString(_))),
      "reviewsPassedOther" -> reviewsPassedOther,
      "standards" -> standardsJson,
      "taskInfo" -> obj(
        "gradeLevel" -> JsArray(taskInfo.gradeLevel.map(JsString(_))),
        "title" -> taskInfo.title,
        "description" -> taskInfo.description,
        "subjects" -> obj(
          "primary" -> obj(
            "id" -> taskInfo.subjects.primary.id),
          "related" -> arr(obj("id" -> taskInfo.subjects.related(0).id)))),
      "workflow" -> obj(
        "setup" -> true,
        "tagged" -> true,
        "standardsAligned" -> true,
        "qaReview" -> true)),
    "supportingMaterials" -> arr(
      obj(
        "name" -> supportingMaterial.name,
        "files" -> arr(obj("name" -> supportingMaterial.files(0))))))

  "profile" should {

    val profile = playerJsonToItem.profile(item, itemJson \ "profile")

    "contributorDetails" should {
      val cd = profile.contributorDetails.get

      "have correct author" in { cd.author must_== Some(contributorDetails.author) }
      "have correct contributor" in { cd.contributor must_== Some(contributorDetails.contributor) }

      "copyright" should {
        val ic = cd.copyright.get
        "have correct owner" in { ic.owner must_== Some(contributorDetails.copyright.owner) }
        "have correct year" in { ic.year must_== Some(contributorDetails.copyright.year) }
      }

      "have correct credentials" in { cd.credentials must_== Some(contributorDetails.credentials) }
      "have correct licenseType" in { cd.licenseType must_== Some(contributorDetails.licenseType) }
      "have correct sourceUrl" in { cd.sourceUrl must_== Some(contributorDetails.sourceUrl) }

    }

    "otherAlignments" should {
      val oa = profile.otherAlignments.get
      "have correct bloomsTaxonomy" in { oa.bloomsTaxonomy must_== Some(otherAlignments.bloomsTaxonomy) }
      "have correct keySkills" in { oa.keySkills must_== otherAlignments.keySkills }
      "have correct relatedCurriculum" in { oa.relatedCurriculum must_== Some(otherAlignments.relatedCurriculum) }
      "have correct depthOfKnowledge" in { oa.depthOfKnowledge must_== Some(otherAlignments.depthOfKnowledge) }
    }

    "taskInfo" should {
      val ti = profile.taskInfo.get
      "have correct gradeLevel" in { ti.gradeLevel must_== taskInfo.gradeLevel }
      "have correct description" in { ti.description must_== Some(taskInfo.description) }
      "have correct title" in { ti.title must_== Some(taskInfo.title) }
      "have correct primary subject" in { ti.subjects.get.primary must_== Some(taskInfo.subjects.primary.id) }
      "have correct related subjects" in { ti.subjects.get.related must_== taskInfo.subjects.related.map(i => new ObjectId(i.id)) }
    }

    "workflow" should {
      val w = profile.workflow.get
      "have correct setup" in { w.setup must_== true }
      "have correct tagged" in { w.tagged must_== true }
      "have correct standardsAligned" in { w.standardsAligned must_== true }
      "have correct qaReview" in { w.qaReview must_== true }
    }
    "have correct priorUse" in { profile.priorUse must_== Some(priorUse) }
    "have correct priorGradeLevels" in { profile.priorGradeLevels must_== priorGradeLevels }
    "have correct reviewsPassed" in { profile.reviewsPassed must_== reviewsPassed }
    "have correct reviewsPassedOther" in { profile.reviewsPassedOther must_== Some(reviewsPassedOther) }
    "have correct standards" in { profile.standards must_== standards }
  }

  "playerDefinition" should {
    val conversion = playerJsonToItem.playerDef(item, itemJson)
    val pd = conversion.playerDefinition.get
    "have correct xhtml" in { pd.xhtml must_== playerDefinition.xhtml }
    "have correct summaryFeedback" in { pd.summaryFeedback must_== playerDefinition.summaryFeedback }
    "have correct components" in { pd.components must_== playerDefinition.components }
    "have correct customScoring" in { pd.customScoring must_== Some(playerDefinition.customScoring) }
  }
}
