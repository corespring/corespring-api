package org.corespring.v2.player

import org.bson.types.ObjectId
import org.corespring.drafts.item.models.DraftId
import org.corespring.it.{ IntegrationHelpers, IntegrationSpecification }
import org.corespring.platform.core.models.Organization
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.SecureSocialHelpers
import org.corespring.v2.auth.identifiers.WithRequestIdentitySequence
import org.corespring.v2.auth.models.PlayerAccessSettings
import org.corespring.v2.errors.Errors.{ generalError, noOrgIdAndOptions }
import org.corespring.v2.player.scopes._
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.FakeRequest

class LoadEditorTest
  extends IntegrationSpecification
  with IntegrationHelpers {

  import org.corespring.container.client.controllers.apps.routes.DraftEditor

  override val logger: org.slf4j.Logger = LoggerFactory.getLogger("it.load-editor")

  "the editor" should {

    "fail if I'm not authorized" in new unknownUser_editItemLoader() {
      status(result) === UNAUTHORIZED
      val err = generalError(WithRequestIdentitySequence.errorMessage, UNAUTHORIZED)
      contentAsString(result) === org.corespring.container.client.views.html.error.main(err.statusCode, err.message, false).toString
    }

    "work if authorized with an access token" in new token_editItemLoader() {
      status(result) === OK
    }

    "work if authorized as a logged in user" in new user_editItemLoader() {
      status(result) === OK
    }

    "fail if there are bad options" in new clientIdAndPlayerToken_editItemLoader("let me in") {
      status(result) === UNAUTHORIZED
    }

    "work if the options are good" in new clientIdAndPlayerToken_editItemLoader(Json.stringify(Json.toJson(PlayerAccessSettings.ANYTHING))) {
      status(result) === OK
    }

    "once the editor is loaded allow subsequent saves" in new clientIdAndPlayerToken_editItemLoader(Json.stringify(Json.toJson(PlayerAccessSettings.ANYTHING))) {
      //inspect the lazy val to trigger the load
      status(result) === OK
      val call = org.corespring.container.client.controllers.resources.routes.ItemDraft.saveSubset(draftId.toIdString, "xhtml")
      val r = makeRequest(call, AnyContentAsJson(Json.obj("xhtml" -> "hi")))
      route(r.asInstanceOf[Request[AnyContentAsJson]])(writeableOf_AnyContentAsJson).map { result =>
        println(contentAsString(result))
        status(result) === OK
      }
    }
  }

  class unknownUser_editItemLoader extends orgWithAccessTokenAndItem with PlainRequestBuilder with itemDraftLoader {
    override def getCall(draftId: DraftId): Call = DraftEditor.load(draftId.toIdString)
  }

  class token_editItemLoader extends orgWithAccessTokenAndItem with TokenRequestBuilder with itemDraftLoader {
    override def getCall(draftId: DraftId): Call = DraftEditor.load(draftId.toIdString)
  }

  class user_editItemLoader extends userAndItem with SessionRequestBuilder with itemDraftLoader with SecureSocialHelpers {
    override lazy val draftName = user.userName
    override def getCall(draftId: DraftId): Call = DraftEditor.load(draftId.toIdString)

    override def organization: Organization = Organization.findOneById(user.org.orgId).get
  }

  class clientIdAndPlayerToken_editItemLoader(val playerToken: String, val skipDecryption: Boolean = true) extends clientIdAndPlayerToken with IdAndPlayerTokenRequestBuilder with itemDraftLoader {
    override def getCall(draftId: DraftId): Call = {
      DraftEditor.load(draftId.toIdString)
    }
  }
}
