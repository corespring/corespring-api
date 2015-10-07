package org.corespring.api.v1

import com.mongodb.casbah.Imports.ObjectId
import org.corespring.v2
import play.api.mvc.{ Action, Controller }

import scala.concurrent.Future

class CollectionApi(v2CollectionApi: v2.api.CollectionApi) extends Controller {

  def list(q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) = {
    v2CollectionApi.list(q, f, strToOption(c), sk, l, sort)
  }

  private def strToOption(s: String) = if (s == "true") Some(true) else None

  def listWithOrg(orgId: ObjectId, q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) = {
    v2CollectionApi.listWithOrg(orgId, q, f, strToOption(c), sk, l, sort)
  }

  def getCollection(id: ObjectId) = v2CollectionApi.getCollection(id)

  def createCollection = v2CollectionApi.createCollection()

  def updateCollection(id: ObjectId) = v2CollectionApi.updateCollection(id)

  def setEnabledStatus(id: ObjectId, enabled: Boolean) = v2CollectionApi.setEnabledStatus(id, enabled)

  def shareCollection(collectionId: ObjectId, destinationOrgId: ObjectId) = v2CollectionApi.shareCollection(collectionId, destinationOrgId)

  def deleteCollection(id: ObjectId) = v2CollectionApi.deleteCollection(id)

  def shareItemsWithCollection(collectionId: ObjectId) = v2CollectionApi.shareItemsWithCollection(collectionId)

  def unShareItemsWithCollection(collectionId: ObjectId) = v2CollectionApi.unShareItemsWithCollection(collectionId)

  def shareFilteredItemsWithCollection(id: ObjectId, q: Option[String]) = Action(NotImplemented)

  def fieldValuesByFrequency(ids: String, field: String) = v2CollectionApi.fieldValuesByFrequency(ids, field)
}
