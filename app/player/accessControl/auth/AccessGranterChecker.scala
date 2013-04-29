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

object AccessGranterChecker extends CheckPlayerSession{

  val sessionLookup : SessionItemLookup = new SessionItemLookup {
    def containsItem(id: ObjectId, itemId: ObjectId): Boolean = {
      ItemSession.findOneById(id) match {
        case Some(s) => s.itemId == itemId
        case _ => false
      }
    }
  }

  val quizLookup : QuizItemLookup = new QuizItemLookup {
    def containsItem(id: ObjectId, itemId: ObjectId): Boolean = {
      Quiz.findOneById(id) match {
        case Some(q) => q.questions.exists(_.itemId == itemId)
        case _ => false
      }
    }
  }

  val granter : AccessGranter = new AccessGranter(sessionLookup,quizLookup)

  override def grantAccess(activeMode: Option[Mode.Mode], a: RequestedAccess, o: RenderOptions): Either[InternalError, Boolean] = {
    if(granter.grantAccess(activeMode, a, o))
      Right(true)
    else
      Left(InternalError("Access denied"))
  }
}
