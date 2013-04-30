package player.accessControl.models.granter

import org.bson.types.ObjectId
import player.accessControl.models.RequestedAccess.Mode
import player.accessControl.models.granter.constraints._
import player.accessControl.models.{ContentRequest, RenderOptions, RequestedAccess}

class ConstraintGranter(sessionLookup: SessionItemLookup, quizLookup: QuizItemLookup) extends AccessGranter {

  def grant(currentMode: Option[Mode.Mode], request: RequestedAccess, options: RenderOptions): Boolean = {
    val mode: Option[Mode.Mode] = if (request.mode.isDefined) request.mode else currentMode
    grant(request.copy(mode = mode), options)
  }

  /** Return a list of failing constraints - public as it is useful for debugging issues */
  def getFailedConstraints(currentMode: Option[Mode.Mode], request: RequestedAccess, options: RenderOptions): List[ValueAndConstraint[Any]] = {
    val mode: Option[Mode.Mode] = if (request.mode.isDefined) request.mode else currentMode
    getFailedConstraints(request.copy(mode = mode), options)
  }

  def getFailedConstraints(request: RequestedAccess, options: RenderOptions): List[ValueAndConstraint[Any]] = {
    val constraints = buildConstraints(request, options)

    def fold(vc: ValueAndConstraint[Any], acc: List[ValueAndConstraint[Any]]): List[ValueAndConstraint[Any]] = {
      if (vc.valueWithinConstraints)
        acc
      else
        vc.failingConstraints :: acc
    }
    val failedConstraints = constraints.foldRight[List[ValueAndConstraint[Any]]](List())(fold)
    failedConstraints
  }

  def grant(request: RequestedAccess, options: RenderOptions): Boolean = {
    val failedConstraints = getFailedConstraints(request, options)
    failedConstraints.length == 0
  }

  /** create the list of constraints that must return true for the 'valueWithinConstraints' method */
  private def buildConstraints(request: RequestedAccess, options: RenderOptions): List[ValueAndConstraint[Any]] = {

    val common: List[ValueAndConstraint[Any]] = List(
      ValueAndConstraint("expires", System.currentTimeMillis(), List(if (options.expires == 0) new AnyTimeConstraint else new TimeExpiredConstraint(options.expires))))

    val modeSpecificConstraints: List[ValueAndConstraint[Any]] = request.mode.map {
      m => m match {
        case Mode.Preview => request match {
          case RequestedAccess(Some(item), Some(session), _, _) => List(itemValueAndConstraints(item, options), sessionValueAndConstraints(session, options))
          case RequestedAccess(Some(item), None, _, _) => List(itemValueAndConstraints(item, options))
          case _ => List(failed("preview", "failed - no item or session in request"))
        }
        case Mode.Render => request match {
          case RequestedAccess(_, Some(session), _, _) => List(sessionValueAndConstraints(session, options))
          case _ => List(failed("render", "no session in request"))
        }
        case Mode.Aggregate => request match {
          case RequestedAccess(Some(item), None, Some(assessment), _) => List(assessmentValueAndConstraints(assessment, options), itemValueAndConstraints(item, options))
          case _ => List(failed("aggregate", "only an assessment and item request are allowed"))
        }
        case Mode.Administer => request match {
          case RequestedAccess(Some(item), None, None, _) => List(itemValueAndConstraints(item, options))
          case RequestedAccess(None, Some(session), None, _) => List(sessionValueAndConstraints(session, options))
          case RequestedAccess(Some(item), Some(session), None, _) => List(itemValueAndConstraints(item, options), sessionValueAndConstraints(session, options))
          case _ => List(failed("administer", "no item or session passed"))
        }
        case _ => List()
      }
    }.getOrElse(List(failed("mode", "No mode")))

    common ::: modeSpecificConstraints
  }

  private def failed(name: String, msg: String): ValueAndConstraint[Any] = ValueAndConstraint(name, "?", List(new FailedConstraint(msg)))

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

  private def sessionConstraints(options: RenderOptions, session: ContentRequest): List[Constraint[Any]] = {
    boundConstraints(options.itemId, options.sessionId, sessionLookup)
  }

  private def assessmentConstraints(options: RenderOptions, assessment: ContentRequest): List[Constraint[Any]] = {
    boundConstraints(options.itemId, options.assessmentId, quizLookup)
  }

  private def itemConstraints(options: RenderOptions, item: ContentRequest): List[Constraint[Any]] = {
    if (options.itemId == RenderOptions.*)
      List()
    else
      List(makeOrFail(options.itemId, new ObjectIdMatches(_), "invalid item id in RenderOptions"))
  }

  private def itemValueAndConstraints(item: ContentRequest, options: RenderOptions): ValueAndConstraint[Any] = {
    ValueAndConstraint("itemId", item.id, itemConstraints(options, item))
  }

  private def sessionValueAndConstraints(session: ContentRequest, options: RenderOptions): ValueAndConstraint[Any] = {
    ValueAndConstraint("sessionId", session.id, sessionConstraints(options, session))
  }

  private def assessmentValueAndConstraints(assessment: ContentRequest, options: RenderOptions): ValueAndConstraint[Any] = {
    ValueAndConstraint("assessmentId", assessment.id, assessmentConstraints(options, assessment))
  }

}
