package org.corespring.platform.core.models.item

import com.mongodb.BasicDBObject
import org.bson.types.ObjectId
import org.corespring.platform.core
import org.corespring.models.Subject
import org.corespring.models.item.Item.Keys
import org.corespring.models.json.JsonValidationException
import org.corespring.test.BaseTest
import play.api.libs.json.{ JsArray, JsObject, JsString, Json }
import org.corespring.models.item.resource.{ BaseFile, VirtualFile, Resource }

class ItemTest extends BaseTest {

  "createdByApiVersion" should {

    val data = Some(Resource(
      name = "qti.xml",
      files = Seq(VirtualFile(
        name = "qti.xml", contentType = BaseFile.ContentTypes.XML, isMain = true, content = "<root/>"))))

    val playerDefinition = Some(PlayerDefinition(Seq.empty, "", Json.obj(), "", None))

    "identify v1 items" in {
      Item(data = data).createdByApiVersion === 1
    }

    "identify v2 items" in {
      val v2Item = Item(playerDefinition = playerDefinition)
      v2Item.createdByApiVersion === 2
    }

    "if has qti and player def identifies the item as 1" in {
      val hasQtiAndPlayerDef = Item(data = data, playerDefinition = playerDefinition)
      hasQtiAndPlayerDef.createdByApiVersion === 1
    }
  }

  "parse" should {

    "work" in {
      val item = Item(
        otherAlignments = Some(
          Alignments(
            depthOfKnowledge = Some("Recall & Reproduction"),
            bloomsTaxonomy = Some("Applying"))))

      val json = Json.toJson(item)

      import Alignments.Keys
      (json \ Keys.depthOfKnowledge).asOpt[String] === Some("Recall & Reproduction")
      (json \ Keys.bloomsTaxonomy).asOpt[String] === Some("Applying")

      val parsed = json.as[Item]

      parsed.otherAlignments.get.depthOfKnowledge must equalTo(Some("Recall & Reproduction"))
      parsed.otherAlignments.get.bloomsTaxonomy must equalTo(Some("Applying"))
    }

    "parse player definition" in {
      val json = Json.obj(
        "playerDefinition" -> Json.obj(
          "files" -> JsArray(Seq()),
          "xhtml" -> "<div/>",
          "components" -> Json.obj("3" -> Json.obj("componentType" -> "type")),
          "summaryFeedback" -> ""))
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
      val item = Item(priorGradeLevels = Seq("03", "04"))
      val json = Json.toJson(item)
      val parsedItem = json.as[Item]
      parsedItem.priorGradeLevels must equalTo(item.priorGradeLevels)
    }

    "not parse invalid priorGradeLevel" in {
      val item = Item(priorGradeLevels = Seq("apple", "pear"))
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
      val subject = core.models.item.Subjects(primary = Some(dbSubject.get.id), related = Seq(dbSubject.get.id))

      //The json that is submittted to be read is different from the db json
      val jsonToRead = JsObject(
        Seq(
          Keys.id -> JsString(new ObjectId().toString),
          Keys.primarySubject -> JsString(subject.primary.get.toString),
          Keys.relatedSubject -> JsArray(subject.related.map(s => JsString(s.toString)))))

      val parsed = jsonToRead.as[Item]

      parsed.taskInfo.get.subjects.get.primary must equalTo(subject.primary)
      parsed.taskInfo.get.subjects.get.related must equalTo(subject.related)
    }

    "parse contributor details" in {
      val additionalCopyright = AdditionalCopyright(Some("author"), Some("owner"), Some("year"), Some("license"), Some("mediaType"), Some("sourceUrl"))
      val copyright = Copyright(Some("Ed"), Some("2001"), Some("3000"), Some("imageName.png"))
      val contributorDetails = ContributorDetails(
        additionalCopyrights = List(additionalCopyright),
        copyright = Some(copyright),
        costForResource = Some(10),
        author = Some("Ed"),
        contributor = Some("Jenni"))
      val item = Item(contributorDetails = Some(contributorDetails))
      val json = Json.toJson(item)
      (json \ "additionalCopyrights").asOpt[Seq[AdditionalCopyright]].get(0) must equalTo(additionalCopyright)
      (json \ Keys.copyrightOwner).asOpt[String] must equalTo(Some("Ed"))
      (json \ Keys.copyrightYear).asOpt[String] must equalTo(Some("2001"))
      (json \ Keys.copyrightExpirationDate).asOpt[String] must equalTo(Some("3000"))
      (json \ Keys.copyrightImageName).asOpt[String] must equalTo(Some("imageName.png"))
      (json \ Keys.costForResource).asOpt[Int] must equalTo(Some(10))
      (json \ Keys.author).asOpt[String] must equalTo(Some("Ed"))
      (json \ Keys.contributor).asOpt[String] must equalTo(Some("Jenni"))
      (json \ Keys.licenseType).asOpt[String] must beNone
      (json \ Keys.sourceUrl).asOpt[String] must beNone

      val parsedItem = json.as[Item]
      parsedItem.contributorDetails.get.additionalCopyrights(0) must equalTo(additionalCopyright)
      parsedItem.contributorDetails.get.copyright.get.owner must equalTo(Some("Ed"))
      parsedItem.contributorDetails.get.copyright.get.year must equalTo(Some("2001"))
      parsedItem.contributorDetails.get.copyright.get.expirationDate must equalTo(Some("3000"))
      parsedItem.contributorDetails.get.costForResource must equalTo(Some(10))
      parsedItem.contributorDetails.get.author must equalTo(Some("Ed"))
      parsedItem.contributorDetails.get.contributor must equalTo(Some("Jenni"))
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

      val item = Item(collectionId = Some("1234567"))
      val clonedItem = itemService.clone(item)
      item.collectionId === clonedItem.get.collectionId

      item.collectionId = Some("0987654321")
      itemService.save(item)

      itemService.findOneById(clonedItem.get.id) match {
        case Some(fromDb) => clonedItem.get.collectionId === fromDb.collectionId
        case _ => failure("couldn't find cloned item")
      }
    }

    "prepend [copy] to title" in {
      val item = Item(collectionId = Some("1234567"), taskInfo = Some(TaskInfo(title = Some("something"))))
      val clonedItem = itemService.clone(item)
      clonedItem.get.taskInfo.get.title.get === "[copy] " + item.taskInfo.get.title.get
    }

    "prepend [copy] to empty taskinfo" in {
      val item = Item(collectionId = Some("1234567"))
      val clonedItem = itemService.clone(item)
      clonedItem.get.taskInfo.get.title.get === "[copy]"
    }

    "prepend [copy] to empty title" in {
      val item = Item(collectionId = Some("1234567"), taskInfo = Some(TaskInfo()))
      val clonedItem = itemService.clone(item)
      clonedItem.get.taskInfo.get.title.get === "[copy]"
    }

    /**/
  }

}
