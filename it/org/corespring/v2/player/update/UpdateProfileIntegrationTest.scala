package org.corespring.v2.player.update

import global.Global
import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.SecureSocialHelper
import org.corespring.it.scopes.{ SessionRequestBuilder, userAndItem }
import org.corespring.models.item._
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.specification.Scope
import play.api.libs.json.Json._
import play.api.libs.json.{ JsArray, JsString }
import play.api.mvc.{ AnyContentAsJson, Call, Request }

abstract class UpdateProfileIntegrationTest extends IntegrationSpecification {

  def getUpdateProfileCall(itemId: VersionedId[ObjectId], username: String): Call

  def getUpdatedItem(itemId: VersionedId[ObjectId], username: String, orgId: ObjectId): Item

  def initData(itemId: VersionedId[ObjectId], username: String, orgId: ObjectId): Unit

  trait scope extends Scope with userAndItem with SessionRequestBuilder with SecureSocialHelper {

    case class Id(id: String)
    case class Name(name: String)

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
      val additionalCopyrights = Seq(Name("ed"))
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
    val priorUseOther = "Other"
    val priorGradeLevels = Seq("03", "04")
    val reviewsPassed = Seq("Bias")
    val reviewsPassedOther = "Other"
    val standards = Seq("RL.2.6")
    def standardsJson = JsArray(standards.map(dn => obj("dotNotation" -> dn)))

    val profileJson = obj(
      "contributorDetails" -> obj(
        "additionalCopyrights" -> JsArray(Seq(obj("author" -> contributorDetails.additionalCopyrights(0).name))),
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
      "priorUseOther" -> priorUseOther,
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
        "qaReview" -> true))

    def sk(s: String) = StringKeyValue(s, s)

    val fieldValue: FieldValue = FieldValue(
      bloomsTaxonomy = Seq(sk("Analyzing")),
      gradeLevels = Seq(sk("03"), sk("04")),
      credentials = Seq(sk(contributorDetails.credentials)),
      keySkills = Seq(ListKeyValue("dummy", Seq("Define", "Discuss", "Distinguish", "Choose", "Analyze", "Examine"))))

    val id = Global.main.fieldValueService.insert(fieldValue).toOption.get
    initData(itemId, user.userName, orgId)
    val call = getUpdateProfileCall(itemId, user.userName)
    val request: Request[AnyContentAsJson] = makeJsonRequest(call, profileJson)
    logger.debug(s"request: $request")
    logger.debug(s"body: ${request.body.json}")
    val result = route(request)(writeableOf_AnyContentAsJson).get
    logger.debug(s"result: ${contentAsString(result)}")
    lazy val updatedItem = getUpdatedItem(itemId, user.userName, orgId)

    override def after = {
      super.after
      Global.main.fieldValueService.delete(id)
    }
  }

  "update profile" should {

    "save taskInfo correctly to the backend" in new scope {
      status(result) must_== OK
      val ti = updatedItem.taskInfo.get
      ti.title must_== Some(taskInfo.title)
      ti.description must_== Some(taskInfo.description)
      ti.gradeLevel must_== taskInfo.gradeLevel
      ti.description must_== Some(taskInfo.description)
      ti.title must_== Some(taskInfo.title)
      ti.subjects.get.primary must_== Some(taskInfo.subjects.primary.id)
      ti.subjects.get.related must_== taskInfo.subjects.related.map(i => new ObjectId(i.id))
    }

    "save workflow correctly to the backend" in new scope {
      status(result) must_== OK
      val w = updatedItem.workflow.get
      w.setup must_== true
      w.tagged must_== true
      w.standardsAligned must_== true
      w.qaReview must_== true
    }

    "save contributorDetails to the backend" in new scope {
      status(result) must_== OK
      val cd = updatedItem.contributorDetails.get
      cd.author must_== Some(contributorDetails.author)
      cd.contributor must_== Some(contributorDetails.contributor)
      val ic = cd.copyright.get
      ic.owner must_== Some(contributorDetails.copyright.owner)
      ic.year must_== Some(contributorDetails.copyright.year)
      cd.credentials must_== Some(contributorDetails.credentials)
      cd.licenseType must_== Some(contributorDetails.licenseType)
      cd.sourceUrl must_== Some(contributorDetails.sourceUrl)
      cd.additionalCopyrights(0).author must_== Some(contributorDetails.additionalCopyrights(0).name)
    }

    "save otherAlignments to the backend" in new scope {
      status(result) must_== OK
      val oa = updatedItem.otherAlignments.get
      oa.bloomsTaxonomy must_== Some(otherAlignments.bloomsTaxonomy)
      oa.keySkills must_== otherAlignments.keySkills
      oa.relatedCurriculum must_== Some(otherAlignments.relatedCurriculum)
      oa.depthOfKnowledge must_== Some(otherAlignments.depthOfKnowledge)
    }

    "save remaining profile data to the backend" in new scope {
      status(result) must_== OK
      updatedItem.priorUse must_== Some(priorUse)
      updatedItem.priorUseOther must_== Some(priorUseOther)
      updatedItem.priorGradeLevels must_== priorGradeLevels
      updatedItem.reviewsPassed must_== reviewsPassed
      updatedItem.reviewsPassedOther must_== Some(reviewsPassedOther)
      updatedItem.standards must_== standards
    }
  }
}

