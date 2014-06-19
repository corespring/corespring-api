package org.corespring.v2player.integration

import org.bson.types.ObjectId
import org.corespring.it.{ IntegrationHelpers, IntegrationSpecification }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.SecureSocialHelpers
import org.corespring.v2player.integration.errors.Errors.noOrgIdAndOptions
import org.corespring.v2player.integration.scopes._
import org.slf4j.LoggerFactory
import play.api.mvc._
import play.api.test.FakeRequest

class LoadEditorTest
  extends IntegrationSpecification
  with IntegrationHelpers {

  override val logger: org.slf4j.Logger = LoggerFactory.getLogger("it.load-editor")

  "when I load the editor js with orgId and encrypted options" should {

    "fail if I'm not authorized" in new unknownUser_editItemLoader() {
      status(result) === UNAUTHORIZED
      val err = noOrgIdAndOptions(FakeRequest("", ""))
      contentAsString(result) === org.corespring.container.client.views.html.error.main(err.code, err.message).toString
    }

    "work if authorized with an access token" in new token_editItemLoader() {
      status(result) === OK
    }

    "work if authorized as a logged in user" in new user_editItemLoader() {
      status(result) === OK
    }
  }

  class unknownUser_editItemLoader extends orgWithAccessTokenAndItem with PlainRequestBuilder with itemLoader {
    import org.corespring.container.client.controllers.apps.routes.Editor
    override def getCall(itemId: VersionedId[ObjectId]): Call = Editor.editItem(itemId.toString)
  }

  class token_editItemLoader extends orgWithAccessTokenAndItem with TokenRequestBuilder with itemLoader {
    import org.corespring.container.client.controllers.apps.routes.Editor
    override def getCall(itemId: VersionedId[ObjectId]): Call = Editor.editItem(itemId.toString)
  }

  class user_editItemLoader extends userAndItem with SessionRequestBuilder with itemLoader with SecureSocialHelpers {
    import org.corespring.container.client.controllers.apps.routes.Editor
    override def getCall(itemId: VersionedId[ObjectId]): Call = Editor.editItem(itemId.toString)
  }
}
