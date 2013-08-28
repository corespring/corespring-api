package basiclti.accessControl.auth

import basiclti.accessControl.auth.cookies.LtiCookieKeys
import org.bson.types.ObjectId
import org.corespring.player.accessControl.auth.ActionBuilder
import org.corespring.player.accessControl.cookies.PlayerCookieReader
import play.api.mvc.{ Request, AnyContent, Result, Action, BodyParser }

/**
 * Builds an action using the given block, if the query returns true.
 * The query receives the orgId and the quizId
 * @tparam REQUEST - The request type to pass into the block
 */
abstract class ValidateQuizIdAndOrgId[REQUEST <: Request[AnyContent]]
  extends ActionBuilder[(String, String) => Boolean, AnyContent, REQUEST]
  with PlayerCookieReader {

  type OrgIdAndQuizIdAreValid = (String, String) => Boolean

  /** Build the request to be passed into the block */
  def makeRequest(orgId: ObjectId, r: Request[AnyContent]): Option[REQUEST]

  import play.api.mvc.Results._

  def ValidatedAction(query: OrgIdAndQuizIdAreValid)(block: (REQUEST) => Result): Action[AnyContent] =
    ValidatedAction(play.api.mvc.BodyParsers.parse.anyContent)(query)(block)

  def ValidatedAction(p: BodyParser[AnyContent])(query: OrgIdAndQuizIdAreValid)(block: (REQUEST) => Result): Action[AnyContent] = Action {
    request =>
      {
        for {
          quizId <- request.session.get(LtiCookieKeys.QUIZ_ID)
          orgId <- orgIdFromCookie(request)
          if (query(quizId, orgId))
          r <- makeRequest(new ObjectId(orgId), request)
        } yield block(r)
      }.getOrElse(Unauthorized("You are not authorized"))
  }

}
