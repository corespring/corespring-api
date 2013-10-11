package org.corespring.player.accessControl.models.granter

import org.bson.types.ObjectId
import org.corespring.platform.core.models.versioning.VersionedIdImplicits.Binders.stringToVersionedId
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.player.accessControl.models.ContentRequest
import org.corespring.player.accessControl.models.RequestedAccess.Mode._
import org.corespring.player.accessControl.models.VersionedContentRequest
import org.corespring.player.accessControl.models.granter.constraints._
import org.corespring.player.accessControl.models.{ RenderOptions, RequestedAccess }

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
      m =>
        m match {
          case Preview => (itemAndSession orElse itemOnly orElse noMatch(Preview, "no item or session"))(request)
          case Render => (sessionAndRole orElse sessionOnly orElse noMatch(Render, "no session found"))(request)
          case Aggregate => (itemAndAssessment orElse noMatch(Aggregate, "only an assessment id and item are allowed"))(request)
          case Administer => (itemOnly orElse sessionOnly orElse itemAndSession orElse noMatch(Administer, "no item or session passed"))(request)
          case _ => List()
        }
    }.getOrElse(List(failed("mode", "No mode")))

    common ::: modeSpecificConstraints
  }

  private def itemAndAssessment(implicit options: RenderOptions): PartialFunction[RequestedAccess, List[ValueAndConstraint[Any]]] = {
    case RequestedAccess(Some(item), None, Some(assessment),_,_) => List(assessmentValueAndConstraints(assessment), itemValueAndConstraints(item))
  }

  private def itemAndSession(implicit options: RenderOptions): PartialFunction[RequestedAccess, List[ValueAndConstraint[Any]]] = {
    case RequestedAccess(Some(item), Some(session), _, _,_) => List(itemValueAndConstraints(item), sessionValueAndConstraints(session))
  }

  private def itemOnly(implicit options: RenderOptions): PartialFunction[RequestedAccess, List[ValueAndConstraint[Any]]] = {
    case RequestedAccess(Some(item), None, None, _,_) => List(itemValueAndConstraints(item))
  }

  private def sessionOnly(implicit options: RenderOptions): PartialFunction[RequestedAccess, List[ValueAndConstraint[Any]]] = {
    case RequestedAccess(_, Some(session), None, _,_) => List(sessionValueAndConstraints(session))
  }

  private def sessionAndRole(implicit options: RenderOptions): PartialFunction[RequestedAccess, List[ValueAndConstraint[Any]]] = {
    case RequestedAccess(_, Some(session), None, _,Some(role)) => List(sessionValueAndConstraints(session),roleConstraints(role))
  }

  private def noMatch(key: Mode, msg: String): PartialFunction[RequestedAccess, List[ValueAndConstraint[Any]]] = {
    case _ => List(failed(key.toString, msg))
  }

  private def oid(s: String): Option[ObjectId] = try {
    Some(new ObjectId(s))
  } catch {
    case e: Throwable => None
  }

  private def void(s: String): Option[VersionedId[ObjectId]] = {
    stringToVersionedId(s)
  }

  private def makeOrFail[ID](s: String, converterFn: String => Option[ID], fn: (ID => Constraint[Any]), failedMsg: String) = {
    converterFn(s).map {
      o => fn(o)
    }.getOrElse(new FailedConstraint(failedMsg))
  }

  private def boundConstraints(itemId: String, otherId: String, lookup: ItemLookup): List[Constraint[Any]] = {

    def itemIdToSessionConstraint: List[Constraint[Any]] = {
      if (itemId == RenderOptions.*) {
        List()
      } else {
        List(makeOrFail[VersionedId[ObjectId]](itemId, void, new LookupContainsItemId(_, lookup), "invalid itemId in RenderOptions"))
      }
    }

    if (otherId == RenderOptions.*) {
      itemIdToSessionConstraint
    } else {
      itemIdToSessionConstraint ::: List(makeOrFail[ObjectId](otherId, oid, new ObjectIdMatches(_), "invalid id"))
    }
  }

  private def sessionConstraints(session: ContentRequest)(implicit options: RenderOptions): List[Constraint[Any]] = {
    boundConstraints(options.itemId, options.sessionId, sessionLookup)
  }

  private def assessmentConstraints(assessment: ContentRequest)(implicit options: RenderOptions): List[Constraint[Any]] = {
    boundConstraints(options.itemId, options.assessmentId, quizLookup)
  }

  private def itemConstraints(item: VersionedContentRequest)(implicit options: RenderOptions): List[Constraint[Any]] = {
    if (options.itemId == RenderOptions.*)
      List()
    else
      List(makeOrFail[VersionedId[ObjectId]](options.itemId, void, new VersionedIdMatches(_), "invalid item id in RenderOptions"))
  }

  private def roleConstraints(role:String)(implicit options:RenderOptions): ValueAndConstraint[Any] = {
    ValueAndConstraint("role",role,
      if (options.role == RenderOptions.*) List(new WildcardConstraint)
      else if (options.role == "instructor") List(new SuccessConstraint)
      else List(new StringEqualsConstraint(options.role))
    )
  }

  private def itemValueAndConstraints(item: VersionedContentRequest)(implicit options: RenderOptions): ValueAndConstraint[Any] = {
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
