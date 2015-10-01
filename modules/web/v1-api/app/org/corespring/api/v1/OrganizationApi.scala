package org.corespring.api.v1

import org.bson.types.ObjectId
import play.api.mvc.Controller

class OrganizationApi(v2Api: org.corespring.v2.api.OrganizationApi) extends Controller {

  def getOrgsWithSharedCollection(collectionId: ObjectId) = v2Api.getOrgsWithSharedCollection(collectionId)
}
