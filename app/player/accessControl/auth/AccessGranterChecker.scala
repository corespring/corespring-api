package player.accessControl.auth

import player.accessControl.models._
import org.bson.types.ObjectId
import models.itemSession.ItemSession
import models.quiz.basic.Quiz
import player.accessControl.models.RequestedAccess.Mode
import scala.Left
import scala.Some
import scala.Right
import controllers.InternalError
import player.accessControl.models.granter._
import controllers.InternalError
import scala.Left
import scala.Some
import scala.Right
import play.api.Logger

object AccessGranterChecker extends CheckPlayerSession {

  val sessionLookup: SessionItemLookup = new SessionItemLookup {
    def containsItem(id: ObjectId, itemId: ObjectId): Boolean = {
      ItemSession.findOneById(id) match {
        case Some(s) => s.itemId == itemId
        case _ => false
      }
    }
  }

  val quizLookup: QuizItemLookup = new QuizItemLookup {
    def containsItem(id: ObjectId, itemId: ObjectId): Boolean = {
      Quiz.findOneById(id) match {
        case Some(q) => q.questions.exists(_.itemId == itemId)
        case _ => false
      }
    }
  }

  val granter: ConstraintGranter = new ConstraintGranter(sessionLookup, quizLookup)

  override def grantAccess(activeMode: Option[Mode.Mode], a: RequestedAccess, o: RenderOptions): Either[InternalError, Boolean] = {
    if (granter.grant(activeMode, a, o))
      Right(true)
    else{
      val failedConstraints = granter.getFailedConstraints(activeMode, a, o)
      Logger.warn( failedConstraints.mkString(", \n"))
      Left(InternalError("Access denied: " + failedConstraints.mkString(",\n")))
    }
  }
}
