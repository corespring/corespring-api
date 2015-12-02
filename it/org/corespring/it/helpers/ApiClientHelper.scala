package org.corespring.it.helpers

import bootstrap.Main
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.models.auth.ApiClient
import org.corespring.services.salat.bootstrap.CollectionNames

object ApiClientHelper {

  lazy val service = bootstrap.Main.apiClientService
  //TODO: remove mongo reference - use service instead.
  lazy val mongoCollection = Main.db(CollectionNames.apiClient)

  def create(orgId: ObjectId): ApiClient = {
    println(s"[ApiClientHelper] create api client with id: $orgId")
    service.getOrCreateForOrg(orgId).toOption.get
  }

  def delete(client: ApiClient) = {
    mongoCollection.remove(MongoDBObject("clientId" -> client.clientId))
  }
}
