package org.corespring.v2.player

import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.{ IntegrationHelpers, SecureSocialHelper }
import org.corespring.it.scopes._
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.identifiers.WithRequestIdentitySequence
import org.corespring.v2.auth.models.PlayerAccessSettings
import org.corespring.v2.errors.Errors.generalError
import play.api.libs.json.Json
import play.api.mvc._

class LoadCatalogIntegrationTest
  extends IntegrationSpecification
  with IntegrationHelpers {

  import org.corespring.container.client.controllers.apps.routes.Catalog

  override protected val logger = Logger("it.load-catalog")

  "showing catalog" should {

    "fail if I'm not authorized" in new unknownUser_catalogLoader() {
      status(result) === UNAUTHORIZED
      val err = generalError(WithRequestIdentitySequence.errorMessage, UNAUTHORIZED)
      contentAsString(result) === org.corespring.container.client.views.html.error.main(err.statusCode, err.message, false).toString
    }

    "work if authorized with an access token" in new token_catalogLoader() {
      status(result) === OK
    }

    "work if authorized as a logged in user" in new user_catalogLoader() {
      status(result) === OK
    }

    "fail if there are bad options" in new clientIdAndPlayerToken_catalogLoader("let me in") {
      status(result) === UNAUTHORIZED
    }

    "work if the options are good" in new clientIdAndPlayerToken_catalogLoader(Json.stringify(Json.toJson(PlayerAccessSettings.ANYTHING))) {
      status(result) === OK
    }

  }

  class unknownUser_catalogLoader extends orgWithAccessTokenAndItem with PlainRequestBuilder with itemLoader {
    override def getCall(itemId: VersionedId[ObjectId]): Call = Catalog.load(itemId.toString)
  }

  class token_catalogLoader extends orgWithAccessTokenAndItem with TokenRequestBuilder with itemLoader {
    override def getCall(itemId: VersionedId[ObjectId]): Call = Catalog.load(itemId.toString)
  }

  class user_catalogLoader extends userAndItem with SessionRequestBuilder with itemLoader with SecureSocialHelper {
    override def getCall(itemId: VersionedId[ObjectId]): Call = Catalog.load(itemId.toString)
  }

  class clientIdAndPlayerToken_catalogLoader(val playerToken: String, val skipDecryption: Boolean = true) extends clientIdAndPlayerToken with IdAndPlayerTokenRequestBuilder with itemLoader {
    override def getCall(itemId: VersionedId[ObjectId]): Call = Catalog.load(itemId.toString)
  }
}
