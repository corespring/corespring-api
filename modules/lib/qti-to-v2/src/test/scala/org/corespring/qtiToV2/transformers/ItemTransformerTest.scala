package org.corespring.qtiToV2.transformers

import com.mongodb.casbah.Imports._
import com.novus.salat.dao.ModelCompanion
import org.bson.types.ObjectId
import org.corespring.platform.core.models.ContentCollection
import org.corespring.platform.core.models.item._
import org.corespring.platform.core.models.item.resource.{BaseFile, Resource, VirtualFile}
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.PlaySingleton
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.Configuration
import play.api.libs.json.{ JsObject, JsValue, Json }

class ItemTransformerTest extends Specification with Mockito {


  PlaySingleton.start()

  val itemServiceMock = mock[ItemService]

  val mockCollectionId = ObjectId.get()

  var mockCollection:Option[ContentCollection] = Some(new ContentCollection("Collection name",ownerOrgId = ObjectId.get(),id = mockCollectionId))

  val itemTransformer = new ItemTransformer {
    def itemService = itemServiceMock

    override def configuration: Configuration = Configuration.empty

    override def findCollection(id:ObjectId):Option[ContentCollection] = mockCollection
  }

  val qti =
    <assessmentItem>
      <responseDeclaration identifier="Q_01" cardinality="single" baseType="identifier">
        <correctResponse>
          <value>ChoiceA</value>
        </correctResponse>
      </responseDeclaration>
      <itemBody>
        <choiceInteraction responseIdentifier="Q_01" shuffle="false" maxChoices="1">
          <prompt>ITEM PROMPT?</prompt>
          <simpleChoice identifier="ChoiceA">
            ChoiceA text (Correct Choice)
            <feedbackInline identifier="ChoiceA" defaultFeedback="true"/>
          </simpleChoice>
          <simpleChoice identifier="ChoiceD">
            ChoiceD text
            <feedbackInline identifier="ChoiceD" defaultFeedback="true"/>
          </simpleChoice>
        </choiceInteraction>
      </itemBody>
    </assessmentItem>

  "Item transformer" should {

    "map task info" in {

      val oid = ObjectId.get.toString
      val apiJson = Json.obj(
        "primarySubject" -> Json.obj("id" -> oid, "subject" -> "a", "category" -> "b"),
        "relatedSubject" -> Json.obj("id" -> oid, "subject" -> "c", "category" -> "d"))

      val v2Json = Json.obj(
        "subjects" -> Json.obj(
          "primary" -> apiJson \ "primarySubject",
          "related" -> apiJson \ "relatedSubject"))

      itemTransformer.mapTaskInfo(apiJson) === v2Json
    }

    "transform an item to poc json" in {
      val itemId = VersionedId(ObjectId.get())

      val item = Item(
        id = itemId,
        collectionId = Some(mockCollectionId.toString),
        lexile = Some("30"),
        reviewsPassed = Seq("RP1", "RP2"),
        reviewsPassedOther = Some("RPO"),
        priorGradeLevels = Seq("PGL1", "PGL2"),
        priorUse = Some("PU"),
        priorUseOther = Some("PUO"),
        taskInfo = Some(
          TaskInfo(
            title = Some("item one"))),
        contributorDetails = Some(ContributorDetails(
          credentials = Some("CR"),
          credentialsOther = Some("CRO"),
          copyright = Some(Copyright(
            owner = Some("owner"),
            year = Some("1234"),
            expirationDate = Some("2345"))))),
        otherAlignments = Some(Alignments(
          bloomsTaxonomy = Some("BT"),
          keySkills = Seq("KS1", "KS2"),
          depthOfKnowledge = Some("DOK"))),
        data = Some(
          Resource(
            name = "data",
            files = Seq(
              VirtualFile(
                name = "qti.xml",
                contentType = "text/xml",
                content = qti.toString,
                isMain = true),
              VirtualFile(
                name = "kittens.jpeg",
                contentType = "image/jpeg",
                content = "")))))

      var json = itemTransformer.transformToV2Json(item)
      val imageJson = (json \ "files").as[Seq[JsObject]].head

      (json \ "itemId").as[String] must be equalTo itemId.toString
      (json \ "collection" \ "id").as[String] must be equalTo mockCollectionId.toString
      (json \ "collection" \ "name").as[String] must be equalTo "Collection name"
      (json \ "metadata" \ "title").as[String] === "item one"

      (json \ "components" \ "Q_01").asOpt[JsObject] must beSome[JsObject]
      (json \ "files").as[Seq[JsObject]].map(f => (f \ "name").as[String]).contains("qti.xml") must beFalse
      (imageJson \ "name").as[String] must be equalTo "kittens.jpeg"
      (imageJson \ "contentType").as[String] must be equalTo "image/jpeg"

      (json \ "profile").asOpt[JsObject] must beSome[JsObject]

      (json \ "profile" \ "lexile").asOpt[String] === Some("30")
      (json \ "profile" \ "reviewsPassed").as[Seq[String]] === Seq("RP1", "RP2")
      (json \ "profile" \ "reviewsPassedOther").asOpt[String] === Some("RPO")
      (json \ "profile" \ "priorGradeLevel").as[Seq[String]] === Seq("PGL1", "PGL2")
      (json \ "profile" \ "priorUse").asOpt[String] === Some("PU")
      (json \ "profile" \ "priorUseOther").asOpt[String] === Some("PUO")

      (json \ "profile" \ "contributorDetails").asOpt[JsObject] must beSome[JsObject]
      (json \ "profile" \ "contributorDetails" \ "credentials").asOpt[String] === Some("CR")
      (json \ "profile" \ "contributorDetails" \ "credentialsOther").asOpt[String] === Some("CRO")
      (json \ "profile" \ "contributorDetails" \ "copyrightOwner").asOpt[String] === Some("owner")
      (json \ "profile" \ "contributorDetails" \ "copyrightYear").asOpt[String] === Some("1234")
      (json \ "profile" \ "contributorDetails" \ "copyrightExpirationDate").asOpt[String] === Some("2345")

      (json \ "profile" \ "otherAlignments").asOpt[JsObject] must beSome[JsObject]
      (json \ "profile" \ "otherAlignments" \ "bloomsTaxonomy").asOpt[String] === Some("BT")
      (json \ "profile" \ "otherAlignments" \ "keySkills").as[Seq[String]] === Seq("KS1", "KS2")
      (json \ "profile" \ "otherAlignments" \ "depthOfKnowledge").asOpt[String] === Some("DOK")

      mockCollection = None
      json = itemTransformer.transformToV2Json(item)
      (json \ "collection").asOpt[JsObject] must beSome[JsObject]
    }

  }

  "createPlayerDefinition" should {

    val itemTypes = Map("corespring-multiple-choice" -> 1)

    val qtiFile = new VirtualFile(name = "qti.xml", contentType = "application/xml", isMain = true, qti.toString)

    val item = Item(data = Some(Resource(name = "qti.xml", files = Seq(qtiFile))))

    "save item with playerDefinition" in {
      val captor = capture[Item]
      itemTransformer.createPlayerDefinition(item)
      there were two(itemServiceMock).save(captor, any[Boolean])
      captor.value.taskInfo.map(_.itemTypes) must beEqualTo(Some(itemTypes))
    }

    "return item with playerDefinition" in {
      itemTransformer.createPlayerDefinition(item).itemTypes must beEqualTo(itemTypes)
    }

  }

}

