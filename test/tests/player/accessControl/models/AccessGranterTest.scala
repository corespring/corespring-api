package tests.player.accessControl.models

import org.specs2.mutable.Specification
import player.accessControl.models._
import org.bson.types.ObjectId
import player.accessControl.models.RequestedAccess.Mode
import scala.Some

class AccessGranterTest extends Specification {

  def sessionLookup(contains: Boolean): SessionItemLookup = new SessionItemLookup {
    def containsItem(id: ObjectId, itemId: ObjectId): Boolean = contains
  }

  def quizLookup(contains: Boolean): QuizItemLookup = new QuizItemLookup {
    def containsItem(id: ObjectId, itemId: ObjectId): Boolean = contains
  }

  def sOid = Some(new ObjectId())

  "access granter" should {
    "when in render" in {

      "and not bound to an itemId" in {

        "no access unless only a sessionId is defined" in {
          val options = RenderOptions(expires = 0, mode = Mode.Render, sessionId = RenderOptions.*)
          new AccessGranter(sessionLookup(true), quizLookup(true)).grantAccess(Some(Mode.Render), RequestedAccess.asRead(), options) === false
          new AccessGranter(sessionLookup(true), quizLookup(true)).grantAccess(Some(Mode.Render), RequestedAccess.asRead(sessionId = sOid, itemId = sOid), options) === false
          new AccessGranter(sessionLookup(true), quizLookup(true)).grantAccess(Some(Mode.Render), RequestedAccess.asRead(sessionId = sOid, assessmentId = sOid), options) === false
          new AccessGranter(sessionLookup(true), quizLookup(true)).grantAccess(Some(Mode.Render), RequestedAccess.asRead(sessionId = sOid), options) === true
        }
      }

      "and bound to a sessionId AND an itemId" in {
        val sessionId = new ObjectId()
        val itemId = new ObjectId()
        val options = RenderOptions(expires = 0, mode = Mode.Render, sessionId = sessionId.toString, itemId = itemId.toString)

        "only allow access to the session if it contains that item id" in {
          new AccessGranter(sessionLookup(true), quizLookup(true)).grantAccess(Some(Mode.Render), RequestedAccess.asRead( sessionId = Some(sessionId)), options) === true
          new AccessGranter(sessionLookup(false), quizLookup(true)).grantAccess(Some(Mode.Render), RequestedAccess.asRead( sessionId = Some(sessionId)), options) === false
        }

      }
      "and bound only to a sessionId" in {
        val sessionId = new ObjectId()
        val options = RenderOptions(expires = 0, mode = Mode.Render, sessionId = sessionId.toString)

        "only allow access to the session if it contains that item id" in {
          new AccessGranter(sessionLookup(true), quizLookup(true)).grantAccess(Some(Mode.Render), RequestedAccess.asRead( sessionId = Some(sessionId)), options) === true
          new AccessGranter(sessionLookup(true), quizLookup(true)).grantAccess(Some(Mode.Render), RequestedAccess.asRead( sessionId = sOid), options) === false
        }

      }
      "and bound only to an itemId" in {
        val itemId = new ObjectId()
        val options = RenderOptions(expires = 0, mode = Mode.Render, itemId = itemId.toString, sessionId = RenderOptions.*)
        "only allow access to the session if it contains that item id" in {
          new AccessGranter(sessionLookup(true), quizLookup(true)).grantAccess(Some(Mode.Render), RequestedAccess.asRead( sessionId = sOid), options) === true
          new AccessGranter(sessionLookup(false), quizLookup(true)).grantAccess(Some(Mode.Render), RequestedAccess.asRead( sessionId = sOid), options) === false
        }
      }
    }


    "when in preview" in {
      val granter = new AccessGranter(sessionLookup(true), quizLookup(true))

      "no access when no item id is specified" in {
        val options = RenderOptions(expires = 0, mode = Mode.Preview, itemId = RenderOptions.*)
        val request = RequestedAccess.asRead()
        granter.grantAccess(Some(Mode.Preview), request, options) === false
      }

      "allow access if item id is specified and options has *" in {
        val options = RenderOptions(expires = 0, mode = Mode.Preview, itemId = RenderOptions.*)
        val request = RequestedAccess.asRead(itemId = Some(new ObjectId()))
        granter.grantAccess(Some(Mode.Preview), request, options) === true
      }

      val oid = new ObjectId()

      "allow access if item id is specified and options has specificId" in {
        val options = RenderOptions(expires = 0, mode = Mode.Preview, itemId = oid.toString)
        val request = RequestedAccess.asRead(itemId = Some(oid))
        granter.grantAccess(Some(Mode.Preview), request, options) === true
      }

      "no access if item doesn't match specific id" in {
        val options = RenderOptions(expires = 0, mode = Mode.Preview, itemId = oid.toString)
        val request = RequestedAccess.asRead(itemId = Some(new ObjectId()))
        granter.grantAccess(Some(Mode.Preview), request, options) === false
      }
    }

    "when in aggregate" in {

      def aggregateOptions(assessmentId: Option[String] = None): RenderOptions = assessmentId match {
        case Some(id) =>
          RenderOptions(assessmentId = id, expires = 0, mode = Mode.Aggregate)
        case _ =>
          RenderOptions(expires = 0, mode = Mode.Aggregate)
      }

      "and a session is requested" in {

        "don't allow access" in {
          val quizOne = aggregateOptions()
          val request = RequestedAccess.asRead(sessionId = Some(new ObjectId("000000000000000000000001")))
          new AccessGranter(sessionLookup(true), quizLookup(true)).grantAccess(Some(Mode.Aggregate), request, quizOne) === false
        }
      }

      "and when not bound to a quiz" in {
        "allow access to any item" in {
          val quizOne = aggregateOptions()
          val request = RequestedAccess.asRead(assessmentId = Some(new ObjectId()), itemId = Some(new ObjectId()))
          new AccessGranter(sessionLookup(true), quizLookup(true)).grantAccess(Some(Mode.Aggregate), request, quizOne) === true
          new AccessGranter(sessionLookup(false), quizLookup(false)).grantAccess(Some(Mode.Aggregate), request, quizOne) === true

        }
      }

      "and when bound to a quiz" in {

        val quizId = "000000000000000000000001"
        val quizOne = aggregateOptions(Some(quizId))

        "only allow access to that quiz id" in {
          val request = RequestedAccess.asRead(assessmentId = Some(new ObjectId()), itemId = Some(new ObjectId("000000000000000000000001")))
          new AccessGranter(sessionLookup(true), quizLookup(true)).grantAccess(Some(Mode.Aggregate), request, quizOne) === false
          new AccessGranter(sessionLookup(true), quizLookup(false)).grantAccess(Some(Mode.Aggregate), request, quizOne) === false
        }

        "only allow access to an item bound to that quiz" in {
          val request = RequestedAccess.asRead(assessmentId = Some(new ObjectId(quizId)), itemId = Some(new ObjectId("000000000000000000000001")))
          new AccessGranter(sessionLookup(true), quizLookup(true)).grantAccess(Some(Mode.Aggregate), request, quizOne) === true
          new AccessGranter(sessionLookup(true), quizLookup(false)).grantAccess(Some(Mode.Aggregate), request, quizOne) === false
        }
      }
    }
  }

}
