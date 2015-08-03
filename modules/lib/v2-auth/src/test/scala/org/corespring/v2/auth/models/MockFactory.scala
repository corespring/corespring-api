package org.corespring.v2.auth.models

import org.bson.types.ObjectId
import org.corespring.models.{ ContentCollRef }
import org.corespring.models.Organization
import org.corespring.models.item.Item
import org.corespring.v2.auth.models.AuthMode.AuthMode
import org.specs2.mock.Mockito

trait MockFactory extends Mockito {

  def mockOrg(collections: Seq[ObjectId] = Seq.empty) = {
    val m = mock[Organization]
    m.id returns ObjectId.get
    m.name returns "mock org"
    m.contentcolls returns collections.map(ContentCollRef(_, enabled = true))
    m
  }

  def mockOrgAndOpts(authMode: AuthMode = AuthMode.AccessToken,
    collections: Seq[ObjectId] = Seq.empty) = OrgAndOpts(mockOrg(collections), PlayerAccessSettings.ANYTHING, authMode, None)

  def mockItem = Item(ObjectId.get.toString)
}
