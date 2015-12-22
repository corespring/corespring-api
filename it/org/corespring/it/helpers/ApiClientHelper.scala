package org.corespring.it.helpers

import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.models.auth.ApiClient
import org.corespring.services.salat.bootstrap.CollectionNames
import global.Global.main

object ApiClientHelper {

  lazy val service = main.apiClientService
  //TODO: remove mongo reference - use service instead.
  lazy val mongoCollection = main.db(CollectionNames.apiClient)

  def create(orgId: ObjectId): ApiClient = {
    println(s"[ApiClientHelper] create api client with id: $orgId")
    service.getOrCreateForOrg(orgId).toOption.get
  }

  def delete(client: ApiClient) = {
    mongoCollection.remove(MongoDBObject("clientId" -> client.clientId))
  }
}
