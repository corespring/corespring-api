package org.corespring.platform.core.models.item

import com.mongodb.BasicDBObject
import org.bson.types.ObjectId
import org.corespring.platform.core
import org.corespring.platform.core.models.Subject
import org.corespring.platform.core.models.item.Item.Keys
import org.corespring.platform.core.models.json.JsonValidationException
import org.corespring.test.BaseTest
import play.api.libs.json.{ JsArray, JsObject, JsString, Json }
import scala.Some

class ItemTest extends BaseTest {

  "parse" should {

    "work" in {
      val item = Item(
        otherAlignments = Some(
          Alignments(
            demonstratedKnowledge = Some("Factual"),
            bloomsTaxonomy = Some("Applying"))))

      val json = Json.toJson(item)

      import Alignments.Keys
      (json \ Keys.demonstratedKnowledge).asOpt[String] === Some("Factual")
      (json \ Keys.bloomsTaxonomy).asOpt[String] === Some("Applying")

      val parsed = json.as[Item]

      parsed.otherAlignments.get.demonstratedKnowledge must equalTo(Some("Factual"))
      parsed.otherAlignments.get.bloomsTaxonomy must equalTo(Some("Applying"))
    }

    "parse player definition" in {
      val json = Json.obj(
        "playerDefinition" -> Json.obj(
          "files" -> JsArray(Seq()),
          "xhtml" -> "<div/>",
          "components" -> Json.obj("3" -> Json.obj("componentType" -> "type"))))
      val item = json.as[Item]
      item.playerDefinition.isDefined === true
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
    "parse standards" in {
      val item = Item(standards = Seq("RL.K.9"))
      val json = Json.toJson(item)
      /*
      * Our Js client only sends the dot notation back to the server when saving so mimic that.
      */
      val string = """{ "standards" : ["RL.K.9"] }"""
      val parsed = Json.parse(string).as[Item]
      parsed.standards must equalTo(item.standards)
    }

    "parse priorGradeLevel" in {
      val item = Item(priorGradeLevel = Seq("03", "04"))
      val json = Json.toJson(item)
      val parsedItem = json.as[Item]
      parsedItem.priorGradeLevel must equalTo(item.priorGradeLevel)
    }

    "not parse invalid priorGradeLevel" in {
      val item = Item(priorGradeLevel = Seq("apple", "pear"))
      val json = Json.toJson(item)
      json.as[Item] must throwA[JsonValidationException]
    }

    "parse subjects with only primary" in {
      val dbSubject = Subject.findOne(new BasicDBObject())
      val subject = Subjects(primary = Some(dbSubject.get.id))

      //The json that is submittted to be read is different from the db json
      val jsonToRead = JsObject(
        Seq(
          Keys.id -> JsString(new ObjectId().toString),
          Keys.primarySubject -> JsString(subject.primary.get.toString)))
      val parsed = jsonToRead.as[Item]

      parsed.taskInfo.get.subjects.get.primary must equalTo(subject.primary)
    }

    "parse subjects with primary and related" in {
      val dbSubject = Subject.findOne(new BasicDBObject())
      val subject = core.models.item.Subjects(primary = Some(dbSubject.get.id), related = Some(dbSubject.get.id))

      //The json that is submittted to be read is different from the db json
      val jsonToRead = JsObject(
        Seq(
          Keys.id -> JsString(new ObjectId().toString),
          Keys.primarySubject -> JsString(subject.primary.get.toString),
          Keys.relatedSubject -> JsString(subject.related.get.toString)))

      val parsed = jsonToRead.as[Item]

      parsed.taskInfo.get.subjects.get.primary must equalTo(subject.primary)
      parsed.taskInfo.get.subjects.get.related must equalTo(subject.related)
    }

    "parse contributor details" in {
      val copyright = Copyright(Some("Ed"), Some("2001"), Some("3000"), Some("imageName.png"))
      val contributorDetails = ContributorDetails(
        copyright = Some(copyright),
        costForResource = Some(10),
        author = Some("Ed"))
      val item = Item(contributorDetails = Some(contributorDetails))
      val json = Json.toJson(item)
      (json \ Keys.copyrightOwner).asOpt[String] must equalTo(Some("Ed"))
      (json \ Keys.copyrightYear).asOpt[String] must equalTo(Some("2001"))
      (json \ Keys.copyrightExpirationDate).asOpt[String] must equalTo(Some("3000"))
      (json \ Keys.copyrightImageName).asOpt[String] must equalTo(Some("imageName.png"))
      (json \ Keys.costForResource).asOpt[Int] must equalTo(Some(10))
      (json \ Keys.author).asOpt[String] must equalTo(Some("Ed"))
      (json \ Keys.licenseType).asOpt[String] must beNone
      (json \ Keys.sourceUrl).asOpt[String] must beNone

      val parsedItem = json.as[Item]
      parsedItem.contributorDetails.get.copyright.get.owner must equalTo(Some("Ed"))
      parsedItem.contributorDetails.get.copyright.get.year must equalTo(Some("2001"))
      parsedItem.contributorDetails.get.copyright.get.expirationDate must equalTo(Some("3000"))
      parsedItem.contributorDetails.get.costForResource must equalTo(Some(10))
      parsedItem.contributorDetails.get.author must equalTo(Some("Ed"))
      parsedItem.contributorDetails.get.licenseType must beNone
      parsedItem.contributorDetails.get.sourceUrl must beNone
    }
  }
  "item properties are validated when saving" in {
    //TODO: Item.updateItem(item.id, item, None).isLeft must equalTo(true)
    pending
  }
  "clone" should {

    "work" in {

      val item = Item(collectionId = "1234567")
      val clonedItem = itemService.cloneItem(item)
      item.collectionId === clonedItem.get.collectionId

      item.collectionId = "0987654321"
      itemService.save(item)

      itemService.findOneById(clonedItem.get.id) match {
        case Some(fromDb) => clonedItem.get.collectionId === fromDb.collectionId
        case _ => failure("couldn't find cloned item")
      }
    }

    "prepend [copy] to title" in {
      val item = Item(collectionId = "1234567", taskInfo = Some(TaskInfo(title = Some("something"))))
      val clonedItem = itemService.cloneItem(item)
      clonedItem.get.taskInfo.get.title.get === "[copy] " + item.taskInfo.get.title.get
    }

    "prepend [copy] to empty taskinfo" in {
      val item = Item(collectionId = "1234567")
      val clonedItem = itemService.cloneItem(item)
      clonedItem.get.taskInfo.get.title.get === "[copy]"
    }

    "prepend [copy] to empty title" in {
      val item = Item(collectionId = "1234567", taskInfo = Some(TaskInfo()))
      val clonedItem = itemService.cloneItem(item)
      clonedItem.get.taskInfo.get.title.get === "[copy]"
    }

    /**/
  }

}
