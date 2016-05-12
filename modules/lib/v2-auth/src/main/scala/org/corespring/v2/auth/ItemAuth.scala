package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.models.item.{Item, PlayerDefinition}
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.SessionAuth.Session
import org.corespring.v2.errors.V2Error
import org.joda.time.DateTime
import play.api.libs.json.JsValue

import scalaz.Validation

trait Auth[D, IDENTITY, UID] {
  def insert(data: D)(implicit identity: IDENTITY): Option[UID]
  def loadForRead(id: String)(implicit identity: IDENTITY): Validation[V2Error, D]
  def loadForWrite(id: String)(implicit identity: IDENTITY): Validation[V2Error, D]
  def save(data: D, createNewVersion: Boolean)(implicit identity: IDENTITY)
}

trait ItemAuth[A] extends Auth[Item, A, VersionedId[ObjectId]] {
  def canCreateInCollection(collectionId: String)(identity: A): Validation[V2Error, Boolean]
  def canWrite(id: String)(implicit identity: A): Validation[V2Error, Boolean]
  def delete(id: String)(implicit identity: A): Validation[V2Error, VersionedId[ObjectId]]
}

object SessionAuth {
  type Session = JsValue
}

trait SessionAuth[IDENTITY, CONTENT] {
  def canCreate(itemId: String)(implicit identity: IDENTITY): Validation[V2Error, Boolean]
  def cloneIntoPreview(sessionId: String)(implicit identity: IDENTITY): Validation[V2Error, ObjectId]
  def complete(sessionId: String)(implicit identity: IDENTITY): Validation[V2Error, Session]
  def create(session: Session)(implicit identity: IDENTITY): Validation[V2Error, ObjectId]
  def loadForRead(sessionId: String)(implicit identity: IDENTITY): Validation[V2Error, (Session, CONTENT)]
  def loadForSave(sessionId: String)(implicit identity: IDENTITY): Validation[V2Error, Session]
  def loadForScoring(sessionId: String)(implicit identity: IDENTITY): Validation[V2Error, (Session, CONTENT)]
  def loadForScoringMultiple(sessionIds: Seq[String])(implicit identity: IDENTITY): Seq[(String, Validation[V2Error, (JsValue, CONTENT)])]
  def loadForWrite(sessionId: String)(implicit identity: IDENTITY): Validation[V2Error, (Session, CONTENT)]
  def loadWithIdentity(sessionId: String)(implicit identity: IDENTITY): Validation[V2Error, (Session, CONTENT)]
  def orgCount(orgId: ObjectId, month: DateTime)(implicit identity: IDENTITY): Validation[V2Error, Map[DateTime, Long]]
  def reopen(sessionId: String)(implicit identity: IDENTITY): Validation[V2Error, Session]
  def saveSessionFunction(implicit identity: IDENTITY): Validation[V2Error, (String, Session) => Option[Session]]

}


