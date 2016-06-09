package org.corespring.itemSearch

import java.net.URL

import org.bson.types.ObjectId
import org.corespring.elasticsearch.Index
import play.api.libs.json.{ JsObject, JsValue, Json }
import Json._
import org.corespring.futureValidation.FutureValidation
import play.api.Logger

import scala.concurrent.Future
import scalaz.{ Failure, Success, Validation }

/**
 * A helper object for the content index.
 * //TODO: this should really be part of a `ContentIndex` api.
 */
class ContentIndexHelper(
  index: Index,
  elasticSearchExecutionContext: ElasticSearchExecutionContext,
  auth: AuthenticatedUrl,
  implicit val url: URL) {

  private val logger = Logger(this.getClass)

  implicit val ec = elasticSearchExecutionContext.context

  def addLatest(oid: ObjectId, version: Long, json: JsObject): Future[Validation[Error, String]] = {

    logger.info(s"function=addLatest, oid=$oid, version=$version")
    logger.trace(s"function=addLatest, oid=$oid, version=$version, json=${prettyPrint(json)}")

    val currentPublished = (json \ "published").asOpt[Boolean].getOrElse(false)

    val flagged = json ++
      obj("latest" -> true, "latestPublished" -> currentPublished)

    import FutureValidation._

    logger.trace(s"function=addLatest, oid=$oid, flagged=${prettyPrint(flagged)}")

    val o = for {
      deleteResult <- deleteOldVersions(oid, version)
      updateResult <- updatePenultimateFlags(oid, version - 1, latest = false, latestPublished = !currentPublished)
      add <- fv(index.add(s"$oid:$version", flagged.toString))
    } yield add

    o.future
  }

  private def deleteOldVersions(oid: ObjectId, latestVersion: Long): FutureValidation[Error, String] = {

    logger.debug(s"function=deleteOldVersions, oid=$oid, latestVersion=$latestVersion")

    val lastVersionToDelete = latestVersion - 2
    if (lastVersionToDelete < 0) {
      FutureValidation(Success("{}"))
    } else {
      val deleteQuery: JsValue = obj(
        "query" -> obj(
          "bool" -> obj(
            "must" -> arr(
              obj(
                "term" -> obj(
                  "id" -> oid.toString)),
              obj(
                "range" -> obj(
                  "version" -> obj(
                    "gte" -> 0,
                    "lte" -> lastVersionToDelete)))))))

      import play.api.libs.ws.Implicits._

      val out = auth.authed("s/content/content/_query")
        .delete(deleteQuery)
        .map { result =>
          logger.trace(s"function=deleteOldVersions, oid=$oid, latestVersion=$latestVersion, result.body=${result.body}")
          if (result.status == 200) {
            Success(result.body)
          } else {
            Failure(new Error(result.body))
          }
        }

      FutureValidation.fv(out)
    }
  }

  private def updatePenultimateFlags(oid: ObjectId, penultimateVersion: Long, latest: Boolean, latestPublished: Boolean): FutureValidation[Error, String] = {

    logger.debug(s"function=updatePenultimateFlags, oid=$oid, penultimateVersion=$penultimateVersion, latest=$latest, latestPublished=$latestPublished")

    if (penultimateVersion >= 0) {

      //TODO: this assumes that the penultimate item is published:true - will that always be the case?
      val update = obj(
        "doc" -> obj(
          "latest" -> false,
          "latestPublished" -> latestPublished))

      val operation = auth.authed(s"/content/content/$oid:$penultimateVersion/_update")
        .post(update)
        .map { result =>
          logger.trace(s"function=updatePenultimate, oid=$oid, penultimateVersion=$penultimateVersion, result.body=${result.body}")

          if (result.status == 200 || result.status == 404) {
            Success(result.body)
          } else {
            logger.error(result.body)
            Failure(new Error(result.statusText))
          }
        }
      FutureValidation.fv(operation)
    } else {
      FutureValidation(Success("{}"))
    }
  }
}
