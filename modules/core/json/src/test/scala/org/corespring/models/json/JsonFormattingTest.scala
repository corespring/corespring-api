package org.corespring.models.json

import org.bson.types.ObjectId
import org.corespring.models.item.resource.{ VirtualFile, StoredFile }
import org.corespring.models.{ Standard, Subject }
import org.corespring.models.item.Item.Keys
import org.corespring.models.item._
import org.specs2.mutable.Specification
import play.api.libs.json._

class JsonFormattingTest extends Specification {

  val dummyCollectionId = ObjectId.get.toString

  val mockFieldValues = FieldValue(
    depthOfKnowledge = Seq(StringKeyValue("Factual", "Factual")),
    bloomsTaxonomy = Seq(StringKeyValue("Applying", "Applying")),
    gradeLevels = Seq(StringKeyValue("03", "03"), StringKeyValue("04", "04")))
  val rootOrgId = ObjectId.get

  trait TestJsonFormatting extends JsonFormatting {
    override def fieldValue: FieldValue = mockFieldValues

    override def findStandardByDotNotation: (String) => Option[Standard] = s => None

    override def rootOrgId: ObjectId = JsonFormattingTest.this.rootOrgId

    override def findSubjectById: (ObjectId) => Option[Subject] = id => None
  }

  val formatter = new TestJsonFormatting {}

  implicit val f: Format[Item] = formatter.item

  "alignments" should {

    val item = Item(
      collectionId = dummyCollectionId,
      otherAlignments = Some(
        Alignments(
          depthOfKnowledge = Some("Factual"),
          bloomsTaxonomy = Some("Applying"))))

    val json = Json.toJson(item)
    val parsed = json.as[Item]

    "serialize + deserialize depthOfKnowledge" in {
      (json \ "depthOfKnowledge").as[String] === "Factual"
      parsed.otherAlignments.get.depthOfKnowledge must equalTo(Some("Factual"))
    }

    "serialize + deserialize bloomsTaxonomy" in {
      (json \ "bloomsTaxonomy").asOpt[String] === Some("Applying")
      parsed.otherAlignments.get.bloomsTaxonomy must equalTo(Some("Applying"))
    }
  }

  "player definition" should {

    val json = Json.obj(
      "collectionId" -> dummyCollectionId,
      "playerDefinition" -> Json.obj(
        "files" -> JsArray(Seq()),
        "xhtml" -> "<div/>",
        "components" -> Json.obj("3" -> Json.obj("componentType" -> "type")),
        "summaryFeedback" -> ""))
    val item = json.as[Item]

    "define playerDefinintion" in {
      item.playerDefinition.isDefined === true
    }
  }

  "data" should {

    val json = Json.obj(
      "collectionId" -> dummyCollectionId,
      "data" -> Json.obj(
        "name" -> "test resource",
        "files" -> JsArray(Seq(
          Json.obj(
            "name" -> "mc008-3.jpg",
            "contentType" -> "image/jpeg",
            "isMain" -> true,
            "storageKey" -> "52a5ed3e3004dc6f68cdd9fc/0/data/mc008-3.jpg"),
          Json.obj(
            "name" -> "qti.xml",
            "contentType" -> "text/xml",
            "isMain" -> true,
            "content" -> "<?xml version='1.0' encoding='UTF-8'?><assessmentItem></assessmentItem>")))))
    val item = json.as[Item]

    "define data" in {
      item.data.isDefined === true
    }

    "create stored file" in {
      item.data.get.files(0) === StoredFile(
        "mc008-3.jpg",
        "image/jpeg",
        true)
    }

    "create virtual file" in {
      item.data.get.files(1) === VirtualFile(
        "qti.xml",
        "text/xml",
        true,
        "<?xml version='1.0' encoding='UTF-8'?><assessmentItem></assessmentItem>")
    }
  }

  "workflow" should {

    val workflow = Workflow(
      setup = true,
      tagged = true,
      qaReview = true,
      standardsAligned = true)

    val item = Item(collectionId = dummyCollectionId, workflow = Some(workflow))
    val jsonItem = Json.toJson(item)
    val itemFromJson = jsonItem.as[Item]

    "serialize + deserialize setup" in {
      (jsonItem \ "workflow" \ "setup").as[Boolean] must equalTo(true)
      itemFromJson.workflow.get.setup must equalTo(true)
    }
    "serialize + deserialize tagged" in {
      (jsonItem \ "workflow" \ "tagged").as[Boolean] must equalTo(true)
      itemFromJson.workflow.get.tagged must equalTo(true)
    }
    "serialize + deserialize standardsAligned" in {
      (jsonItem \ "workflow" \ "standardsAligned").as[Boolean] must equalTo(true)
      itemFromJson.workflow.get.standardsAligned must equalTo(true)
    }
    "serialize + deserialize qaReview" in {
      (jsonItem \ "workflow" \ "qaReview").as[Boolean] must equalTo(true)
      itemFromJson.workflow.get.qaReview must equalTo(true)
    }
  }

