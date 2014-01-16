package org.corespring.test.helpers.models

import org.corespring.platform.core.models.auth.ApiClient
import org.bson.types.ObjectId
import org.corespring.platform.core.controllers.auth.AuthTokenGenerating

object ApiClientHelper extends AuthTokenGenerating {

  def create(orgId:ObjectId) : ApiClient = {
    val client =  ApiClient(orgId, ObjectId.get, generateToken())
    ApiClient.insert(client)
    client
  }

  def delete(client:ApiClient) : Unit = {
    ApiClient.remove(client)
  }

}
