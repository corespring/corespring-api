package org.corespring.api.v1

import bootstrap.Main
import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.{ StandardHelper, CollectionHelper, ItemHelper }
import org.corespring.it.scopes.{ TokenRequestBuilder, orgWithAccessToken }
import org.corespring.models.Standard
import org.corespring.models.item.{ TaskInfo, Item }
import play.api.libs.json.{ JsObject, Json, JsArray }
import play.api.mvc.{ AnyContent, Request }

class ItemApiSearchIntegrationTest extends IntegrationSpecification {

  val Routes = org.corespring.api.v1.routes.ItemApi

  import bootstrap.Main.jsonFormatting._

  def mkStandards(count: Int): Seq[Standard] = {
    (1 to count).flatMap { c =>
      val standard = Standard(dotNotation = Some(s"MOCK.DOTNOTATION.$count"))
      Main.standardService.insert(standard).map { id =>
        standard.copy(id = id)
      }
    }
  }

  case class JsonResult(array: JsArray)
  trait search extends orgWithAccessToken with TokenRequestBuilder {
    lazy val q: Option[JsObject] = None
    lazy val f: Option[JsObject] = None
    lazy val c = false
    lazy val sk = 0
    lazy val l = 0
    lazy val sort = None
    lazy val call = Routes.list(q.map(Json.stringify(_)), f.map(Json.stringify(_)), c.toString, sk, l, sort)
    lazy val req = makeRequest(call)
    lazy val collectionId = CollectionHelper.create(orgId)

    protected def loadResult(rh: Request[AnyContent]): JsonResult = {
      route(rh)(writeable).map { r =>
        logger.debug(s"${contentAsString(r)}")

        if (status(r) == 200) {
          val json = contentAsJson(r)
          val array = json.as[JsArray]
          JsonResult(array)
        } else {

          logger.warn(s"got ${status(r)} for requst: ${rh.path}?${rh.rawQueryString}")
          throw new RuntimeException("")
        }
      }.getOrElse {
        throw new RuntimeException("No result returned")
      }
    }

    protected def addItems(count: Int): Seq[Item] = {
      (1 to count).map { c =>
        val item = Item(collectionId = collectionId.toString, taskInfo = Some(TaskInfo(title = Some(s"Item: $c"))))
        val vid = ItemHelper.create(collectionId, item)
        item.copy(id = vid)
      }
    }
  }

  "list with no query" should {

    "return empty results" in new search {
      route(req)(writeable).map { r =>
        status(r) === OK
      }
    }

    "return 1 result" in new search {
      ItemHelper.create(collectionId)
      val result = loadResult(req)
      (result.array(0) \ "format").as[JsObject] === Json.obj("apiVersion" -> 1, "hasQti" -> true, "hasPlayerDefinition" -> false)
    }

    "return 100 results" in new search {
      addItems(100)
      val result = loadResult(req)
      result.array.value.length === 100
    }

    "limit a result set of 100 to 20 using 'l=20'" in new search {
      addItems(100)
      override lazy val l = 20
      val result = loadResult(req)
      result.array.value.length === 20
    }

    "skip a result set of 100 to the 20th item using 'sk=20'" in new search {
      val items = addItems(100)
      override lazy val sk = 20
      val result = loadResult(req)
      result.array.value.length === 80 //100 - 20
      (result.array.value(0) \ "id").asOpt[String] === Some(items(20).id.toString)
    }

    "skip AND limit a result set of 100 to return 10 items from the 40th index using 'sk=40&l=10'" in new search {
      val items = addItems(100)
      override lazy val sk = 40
      override lazy val l = 10
      val result = loadResult(req)
      result.array.value.length === 10
      (result.array.value(0) \ "id").asOpt[String] === Some(items(40).id.toString)
    }
  }

  "list with query" should {

    "search for dotNotation" in new search {
      lazy val standards = mkStandards(1)
      val item = Item(collectionId = collectionId.toString, standards = standards.flatMap(_.dotNotation))
      ItemHelper.create(collectionId, item)
      override lazy val q = Some(Json.obj("standards.dotNotation" -> standards(0).dotNotation.get))
      override lazy val f = Some(Json.obj("standards" -> 1))
      val result = loadResult(req)
      (result.array(0) \ "standards").as[JsArray].value(0) === Json.toJson(standards(0))
    }

    "search for title" in new search {
      val items = addItems(1)
      override lazy val q = Some(Json.obj("title" -> items(0).taskInfo.get.title.get))
      override lazy val f = Some(Json.obj("title" -> 1))
      val result = loadResult(req)
      (result.array(0) \ "title").asOpt[String] === Some(items(0).taskInfo.get.title.get)
    }
  }
}
