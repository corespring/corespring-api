package org.corespring.v2.player.item

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.{ ItemHelper, OrgCollectionHelper, SecureSocialHelper }
import org.corespring.it.scopes.user
import org.corespring.models.User
import org.corespring.models.auth.Permission
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.identifiers.WithRequestIdentitySequence
import org.corespring.v2.errors.Errors.orgCantAccessCollection
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.Cookie
import play.api.test.FakeRequest

class CreateItemIntegrationTest extends IntegrationSpecification with SecureSocialHelper {

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

    "should work for a plain request with json by using looking up the default collectionId for the org" in new createItem(
      false,
      id => Some(Json.obj()),
      u => secureSocialCookie(u)) {
      status(result) === OK
      logger.debug(s"content: ${contentAsString(result)}")
      val itemId = (contentAsJson(result) \ "itemId").asOpt[String].flatMap { id =>
        VersionedId(id)
      }
      val itemCollectionId = ItemHelper.get(itemId.get).get.collectionId
      val orgDefaultCollection = OrgCollectionHelper.getDefaultCollection(user.org.orgId).get
      itemCollectionId must_== orgDefaultCollection.id.toString
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
