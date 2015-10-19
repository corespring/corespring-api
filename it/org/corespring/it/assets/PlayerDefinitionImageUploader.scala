package org.corespring.it.assets

import org.bson.types.ObjectId
import org.corespring.models.item.resource.StoredFile
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.Logger

object PlayerDefinitionImageUploader {

  lazy val itemService = bootstrap.Main.itemService
  lazy val logger = Logger(PlayerDefinitionImageUploader.getClass)
  implicit val ctx = bootstrap.Main.context
  def uploadImageAndAddToPlayerDefinition(
    itemId: VersionedId[ObjectId],
    imagePath: String) = {
    val file = ImageUtils.resourcePathToFile(imagePath)

    val name = grizzled.file.util.basename(file.getCanonicalPath)
    val key = s"${itemId.id}/${itemId.version.getOrElse("0")}/data/$name"
    val sf = StoredFile(name = name, contentType = "image/png", storageKey = key)
    itemService.addFileToPlayerDefinition(itemId, sf)
    val updatedItem = itemService.findOneById(itemId)
    logger.debug(s"Saved item in mongo as: $updatedItem")
    logger.debug(s"Uploading image...: ${file.getPath} -> $key")
    ImageUtils.upload(file, key)
    itemId
  }
}
