package tests.models

import tests.BaseTest
import models.{Copyright, Subject, Item}
import play.api.libs.json.{JsString, JsObject, Json}
import org.bson.types.ObjectId
import com.mongodb.BasicDBObject

class ItemTest extends BaseTest {

  "item" should {
    "parse itemType" in {

      val item = Item(itemType = Some("itemType"))
      val json = Json.toJson(item)
      val parsedItem = json.as[Item]
      parsedItem.itemType must equalTo(item.itemType)
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

    "parse copyright" in {
      val copyright = Copyright(Some("Ed"), Some("2001"), Some("3000"))
      val item = Item(copyright = Some(copyright))
      val json = Json.toJson(item)
      (json \ "copyrightOwner").asOpt[String] must equalTo(Some("Ed"))
      (json \ "copyrightYear").asOpt[String] must equalTo(Some("2001"))
      (json \ "copyrightExpirationDate").asOpt[String] must equalTo(Some("3000"))

      val parsedItem = json.as[Item]
      parsedItem.copyright.get.owner must equalTo(Some("Ed"))
      parsedItem.copyright.get.year must equalTo(Some("2001"))
      parsedItem.copyright.get.expirationDate must equalTo(Some("3000"))
    }

    "parse cost for resource" in {
      val item = Item(costForResource = Some(10))
      val json = Json.toJson(item)
      (json \ Item.costForResource).asOpt[Int] must equalTo(Some(10))
      val parsed = json.as[Item]
      parsed.costForResource must equalTo(Some(10))
    }
  }

}
