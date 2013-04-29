package player.accessControl.models

import org.bson.types.ObjectId
import player.accessControl.models.RequestedAccess.Mode

trait ItemLookup {
  def containsItem(id: ObjectId, itemId: ObjectId): Boolean
}

trait QuizItemLookup extends ItemLookup

trait SessionItemLookup extends ItemLookup

class AccessGranter(sessionLookup: SessionItemLookup, quizLookup: QuizItemLookup) {

  def grantAccess(currentMode: Option[Mode.Mode], request: RequestedAccess, options: RenderOptions): Boolean = {

    val mode: Option[Mode.Mode] = if (request.mode.isDefined) request.mode else currentMode

    def grantAggregateAccess: Boolean = request match {
      case RequestedAccess(Some(ContentRequest(itemId, _)), None, Some(ContentRequest(assessmentId, _)), _) => {
        if (options.assessmentId == RenderOptions.*)
          true
        else {
          oid(options.assessmentId).map {
            aid =>
              aid == assessmentId && quizLookup.containsItem(aid, itemId)
          }.getOrElse(false)
        }
      }
      case _ => false
    }

    def allowItemId(itemId: ObjectId): Boolean = if (options.itemId == RenderOptions.*) {
      true
    } else {
      options.itemId == itemId.toString
    }

    def grantPreviewAccess: Boolean = request match {
      case RequestedAccess(Some(ContentRequest(itemId, _)), None, None, _) => allowItemId(itemId)
      case RequestedAccess(Some(ContentRequest(itemId, _)), Some(ContentRequest(sessionId, _)), None, _) => {
        allowItemId(itemId) && allowSessionId(sessionId)
      }
      case _ => false
    }

    def allowSessionId(sessionId: ObjectId): Boolean = {

      def allowByItemId: Boolean = {
        if (options.itemId == RenderOptions.*) {
          true
        } else {
          oid(options.itemId).map {
            o => sessionLookup.containsItem(sessionId, o)
          }.getOrElse(false)
        }
      }

      if (options.sessionId == RenderOptions.*)
        allowByItemId
      else
        allowByItemId && options.sessionId == sessionId.toString
    }

    def grantRenderAccess: Boolean = request match {
      case RequestedAccess(None, Some(ContentRequest(sessionId, _)), None, _) => allowSessionId(sessionId)
      case _ => false
    }

    def grantAdministerAccess: Boolean = request match {
      //request by itemid
      case RequestedAccess(Some(ContentRequest(itemId, _)), None, None, _) => allowItemId(itemId)
      //request by sessionId
      case RequestedAccess(None, Some(ContentRequest(sessionId, _)), None, _) => allowSessionId(sessionId)
      //request by both?
      case RequestedAccess(Some(ContentRequest(itemId, _)), Some(ContentRequest(sessionId, _)), None, _) => allowSessionId(sessionId)
      case _ => false
    }

    mode.map {
      m => m match {
        case Mode.Aggregate => grantAggregateAccess
        case Mode.Preview => grantPreviewAccess
        case Mode.Render => grantRenderAccess
        case Mode.Administer => grantAdministerAccess
      }
    }.getOrElse(false)
  }

  private def oid(s: String) = try {
    Some(new ObjectId(s))
  } catch {
    case e: Throwable => None
  }
}
