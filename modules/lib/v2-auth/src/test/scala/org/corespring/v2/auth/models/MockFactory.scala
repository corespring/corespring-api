package org.corespring.v2.auth.models

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.specs2.mock.Mockito

trait MockFactory extends Mockito{

  def mockOrg = {
    val m = mock[Organization]
    m.id returns ObjectId.get
    m.name returns "mock org"
    m
  }
}
