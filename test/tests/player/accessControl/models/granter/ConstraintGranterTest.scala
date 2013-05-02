package tests.player.accessControl.models.granter

import org.specs2.mutable.Specification
import player.accessControl.models.{RequestedAccess, RenderOptions}
import player.accessControl.models.RequestedAccess.Mode
import player.accessControl.models.RequestedAccess.Mode._
import player.accessControl.models.granter.{QuizItemLookup, SessionItemLookup, ConstraintGranter}
import org.bson.types.ObjectId
import org.specs2.execute.Success
import org.specs2.matcher.MatchResult

class ConstraintGranterTest extends Specification {

  def sessionLookup(contains: Boolean): SessionItemLookup = new SessionItemLookup {
    def containsItem(id: ObjectId, itemId: ObjectId): Boolean = contains
  }

  def yesSession = sessionLookup(contains = true)

  def noSession = sessionLookup(contains = false)

  def yesQuiz = quizLookup(contains = true)

  def noQuiz = quizLookup(contains = false)

  def quizLookup(contains: Boolean): QuizItemLookup = new QuizItemLookup {
    def containsItem(id: ObjectId, itemId: ObjectId): Boolean = contains
  }

  def granter(session: SessionItemLookup = yesSession, quiz: QuizItemLookup = yesQuiz) = new ConstraintGranter(session, quiz)

  private def ra(m: Mode, itemId: Option[ObjectId] = None, sessionId: Option[ObjectId] = None, assessmentId: Option[ObjectId] = None): RequestedAccess = {
    RequestedAccess.asRead(mode = Some(m), itemId = itemId, sessionId = sessionId, assessmentId = assessmentId)
  }

  private def oidString(id: Int): String = "00000000000000000000000" + id

  def sOid(s: String = oidString(1)) = Some(new ObjectId(s))


  def randomId = Some(new ObjectId())

  import org.specs2.execute.{Result => SpecsResult}

  case class GrantAssert(r: RequestedAccess, expected: Boolean, session: SessionItemLookup = yesSession, quiz: QuizItemLookup = yesQuiz)

  def assertGrant(options: RenderOptions, mode: Mode, assertions: GrantAssert*): SpecsResult = {

    val results: Seq[MatchResult[Any]] = assertions.map {
      (a) =>
        granter(a.session, a.quiz).grant(a.r, options) === a.expected
    }
    results.filterNot(m => m.isSuccess).length === 0
  }

  "constraint granter" should {

    "when in administer" in {


      "session wildcard, item wildcard" in {
        val o = RenderOptions(itemId = RenderOptions.*, sessionId = RenderOptions.*, expires = 0, mode = Administer)

        assertGrant(o, Administer,
          GrantAssert(ra(Administer, itemId = randomId), true),
          GrantAssert(ra(Administer, sessionId = randomId), true),
          GrantAssert(ra(Administer, itemId = randomId, sessionId = randomId), true)
        )
      }

      "session bound, item wildcard" in {
        val sessionId = oidString(1)
        val o = RenderOptions(itemId = RenderOptions.*, sessionId = sessionId, expires = 0, mode = Administer)
        assertGrant(o, Administer,
          GrantAssert(ra(Administer, itemId = randomId), true),
          GrantAssert(ra(Administer, sessionId = randomId), false),
          GrantAssert(ra(Administer, sessionId = sOid(sessionId)), true),
          GrantAssert(ra(Administer, itemId = randomId, sessionId = sOid(sessionId)), true)
        )
      }

      "item bound, session wildcard" in {
        val itemId = oidString(1)
        val o = RenderOptions(itemId = itemId, sessionId = RenderOptions.*, expires = 0, mode = Administer)
        assertGrant(o, Administer,
          GrantAssert(ra(Administer, itemId = randomId), false),
          GrantAssert(ra(Administer, itemId = sOid(itemId)), true),
          GrantAssert(ra(Administer, sessionId = randomId), true),
          GrantAssert(ra(Administer, sessionId = randomId), false, session = noSession)
        )
      }

      "item bound, session bound" in {
        val itemId = oidString(1)
        val sessionId = oidString(2)

        val o = RenderOptions(itemId = itemId, sessionId = sessionId, expires = 0, mode = Administer)
        granter().grant(ra(Administer, itemId = randomId), o) === false
        granter().grant(ra(Administer, itemId = sOid(itemId)), o) === true
        granter().grant(ra(Administer, sessionId = randomId), o) === false
        granter(noSession).grant(ra(Administer, sessionId = randomId), o) === false
        granter(noSession).grant(ra(Administer, sessionId = sOid(sessionId)), o) === false
        granter().grant(ra(Administer, sessionId = sOid(sessionId)), o) === true
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
          granter().grant(ra(Aggregate, sessionId = sOid()), quizOne) === false
        }
      }

      "and when not bound to a quiz" in {
        "allow access to any item" in {
          val quizOne = aggregateOptions()
          granter().grant(ra(Aggregate, assessmentId = randomId, itemId = randomId), quizOne) === true
          granter(noSession, noQuiz).grant(ra(Aggregate, assessmentId = randomId, itemId = randomId), quizOne) === true
        }
      }

      "and when bound to a quiz" in {

        val quizId = "000000000000000000000001"
        val itemId = "000000000000000000000002"

        "only allow access to that quiz id" in {
          val quizOne = aggregateOptions(Some(quizId))
          granter().grant(ra(Aggregate, assessmentId = randomId, itemId = sOid()), quizOne) === false
          granter(quiz = noQuiz).grant(ra(Aggregate, assessmentId = randomId, itemId = sOid()), quizOne) === false
        }

        "bound quiz id, bound item id" in {
          val ro = RenderOptions(assessmentId = quizId, itemId = itemId, expires = 0, mode = Mode.Aggregate)
          val request = ra(Aggregate, assessmentId = sOid(quizId), itemId = sOid(itemId))
          granter().grant(request, ro) === true
          granter(quiz = noQuiz).grant(request, ro) === false

          val request2 = ra(Aggregate, assessmentId = sOid(quizId), itemId = randomId)
          granter().grant(request2, ro) === false
          granter(quiz = noQuiz).grant(request2, ro) === false
        }

        "wildcard quiz id - bound item id" in {
          val ro = RenderOptions(assessmentId = "*", itemId = itemId, expires = 0, mode = Mode.Aggregate)
          val request = ra(Aggregate, assessmentId = randomId, itemId = sOid(itemId))
          granter(quiz = noQuiz).grant(request, ro) === false
          granter().grant(request, ro) === true
        }
      }
    }


