package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.models.item._
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.PlaySingleton
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import play.api.libs.json.{ JsObject, Json }
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{ FakeHeaders, FakeRequest }

import scalaz.{ Failure, Success, Validation }

class ItemApiCreateTest extends ItemApiSpec {

  /**
   * We should not need to run the app for a unit test.
   * However the way the app is tied up (global Dao Objects) - we need to boot a play application.
   */
  PlaySingleton.start()

  case class createApiScope(
    defaultCollectionId: ObjectId = ObjectId.get,
    insertFails: Boolean = false,
    canCreate: Validation[V2Error, Boolean] = Success(true)) extends ItemApiScope {

    mockItemService.insert(any[Item]).returns {
      if (insertFails) None else Some(VersionedId(ObjectId.get))
    }

    mockItemAuth.canCreateInCollection(anyString)(any[OrgAndOpts]) returns canCreate

    override def orgAndOpts: Validation[V2Error, OrgAndOpts] = canCreate.map(_ => mockOrgAndOpts())
  }

  "V2 - ItemApi" should {

    "when calling create" should {

      "ignores the id in the request" in new createApiScope {
        val result = api.create()(FakeJsonRequest(Json.obj("id" -> "blah")))
        (contentAsJson(result) \ "id").as[String] !== "blah"
        (contentAsJson(result) \ "collectionId").as[String] === defaultCollectionId.toString
        status(result) === OK
      }

      "if there's no json in the body - it uses the default json" in new createApiScope {
        val result = api.create()(FakeJsonRequest(Json.obj()))
        (contentAsJson(result) \ "collectionId").as[String] === defaultCollectionId.toString
        (contentAsJson(result) \ "playerDefinition").as[JsObject] === api.defaultPlayerDefinition
        status(result) === OK
      }

      "if there's no player definition in the json body - it adds the default player definition" in new createApiScope {
        val customCollectionId = ObjectId.get
        val result = api.create()(FakeJsonRequest(Json.obj("collectionId" -> customCollectionId.toString)))
        (contentAsJson(result) \ "collectionId").as[String] === customCollectionId.toString
        (contentAsJson(result) \ "playerDefinition").as[JsObject] === api.defaultPlayerDefinition
        status(result) === OK
      }

      s"returns $OK - it ignores bad json, if it can't be parsed" in new createApiScope {
        val customCollectionId = ObjectId.get
        val result = api.create()(
          FakeRequest("", "", FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), AnyContentAsText("bad")))
        status(result) === OK
      }

      s"returns $UNAUTHORIZED - if permission denied" in new createApiScope(
        canCreate = Failure(generalError("Nope", UNAUTHORIZED))) {
        val result = api.create()(FakeJsonRequest(Json.obj()))
        val e = canCreate.toEither.left.get
        result must beCodeAndJson(e.statusCode, e.json)
      }

      s"create - returns error with a bad save" in new createApiScope(
        insertFails = true) {
        val result = api.create()(FakeJsonRequest(Json.obj()))
        val e = errorSaving("Insert failed")
        result must beCodeAndJson(e.statusCode, e.json)
      }
    }

  }
}
