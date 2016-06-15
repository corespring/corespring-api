package org.corespring.v2.auth.models

import org.bson.types.ObjectId
import org.corespring.models.{ ContentCollRef }
import org.corespring.models.Organization
import org.corespring.models.item.Item
import org.corespring.v2.auth.models.AuthMode.AuthMode
import org.specs2.mock.Mockito

trait MockFactory {

  def mockOrg(collections: Seq[ObjectId] = Seq.empty) = Organization("mock org", contentcolls = collections.map(ContentCollRef(_, enabled = true)))

  def mockOrgAndOpts(authMode: AuthMode = AuthMode.AccessToken,
    collections: Seq[ObjectId] = Seq.empty) = OrgAndOpts(mockOrg(collections), PlayerAccessSettings.ANYTHING, authMode, None)

  def mockCollectionId() = ObjectId.get
  def mockItem = Item(collectionId = mockCollectionId().toString)
}
