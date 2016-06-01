package org.corespring.v2.api.services

import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.models.item.PlayerDefinition
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.{ PlayerDefinitionService }
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import org.corespring.v2.sessiondb.SessionService
import play.api.Logger
import play.api.libs.json.JsValue

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.{ Failure, Success, Validation }

//case class LoadResult(
//                     missing : Seq[String],
//                     noItemId : Seq[String],
//                     badItemId: Seq[String],
//                     inlineItem : Seq[InlineItemSession],
//
//                     )

//
// Seq["111"] =>
//   Result(missing = Nil, noItemId = Nil, badItemId = Nil, loadedSessions = Seq("111" -> {})) =>
//      Result(missing = Nil, noItemId = Nil, badItemId = Nil, loadedSessions = Seq("111" -> {})) =>
//
case class GroupedSessions[D](
  missingSessions: Seq[String],
  noItemIds: Seq[String],
  badItemIds: Seq[String],
  itemSessions: Seq[D])

case class ItemSessions(itemId: VersionedId[ObjectId], sessions: Seq[JsValue])
case class PlayerDefAndSessions(itemId: VersionedId[ObjectId], playerDef: Validation[PlatformServiceError, PlayerDefinition], sessions: Seq[JsValue])
case class OrgScoringExecutionContext(ec: ExecutionContext)