    "when in preview" in {

      "no access when no item id is specified" in {
        val options = RenderOptions(expires = 0, mode = Mode.Preview, itemId = RenderOptions.*)
        val request = ra(Preview)
        granter().grant(request, options) === false
      }

      "allow access if item id is specified and options has *" in {
        val options = RenderOptions(expires = 0, mode = Mode.Preview, itemId = RenderOptions.*)
        val request = ra(Preview, itemId = randomId)
        granter().grant(request, options) === true
      }

      val oid = new ObjectId()

      "allow access if item id is specified and options has specificId" in {
        val options = RenderOptions(expires = 0, mode = Mode.Preview, itemId = oid.toString)
        val request = ra(Preview, itemId = Some(oid))
        granter().grant(request, options) === true
      }

      "no access if item doesn't match specific id" in {
        val options = RenderOptions(expires = 0, mode = Mode.Preview, itemId = oid.toString)
        val request = ra(Preview, itemId = randomId)
        granter().grant(request, options) === false
      }

      "access if item and session id are specified" in {

        val options = RenderOptions(expires = 0, mode = Mode.Preview, itemId = oid.toString, sessionId = oid.toString)
        granter().grant(ra(Preview, itemId = randomId), options) === false
        granter().grant(ra(Preview, sessionId = randomId), options) === false
        granter().grant(ra(Preview, itemId = randomId, sessionId = randomId), options) === false
        granter().grant(ra(Preview, itemId = Some(oid)), options) === true
        granter().grant(ra(Preview, sessionId = Some(oid)), options) === false
        granter().grant(ra(Preview, itemId = Some(oid), sessionId = Some(oid)), options) === true
      }
    }

    "when in render" in {

      "and not bound to an itemId" in {

        "no access if a session isn't defined" in {
          val options = RenderOptions(expires = 0, mode = Mode.Render, sessionId = RenderOptions.*)
          granter().grant(ra(Render), options) === false
          granter(noSession).grant(ra(Render, sessionId = randomId), options) === true
        }
      }

      "and bound to a sessionId AND an itemId" in {
        val sessionId = new ObjectId()
        val itemId = new ObjectId()
        val options = RenderOptions(expires = 0, mode = Mode.Render, sessionId = sessionId.toString, itemId = itemId.toString)

        "only allow access to the session if it contains that item id" in {
          granter().grant(ra(Render, sessionId = Some(sessionId)), options) === true
          granter(noSession).grant(ra(Render, sessionId = Some(sessionId)), options) === false
          granter(noSession).grant(ra(Render, sessionId = randomId), options) === false
        }

      }
      "and bound only to a sessionId" in {
        val sessionId = new ObjectId()
        val options = RenderOptions(expires = 0, mode = Mode.Render, sessionId = sessionId.toString)

        "only allow access to the session if it contains that item id" in {
          granter().grant(ra(Render, sessionId = Some(sessionId)), options) === true
          granter().grant(ra(Render, sessionId = randomId), options) === false
        }

      }
      "and bound only to an itemId" in {
        val itemId = new ObjectId()
        val options = RenderOptions(expires = 0, mode = Mode.Render, itemId = itemId.toString, sessionId = RenderOptions.*)
        "only allow access to the session if it contains that item id" in {
          granter().grant(ra(Render, sessionId = randomId), options) === true
          granter(noSession).grant(ra(Render, sessionId = randomId), options) === false
        }
      }
    }
  }
}
