package org.corespring.services.salat.item

import com.mongodb.casbah.MongoCollection
import com.novus.salat.Context
import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.models.appConfig.ArchiveConfig
import org.corespring.models.item.Item
import org.corespring.models.item.resource.{ CloneError, CloningFailed, CloneFileResult }
import org.corespring.platform.data.VersioningDao
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.OrgCollectionService
import org.corespring.services.salat.bootstrap.SalatServicesExecutionContext
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent.ExecutionContext
import scalaz.{ Failure, Success, Validation }

class ItemServiceTest extends Specification with Mockito {

  "save" should {

    trait save extends Scope {
      def succeed: Boolean

      protected def inc(v: VersionedId[ObjectId]): VersionedId[ObjectId] = v.copy(version = v.version.map(_ + 1))

      val item = Item(collectionId = ObjectId.get.toString, id = VersionedId(ObjectId.get, Some(0)))

      val dao = {
        val m = mock[VersioningDao[Item, VersionedId[ObjectId]]]
        m.save(any[Item], any[Boolean]) returns Right(inc(item.id))
        m.findOneById(any[VersionedId[ObjectId]]) returns Some(item)
        m
      }

      val assets = {
        val m = mock[ItemAssetService]
        m.cloneStoredFiles(any[Item], any[Item]).answers { (args, _) =>
          {
            val out: Validation[CloneError, Item] = if (succeed) {
              val arr = args.asInstanceOf[Array[Any]]
              Success(arr(1).asInstanceOf[Item])
            } else {
              Failure(CloningFailed(Seq.empty[CloneFileResult]))
            }
            out
          }
        }
        m
      }
      val orgCollectionService = {
        val m = mock[OrgCollectionService]
        m
      }

      val context = new Context {
        override val name: String = "mock-context"
      }

      val mongoCollection = mock[MongoCollection]

      val archiveConfig = ArchiveConfig(ObjectId.get, ObjectId.get)
      val salatServicesExecutionContext = SalatServicesExecutionContext(ExecutionContext.global)
      val itemService = new ItemService(
        dao,
        mongoCollection,
        assets,
        orgCollectionService,
        context,
        archiveConfig,
        salatServicesExecutionContext)
    }

    "revert the version if a failure occurred when cloning stored files" in new save {
      override def succeed = false
      val result = itemService.save(item, true)
      there was one(dao).save(any[Item], org.mockito.Matchers.eq(true))
      there was one(dao).revertToVersion(item.id)
      result must_== Failure(PlatformServiceError("Cloning of files failed"))
    }

    "update the version if no failure occurred when cloning stored files" in new save {
      override def succeed = true
      val result = itemService.save(item, true)
      there was one(dao).save(any[Item], org.mockito.Matchers.eq(true))
      there was no(dao).revertToVersion(item.id)
      result must_== Success(inc(item.id))
    }
  }

  "countItemsInCollections" should {
    "call mongoCollection.aggregate" in pending
    "add empty ItemCounts" in pending
    "sort by collectionId" in pending
    "return Seq[ItemCount]" in pending
  }

}
