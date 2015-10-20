package org.corespring.assets

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId

trait AssetKeys[ID] {

  def folder(id: ID): String

  def file(id: ID, filename: String): String = {
    s"${folder(id)}/data/$filename"
  }

  def supportingMaterialFolder(id: ID, material: String): String = {
    s"${folder(id)}/materials/$material"
  }

  def supportingMaterialFile(id: ID, material: String, filename: String): String = {
    s"${supportingMaterialFolder(id, material)}/$filename"
  }
}

object ItemAssetKeys extends AssetKeys[VersionedId[ObjectId]] {
  override def folder(id: VersionedId[ObjectId]): String = {
    val v = id.version.getOrElse {
      throw new RuntimeException(s"Version must be defined for an itemId: $id")
    }
    s"${id.id}/$v"
  }
}
