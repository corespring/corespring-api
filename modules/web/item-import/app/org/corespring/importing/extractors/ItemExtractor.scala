package org.corespring.importing.extractors

import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.resource._
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.SourceWrapper
import play.api.libs.json.JsValue

import scala.io.Source
import scalaz.Validation

trait ItemExtractor {

  val ids: Seq[String]
  val metadata: Map[String, Validation[Error, Option[JsValue]]]
  val itemJson: Map[String, Validation[Error, JsValue]]
  def files(id: String, itemId: VersionedId[ObjectId], itemJson: JsValue): Validation[Error, Option[Resource]]
  def upload(itemId: VersionedId[ObjectId], files: Map[String, SourceWrapper]): Validation[Error, Seq[BaseFile]]

}

object errors {
  val cannotCreateItem = "There was an error saving the item to the database"
  def fileMissing(filename: String) = s"Provided item source did not include $filename"
  def jsonParseError(filename: String) = s"$filename did not contain valid json"
  def metadataParseError(field: String) = s"There was an error parsing $field from metadata"
}