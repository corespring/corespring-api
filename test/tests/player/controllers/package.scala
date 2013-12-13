package tests.player

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.player.accessControl.auth.TokenizedRequestActionBuilder
import org.corespring.player.accessControl.auth.requests.TokenizedRequest
import org.corespring.player.accessControl.models.RequestedAccess
import play.api.mvc._
import scala.concurrent.Future

package object controllers {

  class TestBuilder extends TokenizedRequestActionBuilder[RequestedAccess] {
    def ValidatedAction(access: RequestedAccess)(block: (TokenizedRequest[AnyContent]) => Future[SimpleResult]) =
      ValidatedAction(play.api.mvc.BodyParsers.parse.anyContent)(access)(block)

    def ValidatedAction(p: BodyParser[AnyContent])(access: RequestedAccess)(block: (TokenizedRequest[AnyContent]) => Future[SimpleResult]): Action[AnyContent] = Action.async {
      request =>
        block(TokenizedRequest("test_token", request))
    }
  }

  object TestIds{
    val testId = VersionedId(new ObjectId("50b653a1e4b0ec03f29344b0"))
    val testSessionId = new ObjectId("51116bc7a14f7b657a083c1d")
    val testQuizId = new ObjectId("000000000000000000000001")
    val testQuizItemId = VersionedId(new ObjectId("5153eee1aa2eefdc1b7a5570"))
  }
}
