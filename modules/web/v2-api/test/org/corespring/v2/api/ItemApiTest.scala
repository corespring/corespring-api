package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.api.services.ScoreService
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.{AuthMode, MockFactory, OrgAndOpts}
import org.corespring.v2.errors.Errors.{cantParseItemId, generalError}
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader
import play.api.test.{FakeRequest, PlaySpecification}

import scala.concurrent.ExecutionContext
import scalaz.{Failure, Success, Validation}

class ItemApiTest extends Specification with Mockito with MockFactory with PlaySpecification{

  "ItemApi" should {
    "when calling clone" should{

      lazy val orgAndOpts = mockOrgAndOpts(AuthMode.UserSession)

      lazy val vid = VersionedId(ObjectId.get)
      lazy val clonedId = VersionedId(ObjectId.get)

      lazy val mockItem = {
        val m = mock[Item]
        m.id returns clonedId
        m
      }

      case class ItemApiCloneScope( vid : String = vid.toString,
                                    id:Validation[V2Error,OrgAndOpts] = Success(orgAndOpts),
                                    itemAuthLoadsItem : Boolean = true,
                                    itemServiceClones : Boolean = true,
                                    item : Option[Item] = Some(mockItem)
                                    ) extends Scope{


        lazy val api = new ItemApi {
          override def defaultCollection(implicit identity: OrgAndOpts): Option[String] = ???

          override def itemService: ItemService = {
            val m = mock[ItemService]
            m.clone(any[Item]) returns (if(itemServiceClones) item else None)
            m
          }

          override def getSummaryData: (Item, Option[String]) => JsValue = ???

          override def scoreService: ScoreService = ???

          override def itemAuth: ItemAuth[OrgAndOpts] = {
            import scalaz.Scalaz._
            val m = mock[ItemAuth[OrgAndOpts]]
            m.loadForRead(anyString)(any[OrgAndOpts]) returns {
              val out = if (itemAuthLoadsItem) item else None
              out.toSuccess(generalError("Test error: itemAuth.loadForRead"))
            }
            m
          }

          override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

          override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = id

        }
        lazy val result = api.cloneItem(vid.toString)(FakeRequest("", ""))
      }

      "return 200" in new ItemApiCloneScope {
        status(result) === OK
      }

      "return the cloned id" in new ItemApiCloneScope {
        (contentAsJson(result) \ "id").as[String] === clonedId.toString
      }

      val testError = generalError("test error")

      "fail if there's no identity" in new ItemApiCloneScope(id = Failure(testError)){
        status(result) === testError.statusCode
      }

      "fail if the item id is unparseable" in new ItemApiCloneScope(vid = "?"){
        status(result) === testError.statusCode
        (contentAsJson(result) \ "message").as[String] === cantParseItemId("?").message
      }

      "fail if clone fails" in new ItemApiCloneScope(itemServiceClones = false){
        status(result) === testError.statusCode
        val json = contentAsJson(result)
        (json \ "message").as[String] must contain("Error cloning")
      }
    }
  }
}
