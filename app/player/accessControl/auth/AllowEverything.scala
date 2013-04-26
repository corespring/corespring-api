package player.accessControl.auth

import com.mongodb.casbah.commons.MongoDBObject
import controllers.auth.requests.TokenizedRequest
import models.Organization
import models.auth.AccessToken
import play.api.mvc._
import player.accessControl.models.RequestedAccess
import controllers.auth.TokenizedRequestActionBuilder

/** An impl of ActionBuilder that creates a tokenized request for the Root Corespring Org - effectively a pass through */
object AllowEverything extends TokenizedRequestActionBuilder[RequestedAccess]{

  def ValidatedAction(access:RequestedAccess)(block: TokenizedRequest[AnyContent] => Result): Action[AnyContent] = ValidatedAction(BodyParsers.parse.anyContent)(access)(block)

  def ValidatedAction(p: BodyParser[AnyContent])(access:RequestedAccess)(block: TokenizedRequest[AnyContent] => Result): Action[AnyContent] = {
    Action(p) {
      request =>

        val renderOptions = request.session.get("renderOptions")
        println("renderOptions: ")
        println(renderOptions)
        Organization.findOne(MongoDBObject("name" -> "Corespring Organization")) match {
          case Some(org) => {
            val token = AccessToken.getTokenForOrg(org)
            block(TokenizedRequest(token.tokenId, request))
          }
          case _ => throw new RuntimeException("Can't find Corespring Organization")
        }
    }
  }
}
