package org.corespring.api.v2.services

import org.bson.types.ObjectId
import org.corespring.platform.core.models.{ContentCollRef, Organization}
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.item.Item
import org.specs2.mutable.Specification

class ItemPermissionServiceTest extends Specification {

  "ItemPermissionService" should {

    val s = new ItemPermissionService

    "denied - if no collection id" in {
      s.create(Organization(), Item()).granted === false
    }

    "denied " in {
      val item = Item(collectionId = Some(ObjectId.get.toString))
      s.create(Organization(), item).granted === false
    }

    "denied if read only " in {
      val collectionId = ObjectId.get
      val item = Item(collectionId = Some(collectionId.toString))
      val organization = Organization(contentcolls = Seq(ContentCollRef(collectionId, Permission.Read.value)))
      s.create(organization, item).granted === false
    }
    "denied if read only " in {
      val collectionId = ObjectId.get
      val item = Item(collectionId = Some(collectionId.toString))
      val organization = Organization(contentcolls = Seq(ContentCollRef(collectionId, Permission.Write.value)))
      s.create(organization, item).granted === true
    }
  }
}
