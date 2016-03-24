package org.corespring.v2.auth.models

import org.bson.types.ObjectId
import org.corespring.models.{ColorPalette, ContentCollRef, Organization}
import org.corespring.models.item.Item
import org.corespring.v2.auth.models.AuthMode.AuthMode
import org.specs2.mock.Mockito

trait MockFactory extends Mockito {

  def mockOrg(collections: Seq[ObjectId] = Seq.empty, colorPalette: ColorPalette = ColorPalette.default)
    = Organization("mock org", contentcolls = collections.map(ContentCollRef(_, enabled = true)), colorPalette = colorPalette)

  def mockOrgAndOpts(authMode: AuthMode = AuthMode.AccessToken,
    collections: Seq[ObjectId] = Seq.empty, colorPalette: ColorPalette = ColorPalette.default) =
    OrgAndOpts(mockOrg(collections, colorPalette = colorPalette), PlayerAccessSettings.ANYTHING, authMode, None)

  def mockCollectionId() = ObjectId.get
  def mockItem = Item(collectionId = mockCollectionId().toString)
}
