package org.corespring.v2.api.services

import org.bson.types.ObjectId
import org.corespring.models.item.PlayerDefinition
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import org.corespring.v2.sessiondb.SessionService
import play.api.libs.json.JsValue

import scala.concurrent.Future
import scalaz.{ Failure, Validation }

case class GroupedSessions[D](missingSessions: Seq[String], noItemIds: Seq[String], badItemIds: Seq[String], itemSessions: Seq[D])
case class ItemSessions(itemId: VersionedId[ObjectId], sessions: Seq[JsValue])
case class PlayerDefAndSessions(itemId: VersionedId[ObjectId], playerDef: Option[PlayerDefinition], sessions: Seq[JsValue])

class OrgScoringService(
  sessionService: SessionService,
  itemService: ItemService,
  scoreService: ScoreService) extends ScoringService[OrgAndOpts] {

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

    itemService.findMultiplePlayerDefinitions(orgId, itemIds: _*).map { playerDefs =>
      val defAndSessions: Seq[PlayerDefAndSessions] = playerDefs.map { (tuple) =>
        val (id, d) = tuple
        PlayerDefAndSessions(id, d, groupedSessions.itemSessions.find(_.itemId == id).map(_.sessions).getOrElse(Nil))
      }
      GroupedSessions[PlayerDefAndSessions](groupedSessions.missingSessions, groupedSessions.noItemIds, groupedSessions.badItemIds, defAndSessions)
    }
  }

  private def getScores(grouped: GroupedSessions[PlayerDefAndSessions]): Future[Seq[ScoreResult]] = {
    val futures = grouped.itemSessions.foldRight[Seq[Future[(JsValue, Validation[V2Error, JsValue])]]](Seq.empty) { (pds, acc) =>
      pds.playerDef match {
        case Some(d) => acc ++ scoreService.scoreMultiple(d, pds.sessions)
        case _ => acc ++ pds.sessions.map { s => Future.successful(s -> Failure(generalError("Can't access item"))) }
      }
    }

    def toScoreResult(tuple: (JsValue, Validation[V2Error, JsValue])): ScoreResult = {
      val (session, result) = tuple
      ScoreResult((session \ "id").as[String], result)
    }

    val results: Future[Seq[(JsValue, Validation[V2Error, JsValue])]] = Future.sequence(futures)

    results.map { sessionAndScores =>
      sessionAndScores.map(toScoreResult) ++
        grouped.badItemIds.map(id => ScoreResult(id, Failure(generalError("Bad Item id")))) ++
        grouped.noItemIds.map(id => ScoreResult(id, Failure(generalError("No item id")))) ++
        grouped.missingSessions.map(id => ScoreResult(id, Failure(generalError("No session found"))))
    }
  }

  override def scoreMultipleSessions(identity: OrgAndOpts)(ids: Seq[String]): Future[Seq[ScoreResult]] = for {
    sessions <- sessionService.loadMultiple(ids)
    grouped <- groupSessions(sessions)
    playerDefAndSessions <- getPlayerDefAndSessions(identity.org.id, grouped)
    scores <- getScores(playerDefAndSessions)
  } yield scores

}
