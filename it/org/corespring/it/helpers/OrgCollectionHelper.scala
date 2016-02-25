package org.corespring.it.helpers

import org.bson.types.ObjectId
import global.Global.main

object OrgCollectionHelper {

  def getDefaultCollection(orgId: ObjectId) = main.orgCollectionService.getDefaultCollection(orgId).toOption

}
