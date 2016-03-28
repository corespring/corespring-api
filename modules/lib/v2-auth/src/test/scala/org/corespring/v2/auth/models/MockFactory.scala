package org.corespring.v2.auth.models

import org.bson.types.ObjectId
import org.corespring.models.{DisplayConfig, ContentCollRef, Organization}
import org.corespring.models.item.Item
import org.corespring.v2.auth.models.AuthMode.AuthMode
import org.specs2.mock.Mockito

trait MockFactory extends Mockito {

  def mockOrg(collections: Seq[ObjectId] = Seq.empty, displayConfig: DisplayConfig = DisplayConfig.default)
    = Organization("mock org", contentcolls = collections.map(ContentCollRef(_, enabled = true)), displayConfig = displayConfig)

  def mockOrgAndOpts(authMode: AuthMode = AuthMode.AccessToken,
    collections: Seq[ObjectId] = Seq.empty, displayConfig: DisplayConfig = DisplayConfig.default) =
    OrgAndOpts(mockOrg(collections, displayConfig = displayConfig), PlayerAccessSettings.ANYTHING, authMode, None)

  def mockCollectionId() = ObjectId.get
  def mockItem = Item(collectionId = mockCollectionId().toString)
}
