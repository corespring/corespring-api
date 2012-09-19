package tests.models

import tests.BaseTest
import models.{Subject, Item}
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
  }

}
