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

case class GroupedSessions[D](missingSessions: Seq[String], noItemIds: Seq[String], badItemIds: Seq[String], itemSessions: Seq[D])
case class ItemSessions(itemId: VersionedId[ObjectId], sessions: Seq[JsValue])
case class PlayerDefAndSessions(itemId: VersionedId[ObjectId], playerDef: Validation[PlatformServiceError, PlayerDefinition], sessions: Seq[JsValue])
case class OrgScoringExecutionContext(ec: ExecutionContext)

class OrgScoringService(
  sessionService: SessionService,
  playerDefinitionService: PlayerDefinitionService,
  scoreService: ScoreService,
  scoringServiceExecutionContext: OrgScoringExecutionContext) extends ScoringService[OrgAndOpts] {

  private lazy val logger = Logger(this.getClass)

  private implicit val ec = scoringServiceExecutionContext.ec

  private def groupSessions(sessions: Seq[(String, Option[JsValue])]): Future[GroupedSessions[ItemSessions]] = {
    val groups: Map[String, Seq[(String, Option[JsValue])]] = sessions.groupBy {
      case (id, None) => "missing"
      case (id, Some(json)) => (json \ "itemId").asOpt[String] match {
        case None => "no-item-ids"
        case Some(id) => if (VersionedId(id).isDefined) id else "bad-item-id"
      }
    }

    val itemSessions = groups.toSeq.flatMap {
      case (key, sessions) => if (Seq("missing", "no-item-id", "bad-item-id").contains(key)) {
        None
      } else {
        Some(ItemSessions(VersionedId(key).get, sessions.flatMap(_._2)))
      }
    }

    val out = GroupedSessions(
      missingSessions = groups.getOrElse("missing", Nil).map(_._1),
      noItemIds = groups.getOrElse("no-item-id", Nil).map(_._1),
      badItemIds = groups.getOrElse("bad-item-id", Nil).map(_._1),
      itemSessions = itemSessions)

    Future.successful(out)
  }

  private def getPlayerDefAndSessions(orgId: ObjectId, groupedSessions: GroupedSessions[ItemSessions]): Future[GroupedSessions[PlayerDefAndSessions]] = {
    val itemIds = groupedSessions.itemSessions.map(_.itemId)

    playerDefinitionService.findMultiplePlayerDefinitions(orgId, itemIds: _*).map { playerDefs =>

      logger.trace(s"function=getPlayerDefAndSessions, playerDefs=$playerDefs")

      val defAndSessions: Seq[PlayerDefAndSessions] = playerDefs.map {
        case (id, d) =>
          PlayerDefAndSessions(id, d, groupedSessions.itemSessions.find(_.itemId == id).map(_.sessions).getOrElse(Nil))
      }

      GroupedSessions[PlayerDefAndSessions](groupedSessions.missingSessions, groupedSessions.noItemIds, groupedSessions.badItemIds, defAndSessions)
    }
  }

  private def getScores(grouped: GroupedSessions[PlayerDefAndSessions]): Future[Seq[ScoreResult]] = {
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
    sessions <- sessionService.loadMultipleTwo(ids)
    _ <- Future.successful(logger.debug(s"function=scoreMultipleSessions, ids=$ids, sessions=$sessions"))
    grouped <- groupSessions(sessions)
    _ <- Future.successful(logger.trace(s"function=scoreMultipleSessions, ids=$ids, grouped=$grouped"))
    playerDefAndSessions <- getPlayerDefAndSessions(identity.org.id, grouped)
    _ <- Future.successful(logger.trace(s"function=scoreMultipleSessions, ids=$ids, playerDefAndSessions=$playerDefAndSessions"))
    scores <- getScores(playerDefAndSessions)
  } yield scores

}
