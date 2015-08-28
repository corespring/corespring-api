package org.corespring.v2.player

import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.item.resource.{ Resource, VirtualFile }
import org.corespring.platform.core.models.item.{ Item, TaskInfo }
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.mutable.BeforeAfter

class AllItemVersionTransformerIntegrationTest extends IntegrationSpecification {

  trait WithItem extends BeforeAfter {
    lazy val oid = ObjectId.get
    lazy val idQuery = MongoDBObject("_id._id" -> oid)
    lazy val item = Item(
      id = VersionedId(oid, Some(0)),
      data = Some(Resource(
        name = "data",
        files = Seq(
          VirtualFile(
            name = "qti.xml",
            contentType = "text/xml",
            content = "<assessmentItem><itemBody></itemBody></assessmentItem>",
            isMain = true)))))
    ItemServiceWired.insert(item)
    lazy val update = item.copy(taskInfo = Some(TaskInfo(title = Some("New title"))))
    ItemServiceWired.save(update, createNewVersion = true)
    lazy val transformer = new AllItemVersionTransformer()

    override def after: Any = {
      ItemServiceWired.itemDao.currentCollection.remove(idQuery)
      ItemServiceWired.itemDao.versionedCollection.remove(idQuery)
    }

    override def before: Any = {

    }
  }

  "AllItemVersionTransformer" should {

    "save transformation in the right collection" in new WithItem {
      transformer.updateV2Json(item)
      ItemServiceWired.itemDao.currentCollection.count(idQuery) === 1
      ItemServiceWired.itemDao.versionedCollection.count(idQuery) === 1
    }
  }
}
