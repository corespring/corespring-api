package org.corespring.v2.player.item

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.User
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.SecureSocialHelpers
import org.corespring.test.helpers.models.ItemHelper
import org.corespring.v2.auth.identifiers.WithRequestIdentitySequence
import org.corespring.v2.errors.Errors.{ orgCantAccessCollection, propertyNotFoundInJson }
import org.corespring.v2.player.scopes.user
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.Cookie
import play.api.test.FakeRequest

class CreateItemIntegrationTest extends IntegrationSpecification with SecureSocialHelpers {

  "calling create item" should {

    "should fail for a plain request with no json" in new createItem {
      status(result) === UNAUTHORIZED
      logger.debug(s"content: ${contentAsString(result)}")
      (contentAsJson(result) \ "error").asOpt[String] === Some(WithRequestIdentitySequence.errorMessage)
    }

    "should fail for a plain request with json + collection id" in new createItem(false, id => Some(Json.obj("collectionId" -> id))) {
      status(result) === UNAUTHORIZED
    }

    val badCollectionId = ObjectId.get
    "should fail for a auth request with json + bad collection id" in new createItem(false,
      id => Some(Json.obj("collectionId" -> badCollectionId.toString)),
      u => secureSocialCookie(u)) {
      status(result) === BAD_REQUEST
      logger.debug(s"content: ${contentAsString(result)}")
      (contentAsJson(result) \ "error").asOpt[String] === Some(orgCantAccessCollection(orgId, badCollectionId.toString, Permission.Write.name).message)
    }

    "should fail for a plain request with json" in new createItem(
      false,
      id => Some(Json.obj()),
      u => secureSocialCookie(u)) {
      status(result) === BAD_REQUEST
      logger.debug(s"content: ${contentAsString(result)}")
      (contentAsJson(result) \ "error").asOpt[String] === Some(propertyNotFoundInJson("collectionId").message)
    }

    "should work for a auth request with json + collection id" in new createItem(false,
      id => Some(Json.obj("collectionId" -> id)),
      u => secureSocialCookie(u)) {
      status(result) === OK
      logger.debug(s"content: ${contentAsString(result)}")
      val json = contentAsJson(result)
      VersionedId((json \ "itemId").as[String]).foreach { vid => ItemHelper.delete(vid) }
      (json \ "error").asOpt[String] === None
    }
  }

  class createItem(
    val addCookies: Boolean = false,
    jsonFn: (String => Option[JsValue]) = (s) => None,
    cookieFn: Option[User] => Option[Cookie] = u => None) extends user {

    lazy val result = {
      val create = org.corespring.container.client.controllers.resources.routes.Item.create()
      val cookies = cookieFn(Some(user)).toList
      val fr = FakeRequest(create.method, create.url).withCookies(cookies: _*)
      val result = jsonFn(collectionId.toString).map(route(fr, _)).getOrElse(route(fr))
      result.getOrElse(throw new RuntimeException("no result"))
    }

  }

}
