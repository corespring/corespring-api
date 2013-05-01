package player.accessControl.models.granter

import org.bson.types.ObjectId
import player.accessControl.models.RequestedAccess.Mode._
import player.accessControl.models.granter.constraints._
import player.accessControl.models.{ContentRequest, RenderOptions, RequestedAccess}

/** An AccessGranter that creates a list of Constraints based on the rendering options */
class ConstraintGranter(sessionLookup: SessionItemLookup, quizLookup: QuizItemLookup) extends ConstraintChecker with AccessGranter {

  def grant(currentMode: Option[Mode], request: RequestedAccess, options: RenderOptions): Boolean = {
    val mode: Option[Mode] = if (request.mode.isDefined) request.mode else currentMode
    grant(request.copy(mode = mode), options)
  }

  /** Return a list of failing constraints - public as it is useful for debugging issues */
  def getFailedConstraints(currentMode: Option[Mode], request: RequestedAccess, options: RenderOptions): List[ValueAndConstraint[Any]] = {
    val mode: Option[Mode] = if (request.mode.isDefined) request.mode else currentMode
    getFailedConstraints(request.copy(mode = mode), options)
  }

  def getFailedConstraints(request: RequestedAccess, options: RenderOptions): List[ValueAndConstraint[Any]] = {
    val constraints = buildConstraints(request)(options)
    failedConstraints(constraints)
  }

  def grant(request: RequestedAccess, options: RenderOptions): Boolean = {
    val failedConstraints = getFailedConstraints(request, options)
    failedConstraints.length == 0
  }

  /** create the list of constraints that must return true for the 'valueWithinConstraints' method */
  private def buildConstraints(request: RequestedAccess)(implicit options: RenderOptions): List[ValueAndConstraint[Any]] = {

    val common: List[ValueAndConstraint[Any]] = List(timeConstraint)

    val modeSpecificConstraints: List[ValueAndConstraint[Any]] = request.mode.map {
      m => m match {
        case Preview => (itemAndSession orElse itemOnly orElse noMatch(Preview, "no item or session"))(request)
        case Render => (sessionOnly orElse noMatch(Render, "no session found"))(request)
        case Aggregate => (itemAndAssessment orElse noMatch(Aggregate, "only an assessment id and item are allowed"))(request)
        case Administer => (itemOnly orElse sessionOnly orElse itemAndSession orElse noMatch(Administer, "no item or session passed"))(request)
        case _ => List()
      }
    }.getOrElse(List(failed("mode", "No mode")))

    common ::: modeSpecificConstraints
  }

  private def itemAndAssessment(implicit options: RenderOptions): PartialFunction[RequestedAccess, List[ValueAndConstraint[Any]]] = {
    case RequestedAccess(Some(item), None, Some(assessment), _) => List(assessmentValueAndConstraints(assessment), itemValueAndConstraints(item))
  }

  private def itemAndSession(implicit options: RenderOptions): PartialFunction[RequestedAccess, List[ValueAndConstraint[Any]]] = {
    case RequestedAccess(Some(item), Some(session), _, _) => List(itemValueAndConstraints(item), sessionValueAndConstraints(session))
  }

  private def itemOnly(implicit options: RenderOptions): PartialFunction[RequestedAccess, List[ValueAndConstraint[Any]]] = {
    case RequestedAccess(Some(item), None, None, _) => List(itemValueAndConstraints(item))
  }

  private def sessionOnly(implicit options: RenderOptions): PartialFunction[RequestedAccess, List[ValueAndConstraint[Any]]] = {
    case RequestedAccess(None, Some(session), None, _) => List(sessionValueAndConstraints(session))
  }

  private def noMatch(key: Mode, msg: String): PartialFunction[RequestedAccess, List[ValueAndConstraint[Any]]] = {
    case _ => List(failed(key.toString, msg))
  }

  private def oid(s: String): Option[ObjectId] = try {
    Some(new ObjectId(s))
  } catch {
    case e: Throwable => None
  }

  private def makeOrFail(s: String, fn: (ObjectId => Constraint[Any]), failedMsg: String) = {
    oid(s).map {
      o => fn(o)
    }.getOrElse(new FailedConstraint(failedMsg))
  }


  private def boundConstraints(itemId: String, otherId: String, lookup: ItemLookup): List[Constraint[Any]] = {

    def itemIdToSessionConstraint: List[Constraint[Any]] = {
      if (itemId == RenderOptions.*) {
        List()
      }
      else {
        List(makeOrFail(itemId, new LookupContainsItemId(_, lookup), "invalid itemId in RenderOptions"))
      }
    }

    if (otherId == RenderOptions.*) {
      itemIdToSessionConstraint
    } else {
      itemIdToSessionConstraint ::: List(makeOrFail(otherId, new ObjectIdMatches(_), "invalid id"))
    }
  }

  private def sessionConstraints(session: ContentRequest)(implicit options: RenderOptions): List[Constraint[Any]] = {
    boundConstraints(options.itemId, options.sessionId, sessionLookup)
  }

  private def assessmentConstraints(assessment: ContentRequest)(implicit options: RenderOptions): List[Constraint[Any]] = {
    boundConstraints(options.itemId, options.assessmentId, quizLookup)
  }

  private def itemConstraints(item: ContentRequest)(implicit options: RenderOptions): List[Constraint[Any]] = {
    if (options.itemId == RenderOptions.*)
      List()
    else
      List(makeOrFail(options.itemId, new ObjectIdMatches(_), "invalid item id in RenderOptions"))
  }

  private def itemValueAndConstraints(item: ContentRequest)(implicit options: RenderOptions): ValueAndConstraint[Any] = {
    ValueAndConstraint("itemId", item.id, itemConstraints(item))
  }

  private def sessionValueAndConstraints(session: ContentRequest)(implicit options: RenderOptions): ValueAndConstraint[Any] = {
    ValueAndConstraint("sessionId", session.id, sessionConstraints(session))
  }

  private def assessmentValueAndConstraints(assessment: ContentRequest)(implicit options: RenderOptions): ValueAndConstraint[Any] = {
    ValueAndConstraint("assessmentId", assessment.id, assessmentConstraints(assessment))
  }

  private def timeConstraint(implicit options: RenderOptions): ValueAndConstraint[Any] = {
    ValueAndConstraint("expires", System.currentTimeMillis(), List(if (options.expires == 0) new AnyTimeConstraint else new TimeExpiredConstraint(options.expires)))
  }

}
