package org.corespring.v2.player

import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.models.item.resource.{ Resource, VirtualFile }
import org.corespring.models.item.{ Item, TaskInfo }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.salat.bootstrap.CollectionNames
import org.specs2.mutable.BeforeAfter

class AllItemVersionTransformerIntegrationTest extends IntegrationSpecification {

  trait WithItem extends BeforeAfter {
    lazy val itemService = bootstrap.Main.itemService
    lazy val currentCollection = bootstrap.Main.db(CollectionNames.item)
    lazy val versionedCollection = bootstrap.Main.db(CollectionNames.versionedItem)
    lazy val oid = ObjectId.get
    lazy val idQuery = MongoDBObject("_id._id" -> oid)
    lazy val item = Item(
      collectionId = ObjectId.get.toString,
      id = VersionedId(oid, Some(0)),
      data = Some(Resource(
        name = "data",
        files = Seq(
          VirtualFile(
            name = "qti.xml",
            contentType = "text/xml",
            content = "<assessmentItem><itemBody></itemBody></assessmentItem>",
            isMain = true)))))
    itemService.insert(item)
    lazy val update = item.copy(taskInfo = Some(TaskInfo(title = Some("New title"))))
    itemService.save(update, createNewVersion = true)
    lazy val transformer = bootstrap.Main.itemTransformer

    override def after: Any = {
      currentCollection.remove(idQuery)
      versionedCollection.remove(idQuery)
    }

    override def before: Any = {

    }
  }

  "AllItemVersionTransformer" should {

    "save transformation in the right collection" in new WithItem {
      transformer.updateV2Json(item)
      currentCollection.count(idQuery) === 1
      versionedCollection.count(idQuery) === 1
    }
  }
}
