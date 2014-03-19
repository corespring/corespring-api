package org.corespring.lti.web.accessControl.auth

import org.bson.types.ObjectId
import org.corespring.lti.web.accessControl.cookies.LtiCookieKeys
import org.corespring.player.accessControl.auth.ActionBuilder
import org.corespring.player.accessControl.cookies.PlayerCookieReader
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Builds an action using the given block, if the query returns true.
 * The query receives the orgId and the quizId
 * @tparam REQUEST - The request type to pass into the block
 */
abstract class ValidateAssessmentIdAndOrgId[REQUEST <: Request[AnyContent]]
  extends ActionBuilder[(String, String) => Boolean, AnyContent, REQUEST]
  with PlayerCookieReader {

  import ExecutionContext.Implicits.global
  import play.api.mvc.Results._

  type OrgIdAndAssessmentIdAreValid = (String, String) => Boolean

  /** Build the request to be passed into the block */
  def makeRequest(orgId: ObjectId, r: Request[AnyContent]): Option[REQUEST]


  def ValidatedAction(query: OrgIdAndAssessmentIdAreValid)(block: (REQUEST) => Future[SimpleResult]): Action[AnyContent] =
    ValidatedAction(play.api.mvc.BodyParsers.parse.anyContent)(query)(block)

  def ValidatedAction(p: BodyParser[AnyContent])(query: OrgIdAndAssessmentIdAreValid)(block: (REQUEST) => Future[SimpleResult]) = Action.async {
    request =>
      {
        for {
          quizId <- request.session.get(LtiCookieKeys.ASSESSMENT_ID)
          orgId <- orgIdFromCookie(request)
          if (query(quizId, orgId))
          r <- makeRequest(new ObjectId(orgId), request)
        } yield block(r)
      }.getOrElse(Future(Unauthorized("You are not authorized")))
  }

}
