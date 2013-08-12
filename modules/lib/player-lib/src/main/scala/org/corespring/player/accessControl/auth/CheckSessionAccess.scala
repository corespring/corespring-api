package org.corespring.player.accessControl.auth

import org.bson.types.ObjectId
import org.corespring.common.log.PackageLogging
import org.corespring.platform.core.models.error.InternalError
import org.corespring.platform.core.models.itemSession.{ItemSessionCompanion, PreviewItemSessionCompanion, DefaultItemSession}
import org.corespring.platform.core.services.quiz.basic.QuizService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.player.accessControl.models.RequestedAccess.Mode
import org.corespring.player.accessControl.models.granter.{QuizItemLookup, SessionItemLookup, ConstraintGranter}
import org.corespring.player.accessControl.models.{RenderOptions, RequestedAccess}


/** Delegate calls to grantAccess to the ConstraintGranter.
 */
object CheckSessionAccess extends CheckSession with PackageLogging{

  val sessionLookup: SessionItemLookup = new SessionItemLookup {

    /** Note: We check both the normal collection and the preview item session collection */
    def containsItem(id: ObjectId, itemId: VersionedId[ObjectId]): Boolean = {
      contains(DefaultItemSession, id, itemId) || contains(PreviewItemSessionCompanion, id, itemId)
    }

    private def contains(companion:ItemSessionCompanion, id:ObjectId, itemId:VersionedId[ObjectId]) : Boolean = {
      companion.findOneById(id) match {
        case Some(s) => {
          Logger.debug("SessionItemLookup:contains: companion: " + companion + " itemId:" + s.itemId + " searched for: " + itemId)
          s.itemId == itemId
        }
        case _ => false
      }
    }
  }

  val quizLookup: QuizItemLookup = new QuizItemLookup {
    def containsItem(id: ObjectId, itemId: VersionedId[ObjectId]): Boolean = {
      QuizService.findOneById(id) match {
        case Some(q) => q.questions.exists(_.itemId == itemId)
        case _ => false
      }
    }
  }

  /** a ConstraintGranter with a session and quiz lookup implementation */
  val granter: ConstraintGranter = new ConstraintGranter(sessionLookup, quizLookup)

  /** This implementation delegates to <link>org.corespring.player.accessControl.models.granter.ConstraintGranter</link>
   */
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
