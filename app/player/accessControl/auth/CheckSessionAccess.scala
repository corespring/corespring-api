package player.accessControl.auth

import controllers.InternalError
import models.itemSession.{PreviewItemSessionCompanion, ItemSessionCompanion, DefaultItemSession, ItemSession}
import models.quiz.basic.Quiz
import org.bson.types.ObjectId
import play.api.Logger
import player.accessControl.models.RequestedAccess.Mode
import player.accessControl.models._
import player.accessControl.models.granter._
import scala.Left
import scala.Right
import scala.Some

object CheckSessionAccess extends CheckSession {

  val sessionLookup: SessionItemLookup = new SessionItemLookup {

    /** Note: We check both the normal collection and the preview item session collection */
    def containsItem(id: ObjectId, itemId: ObjectId): Boolean = {
      contains(DefaultItemSession, id, itemId) || contains(PreviewItemSessionCompanion, id, itemId)
    }

    private def contains(companion:ItemSessionCompanion, id:ObjectId, itemId:ObjectId) : Boolean = {
      companion.findOneById(id) match {
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
    else {
      val failedConstraints = granter.getFailedConstraints(activeMode, a, o)

      Logger.warn("Access was refused. The following constraints prevented access: \n " + failedConstraints.mkString(", \n"))
      Left(InternalError("Access denied: " + failedConstraints.mkString(",\n")))
    }
  }
}
