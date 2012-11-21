package tests.models

import tests.BaseTest
import models._
import play.api.libs.json.{JsString, JsObject, Json}
import org.bson.types.ObjectId
import com.mongodb.{DBObject, BasicDBObject}
import models.Copyright
import models.ContributorDetails
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import scala.Some
import controllers.JsonValidationException

class ItemTest extends BaseTest {

  "item" should {

    "general parse" in {
      val item = Item(demonstratedKnowledge = Some("Factual"),
        bloomsTaxonomy = Some("Apply")
      )

      val json = Json.toJson(item)

      (json \ Item.demonstratedKnowledge).asOpt[String] must equalTo(Some("Factual"))
      (json \ Item.bloomsTaxonomy).asOpt[String] must equalTo(Some("Apply"))

      val parsed = json.as[Item]

      parsed.demonstratedKnowledge must equalTo(Some("Factual"))
      parsed.bloomsTaxonomy must equalTo(Some("Apply"))
    }

    "parse workflow" in {
      val workflow = Workflow(setup = true,
        tagged = true,
        qaReview = true,
        standardsAligned = true)

      val item = Item(workflow = Some(workflow))

      val jsonItem = Json.toJson(item)

      (jsonItem \ "workflow" \ Workflow.setup).as[Boolean] must equalTo(true)
      (jsonItem \ "workflow" \ Workflow.tagged).as[Boolean] must equalTo(true)
      (jsonItem \ "workflow" \ Workflow.standardsAligned).as[Boolean] must equalTo(true)
      (jsonItem \ "workflow" \ Workflow.qaReview).as[Boolean] must equalTo(true)

      val itemFromJson = jsonItem.as[Item]

      itemFromJson.workflow.get.setup must equalTo(true)
      itemFromJson.workflow.get.tagged must equalTo(true)
      itemFromJson.workflow.get.standardsAligned must equalTo(true)
      itemFromJson.workflow.get.qaReview must equalTo(true)
    }

    "parse itemType" in {

      val item = Item(itemType = Some("itemType"))
      val json = Json.toJson(item)
      val parsedItem = json.as[Item]
      parsedItem.itemType must equalTo(item.itemType)
    }

    "parses gradeLevel" in {
      val item = Item(gradeLevel = Seq("03","04"))
      val json = Json.toJson(item)
      val parsedItem = json.as[Item]
      parsedItem.gradeLevel must equalTo(item.gradeLevel)
    }

    "does not parse invalid gradeLevel" in {
      val item = Item(gradeLevel = Seq("apple","pear"))
      val json = Json.toJson(item)
      json.as[Item] must throwA[JsonValidationException]
    }

    "parses priorGradeLevel" in {
      val item = Item(priorGradeLevel = Seq("03","04"))
      val json = Json.toJson(item)
      val parsedItem = json.as[Item]
      parsedItem.priorGradeLevel must equalTo(item.priorGradeLevel)
    }

    "does not parse invalid priorGradeLevel" in {
      val item = Item(priorGradeLevel = Seq("apple","pear"))
      val json = Json.toJson(item)
      json.as[Item] must throwA[JsonValidationException]
    }

    "parse subjects with only primary" in {
      val dbSubject = Subject.findOne(new BasicDBObject())
      val subject = models.Subjects(primary = Some(dbSubject.get.id))
      val item = Item(subjects = Some(subject))

      //The json that is submittted to be read is different from the db json
      val jsonToRead = JsObject(
        Seq(
          (Item.id -> JsString((item.id.toString))),
          (Item.primarySubject -> JsString(item.subjects.get.primary.get.toString))
        ))
      val parsed = jsonToRead.as[Item]

      parsed.subjects.get.primary must equalTo(item.subjects.get.primary)
    }

    "parse subjects with primary and related" in {
      val dbSubject = Subject.findOne(new BasicDBObject())
      val subject = models.Subjects(primary = Some(dbSubject.get.id), related = Some(dbSubject.get.id))
      val item = Item(subjects = Some(subject))

      //The json that is submittted to be read is different from the db json
      val jsonToRead = JsObject(
        Seq(
          (Item.id -> JsString((item.id.toString))),
          (Item.primarySubject -> JsString(item.subjects.get.primary.get.toString)),
          (Item.relatedSubject -> JsString(item.subjects.get.related.get.toString))
        ))

      val parsed = jsonToRead.as[Item]

      parsed.subjects.get.primary must equalTo(item.subjects.get.primary)
      parsed.subjects.get.related must equalTo(item.subjects.get.related)
    }

    "item properties are validated when saving" in {

      val item = Item(demonstratedKnowledge = Some("illegalstring value"))
      Item.save(item)
      //TODO: Item.updateItem(item.id, item, None).isLeft must equalTo(true)
      pending
    }

    "parse contributor details" in {
      val copyright = Copyright(Some("Ed"), Some("2001"), Some("3000"), Some("imageName.png"))
      val contributorDetails = ContributorDetails(
        copyright = Some(copyright),
        costForResource = Some(10),
        author = Some("Ed")
      )
      val item = Item(contributorDetails = Some(contributorDetails))
      val json = Json.toJson(item)
      (json \ Item.copyrightOwner).asOpt[String] must equalTo(Some("Ed"))
      (json \ Item.copyrightYear).asOpt[String] must equalTo(Some("2001"))
      (json \ Item.copyrightExpirationDate).asOpt[String] must equalTo(Some("3000"))
      (json \ Item.copyrightImageName).asOpt[String] must equalTo(Some("imageName.png"))
      (json \ Item.costForResource).asOpt[Int] must equalTo(Some(10))
      (json \ Item.author).asOpt[String] must equalTo(Some("Ed"))
      (json \ Item.licenseType).asOpt[String] must beNone
      (json \ Item.sourceUrl).asOpt[String] must beNone

      val parsedItem = json.as[Item]
      parsedItem.contributorDetails.get.copyright.get.owner must equalTo(Some("Ed"))
      parsedItem.contributorDetails.get.copyright.get.year must equalTo(Some("2001"))
      parsedItem.contributorDetails.get.copyright.get.expirationDate must equalTo(Some("3000"))
      parsedItem.contributorDetails.get.costForResource must equalTo(Some(10))
      parsedItem.contributorDetails.get.author must equalTo(Some("Ed"))
      parsedItem.contributorDetails.get.licenseType must beNone
      parsedItem.contributorDetails.get.sourceUrl must beNone
    }

    "clone" in {

      val item = Item(collectionId="1234567")
      val clonedItem = Item.cloneItem(item)
      item.collectionId === clonedItem.get.collectionId

      item.collectionId = "0987654321"
      Item.save(item)

      Item.findOneById(clonedItem.get.id) match {
        case Some(fromDb) => clonedItem.get.collectionId === fromDb.collectionId
        case _ => failure("couldn't find cloned item")
      }
    }

    "list works" in {

      val result  = Item.list()

      (result.length > 0) === true
    }

    "list accepts fields" in {
      true === true
    }

  }

}