class OrgScoringService(
  sessionService: SessionService,
  playerDefinitionService: PlayerDefinitionService,
  scoreService: ScoreService,
  scoringServiceExecutionContext: OrgScoringExecutionContext) extends ScoringService[OrgAndOpts] {

  sealed trait Grouping

  case object Missing extends Grouping
  case object NoItemId extends Grouping
  case object BadItemId extends Grouping
  case class GoodItemId(id: VersionedId[ObjectId]) extends Grouping

  private lazy val logger = Logger(this.getClass)

  private implicit val ec = scoringServiceExecutionContext.ec

  //  private def groupSessions(sessions: Seq[(String, Option[JsValue])]): Future[GroupedSessions[ItemSessions]] = {
  //    val groups: Map[Grouping, Seq[(String, Option[JsValue])]] = sessions.groupBy {
  //      case (id, None) => Missing
  //      case (id, Some(json)) => (json \ "itemId").asOpt[String] match {
  //        case None => NoItemId
  //        case Some(id) => VersionedId(id).map(GoodItemId(_)).getOrElse(BadItemId)
  //      }
  //    }
  //
  //    val itemSessions = groups.toSeq.flatMap {
  //      case (GoodItemId(vid), sessions) => {
  //        Some(ItemSessions(vid, sessions.flatMap(_._2)))
  //      }
  //      case _ => None
  //    }
  //
  //    val out = GroupedSessions(
  //      missingSessions = groups.getOrElse(Missing, Nil).map(_._1),
  //      noItemIds = groups.getOrElse(NoItemId, Nil).map(_._1),
  //      badItemIds = groups.getOrElse(BadItemId, Nil).map(_._1),
  //      itemSessions = itemSessions)
  //
  //    Future.successful(out)
  //  }

  sealed trait SessionLoad {
    def sessionId: String
  }

  case class SessionLoadFailure(sessionId: String, error: V2Error) extends SessionLoad
  case class SessionLoaded(sessionId: String, session: JsValue) extends SessionLoad

  case class SessionsFailed(sessions: Seq[SessionLoadFailure])
  case class SessionsByItemId(sessions: Seq[SessionLoaded], itemId: VersionedId[ObjectId])
  case class SessionsByItemIdAndLoadedDefinition(sessions: Seq[SessionLoaded], itemId: VersionedId[ObjectId], maybeDef: Validation[PlatformServiceError, PlayerDefinition])
  case class SessionsByInlineDefinition(sessions: Seq[SessionLoaded], definition: PlayerDefinition)

  //  case class SessionWithItemId(sessionId:String, session:JsValue, itemId:VersionedId[ObjectId]) extends SessionLoad
  //  case class SessionWithInlineDefinition(sessionId:String, session:JsValue, playerDef:PlayerDefinition) extends SessionLoad
  //  case class SessionWithLoadedDefinition(sessionId:String, session:JsValue, itemId:VersionedId[ObjectId], maybeDef:Validation[PlatformServiceError,PlayerDefinition]) extends SessionLoad

  private def groupSessions(sessions: Seq[(String, Option[JsValue])]): Future[Seq[SessionLoad]] = {

    def toSessionLoad(r: (String, Option[JsValue])): SessionLoad = {
      val (id, maybeSession) = r
      maybeSession.map { s =>
        (s \ "itemId").asOpt[String] match {
          case Some(itemId) => VersionedId(itemId).map(vid => id -> SessionWithItemId(id, s, vid)).getOrElse(SessionLoadFailure(id, generalError("Invalid itemId")))
          case _ => id -> SessionLoadFailure(id, generalError("No item id"))
        }
      }.getOrElse(id -> SessionLoadFailure(id, generalError("Not found")))
    }

    Future { sessions.map(toSessionLoad) }
  }

  private def getPlayerDefAndSessions(orgId: ObjectId, s: Seq[SessionLoad]): Future[Seq[SessionLoad]] = {

    val sessionWithItemIds = s.flatMap({
      case s: SessionWithItemId => Some(s)
      case _ => None
    })

    playerDefinitionService.findMultiplePlayerDefinitions(orgId, sessionWithItemIds.map(_.itemId): _*).map { playerDefs =>

      logger.trace(s"function=getPlayerDefAndSessions, playerDefs=$playerDefs")

      playerDefs.flatMap {
        case (id, d) =>
          sessionWithItemIds.find(_.itemId == id).map { swi =>
            SessionWithLoadedDefinition(swi.sessionId, swi.session, swi.itemId, d)
          }
      }
    }
  }

  private def getScores(loaded: Seq[SessionLoad]): Future[Seq[ScoreResult]] = {

    loaded.map(sl => sl match {
      case SessionLoadFailure(sessionId, err) => ScoreResult(sessionId, Failure(err))
      case SessionWithLoadedDefinition(sessionId, session, itemId, maybeDef) => {
        maybeDef match {
          case Failure(e) => ScoreResult(sessionId, Failure(generalError(e.message)))
          case Success(d) =>
        }

      }

    })
    val futures = grouped.itemSessions.foldRight[Seq[Future[(JsValue, Validation[V2Error, JsValue])]]](Seq.empty) { (pds, acc) =>
      pds.playerDef match {
        case Success(d) => acc ++ scoreService.scoreMultiple(d, pds.sessions)
        case Failure(e) => acc ++ pds.sessions.map { s => Future.successful(s -> Failure(generalError(e.message))) }
      }
    }

    def toScoreResult(tuple: (JsValue, Validation[V2Error, JsValue])): ScoreResult = {
      val (session, result) = tuple
      ScoreResult((session \ "_id" \ "$oid").as[String], result)
    }

    val results: Future[Seq[(JsValue, Validation[V2Error, JsValue])]] = Future.sequence(futures)

    results.map { sessionAndScores =>
      sessionAndScores.map(toScoreResult) ++
        grouped.badItemIds.map(id => ScoreResult(id, Failure(generalError("Bad Item id")))) ++
        grouped.noItemIds.map(id => ScoreResult(id, Failure(generalError("No item id")))) ++
        grouped.missingSessions.map(id => ScoreResult(id, Failure(generalError("No session found"))))
    }
  }

  override def scoreSession(identity: OrgAndOpts)(id: String): Future[ScoreResult] = {
    scoreMultipleSessions(identity)(Seq(id)).map { results =>
      results.head
    }
  }

  override def scoreMultipleSessions(identity: OrgAndOpts)(ids: Seq[String]): Future[Seq[ScoreResult]] = for {
    sessions <- sessionService.loadMultiple(ids)
    _ <- Future.successful(logger.debug(s"function=scoreMultipleSessions, ids=$ids, sessions=$sessions"))
    grouped <- groupSessions(sessions)
    _ <- Future.successful(logger.trace(s"function=scoreMultipleSessions, ids=$ids, grouped=$grouped"))
    playerDefAndSessions <- getPlayerDefAndSessions(identity.org.id, grouped)
    _ <- Future.successful(logger.trace(s"function=scoreMultipleSessions, ids=$ids, playerDefAndSessions=$playerDefAndSessions"))
    scores <- getScores(playerDefAndSessions)
  } yield scores

}
