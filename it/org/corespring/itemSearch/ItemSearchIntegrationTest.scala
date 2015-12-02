package org.corespring.itemSearch

import org.bson.types.ObjectId
import org.corespring.it.helpers.{ StandardHelper, ItemHelper }
import org.corespring.it.scopes.orgWithAccessTokenAndItem
import org.corespring.it.{ IntegrationSpecification, ItemIndexCleaner }
import org.corespring.models.Standard
import org.corespring.models.item.{ Item, PlayerDefinition }
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json.Json

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scalaz.{ Success, Failure }

class ItemSearchIntegrationTest extends IntegrationSpecification {

  trait scope
    extends orgWithAccessTokenAndItem
    with ItemIndexCleaner {

    cleanIndex()

    val itemIndexService = bootstrap.Main.itemIndexService

    val mathStandard = mkStandard("Math", "M.1.2.3")
    val elaStandard = mkStandard("ELA", "E.1.2.3")
    val elaLiteracyStandard = mkStandard("ELA-Literacy", "EL.1.2.3")

    val mathItemId = insertItemWithStandard(mathStandard)
    val elaItemId = insertItemWithStandard(elaStandard)
    val elaLiteracyItemId = insertItemWithStandard(elaLiteracyStandard)

    override def after = {
      logger.info("after.. cleaning up..")
      removeData()
      cleanIndex()
    }

    protected def insertItemWithStandard(standard: Standard) = {
      StandardHelper.create(standard)
      ItemHelper.create(collectionId, itemWithStandard(standard))
    }

    protected def itemWithStandard(s: Standard): Item = {
      Item(collectionId = collectionId.toString,
        standards = Seq(s.dotNotation.get))
    }

    protected def mkStandard(subject: String, dotNotation: String) = {
      Standard(
        subject = Some(subject),
        dotNotation = Some(dotNotation),
        standard = Some("AAA"),
        category = Some("BBB"),
        subCategory = Some("CCC"))
    }

    protected def search(text: Option[String]) = {
      val query = ItemIndexQuery(text = text)
      val futureResult = itemIndexService.search(query)
      Await.ready(futureResult, Duration.Inf).value.get.get
    }

    protected def hitsShouldContain(results: ItemIndexSearchResult, id: VersionedId[ObjectId]*) = {
      val resultIds = results.hits.map(_.id).toSet
      val expectedIds = id.map(_.toString).toSet
      resultIds should_== expectedIds
    }

    protected def hitsToSet(hits: Seq[ItemIndexHit]) = {
      hits.map(_.id).toSet
    }

    protected def idsToSet(id: VersionedId[ObjectId]*) = {
      id.map(_.toString).toSet
    }
  }

  "search" should {

    "find item with Math standard by standard.category" in new scope {

      search(mathStandard.category) match {
        case Failure(e) => failure(s"Unexpected error $e")
        case Success(v) => {
          v.total should_== 1
          hitsToSet(v.hits) should_== idsToSet(mathItemId)
        }
      }
    }

    "find item with ELA standard by standard.subCategory" in new scope {

      search(elaStandard.subCategory) match {
        case Failure(e) => failure(s"Unexpected error $e")
        case Success(v) => {
          v.total should_== 2
          hitsToSet(v.hits) should_== idsToSet(elaItemId, elaLiteracyItemId)
        }
      }
    }

    "find item with ELA-Literacy standard by standard.subCategory" in new scope {

      search(elaLiteracyStandard.subCategory) match {
        case Failure(e) => failure(s"Unexpected error $e")
        case Success(v) => {
          v.total should_== 2
          hitsToSet(v.hits) should_== idsToSet(elaLiteracyItemId, elaItemId)
        }
      }
    }
  }
}
