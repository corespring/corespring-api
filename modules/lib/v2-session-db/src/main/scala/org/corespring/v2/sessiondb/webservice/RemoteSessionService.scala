package org.corespring.v2.sessiondb.webservice

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.sessions.SessionServiceClient
import org.corespring.v2.sessiondb.SessionService
import play.api.libs.json.JsValue

import scala.concurrent.Await
import scala.concurrent.duration._
import scalaz._

/**
 * [[SessionService]] implementation that uses [[SessionServiceClient]] to interface with a sessions webservice
 * (see https://github.com/corespring/session-service#usage for more information)
 */
class RemoteSessionService(host: String, bucket: Option[String], authToken: String) extends SessionService {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val REMOTE_TIMEOUT = Duration(30, SECONDS)

  private val client =
    new SessionServiceClient(host = host, bucket = bucket, authToken = authToken)

  override def sessionCount(itemId: VersionedId[ObjectId]): Long =
    Await.result(client.sessionCount(itemId.toString), REMOTE_TIMEOUT) match {
      case Success(count) => count
      case Failure(error) => throw new Exception(error.getMessage)
    }

  override def load(id: String): Option[JsValue] = Await.result(client.get(new ObjectId(id)), REMOTE_TIMEOUT) match {
    case Success(maybeJson) => maybeJson
    case Failure(error) => None
  }

  override def save(id: String, data: JsValue): Option[JsValue] =
    Await.result(client.update(new ObjectId(id), data), REMOTE_TIMEOUT) match {
      case Success(maybeJson) => Some(maybeJson)
      case Failure(error) => None
    }

  override def create(data: JsValue): Option[ObjectId] = Await.result(client.create(data), REMOTE_TIMEOUT) match {
    case Success(json) => (json \ "id").asOpt[String].map(new ObjectId(_))
    case Failure(error) => None
  }

}
