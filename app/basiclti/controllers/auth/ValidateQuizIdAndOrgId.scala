package basiclti.controllers.auth

import org.bson.types.ObjectId
import play.api.mvc._
import player.controllers.auth.Authenticate
import player.rendering.PlayerCookieReader
import scala.Some


/**
 * Builds an action using the given block, if the query returns true.
 * The query receives the orgId and the quizId
 * @tparam REQUEST - The request type to pass into the block
 */
abstract class ValidateQuizIdAndOrgId[REQUEST <: Request[AnyContent]]
  extends Authenticate[(String, String) => Boolean, AnyContent, REQUEST]
  with PlayerCookieReader {

  type OrgIdAndQuizIdAreValid = (String, String) => Boolean

  /** Build the request to be passed into the block */
  def makeRequest(orgId: ObjectId, r: Request[AnyContent]): Option[REQUEST]

  import play.api.mvc.Results._

  def OrgAction(query: OrgIdAndQuizIdAreValid)(block: (REQUEST) => Result): Action[AnyContent] =
    OrgAction(play.api.mvc.BodyParsers.parse.anyContent)(query)(block)

  def OrgAction(p: BodyParser[AnyContent])(query: OrgIdAndQuizIdAreValid)(block: (REQUEST) => Result): Action[AnyContent] = {
    Action {
      request =>
        request.session.get(LtiCookieKeys.QUIZ_ID) match {
          case Some(quizId) => {
            orgIdFromCookie(request) match {
              case Some(orgId) => {
                if (query(quizId, orgId)) {
                  makeRequest(new ObjectId(orgId), request) match {
                    case Some(r) => block(r)
                    case _ => BadRequest("Error generating a request")
                  }
                } else {
                  Unauthorized("")
                }
              }
            }
          }
        }
    }
  }

}