  "standards" should {
    val item = Item(collectionId = dummyCollectionId, standards = Seq("RL.K.9"))
    val json = Json.toJson(item)
    val readJson = Json.obj("standards" -> Json.arr("RL.K.9"), "collectionId" -> dummyCollectionId)
    val parsed = readJson.as[Item]
    "parse" in parsed.standards === item.standards
  }

  "priorGradeLevel" should {
    val item = Item(collectionId = dummyCollectionId, priorGradeLevels = Seq("03", "04"))
    val json = Json.toJson(item)
    val parsedItem = json.as[Item]
    "parse" in parsedItem.priorGradeLevels === item.priorGradeLevels
  }

  "invalid priorGradeLevel" should {
    val item = Item(collectionId = dummyCollectionId, priorGradeLevels = Seq("apple", "pear"))
    val json = Json.toJson(item)
    "throw an exception" in {
      json.as[Item] must throwA[JsonValidationException]
    }
  }

  "subjects with only primary" should {
    val subjectId = ObjectId.get
    val subject = Subjects(primary = Some(subjectId))

    lazy val mockFormatter = new TestJsonFormatting {
      override def findSubjectById: (ObjectId) => Option[Subject] = (id) => {
        println("find subject by id ----------")
        Some(Subject(id = subjectId, subject = "some subject"))
      }
    }

    //The json that is submittted to be read is different from the db json
    val jsonToRead = Json.obj(
      Keys.collectionId -> dummyCollectionId,
      Keys.primarySubject -> JsString(subject.primary.get.toString))
    val parsed = jsonToRead.as[Item](mockFormatter.item)

    "parse primary" in parsed.taskInfo.get.subjects.get.primary === subject.primary
  }

  "subjects with primary and related" should {
    val subjectId = ObjectId.get
    val subject = Subjects(primary = Some(subjectId), related = Seq(subjectId))

    //The json that is submittted to be read is different from the db json
    val jsonToRead = Json.obj(
      Keys.id -> JsString(new ObjectId().toString),
      Keys.collectionId -> dummyCollectionId,
      Keys.primarySubject -> subject.primary.get.toString,
      Keys.relatedSubject -> Json.arr(subject.related.head.toString))

    val parsed = jsonToRead.as[Item]

    "parse primary" in parsed.taskInfo.get.subjects.get.primary === subject.primary
    "parse related" in parsed.taskInfo.get.subjects.get.related === subject.related
  }

  "contributor details" should {
    val additionalCopyright = AdditionalCopyright(Some("author"), Some("owner"), Some("year"), Some("license"), Some("mediaType"), Some("sourceUrl"))
    val copyright = Copyright(Some("Ed"), Some("2001"), Some("3000"), Some("imageName.png"))
    val contributorDetails = ContributorDetails(
      additionalCopyrights = List(additionalCopyright),
      copyright = Some(copyright),
      costForResource = Some(10),
      author = Some("Ed"))
    val item = Item(collectionId = dummyCollectionId, contributorDetails = Some(contributorDetails))
    val json = Json.toJson(item)

    import formatter.formatAdditionalCopyright

    "serialize" in {
      (json \ "additionalCopyrights").asOpt[Seq[AdditionalCopyright]].get(0) must equalTo(additionalCopyright)
      (json \ "copyrightOwner").asOpt[String] must equalTo(Some("Ed"))
      (json \ "copyrightYear").asOpt[String] must equalTo(Some("2001"))
      (json \ "copyrightExpirationDate").asOpt[String] must equalTo(Some("3000"))
      (json \ "copyrightImageName").asOpt[String] must equalTo(Some("imageName.png"))
      (json \ "costForResource").asOpt[Int] must equalTo(Some(10))
      (json \ "author").asOpt[String] must equalTo(Some("Ed"))
      (json \ "licenseType").asOpt[String] must beNone
      (json \ "sourceUrl").asOpt[String] must beNone
    }

    val parsedItem = json.as[Item]

    "deserialize" in {
      parsedItem.contributorDetails.get.additionalCopyrights(0) must equalTo(additionalCopyright)
      parsedItem.contributorDetails.get.copyright.get.owner must equalTo(Some("Ed"))
      parsedItem.contributorDetails.get.copyright.get.year must equalTo(Some("2001"))
      parsedItem.contributorDetails.get.copyright.get.expirationDate must equalTo(Some("3000"))
      parsedItem.contributorDetails.get.costForResource must equalTo(Some(10))
      parsedItem.contributorDetails.get.author must equalTo(Some("Ed"))
      parsedItem.contributorDetails.get.licenseType must beNone
      parsedItem.contributorDetails.get.sourceUrl must beNone
    }
  }
}

