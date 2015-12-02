package org.corespring.services.salat.metadata

import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.models.ContentCollection
import org.corespring.models.item.{ TaskInfo, Item }
import org.corespring.models.metadata.Metadata
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.salat.ServicesSalatIntegrationTest
import org.specs2.mutable.{ After, BeforeAfter }

class MetadataServiceTest extends ServicesSalatIntegrationTest {

  "MetadataService" should {

    lazy val service = services.metadataService

    trait scope extends After with InsertionHelper {

      lazy val org = insertOrg("1")
      lazy val collection = insertCollection("collection-one", org)
      lazy val itemWithMetadata = addItem(1, collection, Some(Map(
        "metadata-1" -> MongoDBObject("a" -> "1", "b" -> "2"),
        "metadata-2" -> MongoDBObject("c" -> "1", "d" -> "2"))))
      lazy val itemSensMetadata = addItem(3, collection, None)

      lazy val metadataOne = Metadata("metadata-1", Map("a" -> "1", "b" -> "2"))
      lazy val metadataTwo = Metadata("metadata-2", Map("c" -> "1", "d" -> "2"))

      def addItem(id: Int, c: ContentCollection, extended: Option[Map[String, DBObject]] = None) = {
        val item = Item(
          collectionId = c.id.toString,
          taskInfo = if (extended.isDefined) Some(TaskInfo(extended = extended.get)) else None)
        services.itemService.insert(item)
        item
      }

      def after: Any = removeAllData()
    }

    "get" should {
      "return metadata for item" in new scope {
        service.get(itemWithMetadata.id, Seq("metadata-1")) must_== Seq(metadataOne)
      }
      "filter metadata by keys" should {
        "return empty result if key does not exist" in new scope {
          service.get(itemWithMetadata.id, Seq("non existent metadata key")) must_== Seq.empty
        }
        "return empty result if keys is empty" in new scope {
          service.get(itemWithMetadata.id, Seq.empty) must_== Seq.empty
        }
        "return metadata-1" in new scope {
          service.get(itemWithMetadata.id, Seq("metadata-1")) must_== Seq(metadataOne)
        }
        "return metadata-1 and metadata-2" in new scope {
          service.get(itemWithMetadata.id, Seq("metadata-1", "metadata-2")) must_== Seq(metadataOne, metadataTwo)
        }
      }
      "return empty seq, if item does not exist" in new scope {
        service.get(VersionedId(ObjectId.get), Seq("metadata-1")) must_== Seq.empty
      }
      "return empty seq, if item does not have metadata" in new scope {
        service.get(itemSensMetadata.id, Seq("doesn't matter")) must_== Seq.empty
      }
    }
  }
}
