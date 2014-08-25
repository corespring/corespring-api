package org.corespring.test.helpers.models

import org.corespring.platform.core.models.auth.ApiClient
import org.bson.types.ObjectId
import org.corespring.platform.core.controllers.auth.AuthTokenGenerating

object ApiClientHelper extends AuthTokenGenerating {

  def create(orgId: ObjectId): ApiClient = {

    val oid = ObjectId.get
    println(s"[ApiClientHelper] create api client with id: $oid")
    val client = ApiClient(orgId, oid, generateToken())
    ApiClient.insert(client)
    client
  }

  def delete(client: ApiClient): Unit = {
    println(s"[ApiClientHelper] delete api client with id: ${client.clientId}")
    if (ApiClient.findByKey(client.clientId.toString).isEmpty) {
      println(s"[ApiClientHelper] Can't find api client: $client")
    }
    ApiClient.remove(client)
  }

}
