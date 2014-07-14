package org.corespring.v2.player

import org.bson.types.ObjectId
import org.corespring.it.{ IntegrationHelpers, IntegrationSpecification }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.SecureSocialHelpers
import org.corespring.v2.auth.models.PlayerOptions
import org.corespring.v2.errors.Errors.noOrgIdAndOptions
import org.corespring.v2.player.scopes._
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.FakeRequest

class LoadEditorTest
  extends IntegrationSpecification
  with IntegrationHelpers {

  import org.corespring.container.client.controllers.apps.routes.Editor

  override val logger: org.slf4j.Logger = LoggerFactory.getLogger("it.load-editor")

  "the editor" should {

    "fail if I'm not authorized" in new unknownUser_editItemLoader() {
      status(result) === UNAUTHORIZED
      val err = noOrgIdAndOptions(FakeRequest("", ""))
      contentAsString(result) === org.corespring.container.client.views.html.error.main(err.statusCode, err.message).toString
    }

    "work if authorized with an access token" in new token_editItemLoader() {
      status(result) === OK
    }

    "work if authorized as a logged in user" in new user_editItemLoader() {
      status(result) === OK
    }

    "fail if there are bad options" in new clientIdAndOptions_editItemLoader("let me in") {
      status(result) === UNAUTHORIZED
    }

    "work if the options are good" in new clientIdAndOptions_editItemLoader(Json.stringify(Json.toJson(PlayerOptions.ANYTHING))) {
      status(result) === OK
    }

    //TODO: Add test with encryption running..
  }

  class unknownUser_editItemLoader extends orgWithAccessTokenAndItem with PlainRequestBuilder with itemLoader {
    override def getCall(itemId: VersionedId[ObjectId]): Call = Editor.editItem(itemId.toString)
  }

  class token_editItemLoader extends orgWithAccessTokenAndItem with TokenRequestBuilder with itemLoader {
    override def getCall(itemId: VersionedId[ObjectId]): Call = Editor.editItem(itemId.toString)
  }

  class user_editItemLoader extends userAndItem with SessionRequestBuilder with itemLoader with SecureSocialHelpers {
    override def getCall(itemId: VersionedId[ObjectId]): Call = Editor.editItem(itemId.toString)
  }

  class clientIdAndOptions_editItemLoader(val options: String, val skipDecryption: Boolean = true) extends clientIdAndOptions with IdAndOptionsRequestBuilder with itemLoader {
    override def getCall(itemId: VersionedId[ObjectId]): Call = Editor.editItem(itemId.toString)
  }
}
