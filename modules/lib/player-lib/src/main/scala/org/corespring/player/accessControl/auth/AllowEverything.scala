package org.corespring.player.accessControl.auth

import com.mongodb.casbah.commons.MongoDBObject
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.auth.AccessToken
import org.corespring.player.accessControl.auth.requests.TokenizedRequest
import org.corespring.player.accessControl.models.RequestedAccess
import play.api.mvc._
import scala.concurrent.Future


/** An impl of ActionBuilder that creates a tokenized request for the Root Corespring Org - effectively a pass through */
object AllowEverything extends TokenizedRequestActionBuilder[RequestedAccess] {

  def ValidatedAction(access: RequestedAccess)(block: TokenizedRequest[AnyContent] => Future[SimpleResult]): Action[AnyContent] = ValidatedAction(BodyParsers.parse.anyContent)(access)(block)

  def ValidatedAction(p: BodyParser[AnyContent])(access: RequestedAccess)(block: TokenizedRequest[AnyContent] => Future[SimpleResult]): Action[AnyContent] = {
    Action.async(p) {
      request =>
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
