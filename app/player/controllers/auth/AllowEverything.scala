package player.controllers.auth

import play.api.mvc._
import player.models.TokenizedRequest
import com.mongodb.casbah.commons.MongoDBObject
import models.Organization
import models.auth.AccessToken

/** An impl of Authenticate that creates a tokenized request for the Root Corespring Org - effectively a pass through */
object AllowEverything extends Authenticate[AnyContent] {

  def OrgAction(access:RequestedAccess)(block: TokenizedRequest[AnyContent] => Result): Action[AnyContent] = OrgAction(BodyParsers.parse.anyContent)(access)(block)

  def OrgAction(p: BodyParser[AnyContent])(access:RequestedAccess)(block: TokenizedRequest[AnyContent] => Result): Action[AnyContent] = {
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
