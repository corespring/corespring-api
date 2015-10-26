package org.corespring.services.salat.item

import org.bson.types.ObjectId
import org.corespring.models.item.{ TaskInfo, ContributorDetails, Item }
import org.corespring.services.salat.ServicesSalatIntegrationTest
import org.specs2.specification.Scope

class ItemAggregationServiceTest extends ServicesSalatIntegrationTest {

  val collectionId = ObjectId.get

  trait scope extends Scope {

    def item = Item(
      collectionId = collectionId.toString)
    def addContributor(name: String) = {
      val newItem = item.copy(
        contributorDetails = Some(ContributorDetails(contributor = Some(name))))
      services.itemService.insert(newItem)
    }

    def addItemType(name: String) = {
      val newItem = item.copy(
        taskInfo = Some(TaskInfo(itemType = Some(name))))
      services.itemService.insert(newItem)
    }

    val names = Seq("andrew", "attila", "ben", "ed", "ev", "ralf")
    val service = services.itemAggregationService
  }

  "itemTypeCount" should {

    "return counts for itemTypes" in new scope {

      names.foreach { n =>
        (1 to 20).foreach { _ =>
          addItemType(n)
        }
      }
      val future = service.taskInfoItemTypeCounts(Seq(collectionId))
      val map = waitFor(future)
      map.get("ed") === Some(20)
    }
  }

  "contributorCounts" should {

    "return counts for contributors" in new scope {

      names.foreach { n =>
        (1 to 20).foreach { _ =>
          addContributor(n)
        }
      }
      val future = service.contributorCounts(Seq(collectionId))
      val map = waitFor(future)
      map.get("ed") === Some(20)
    }
  }

}
