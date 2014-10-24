package org.corespring.importing.extractors

import com.fasterxml.jackson.core.JsonParseException
import org.bson.types.ObjectId
import org.corespring.json.validation.JsonValidator
import org.corespring.platform.core.models.item.resource._
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json._

import scala.io.Source
import scalaz.{Failure, Success, Validation}

abstract class CorespringItemExtractor(sources: Map[String, Source]) extends ItemExtractor {

  import errors._

  val itemJsonFilename = "item.json"
  val itemMetadataFilename = "metadata.json"

  def metadata: Validation[Error, Option[JsValue]] = {
    try {
      sources.get(itemMetadataFilename).map(item => Success(Some(Json.parse(item.mkString)))).getOrElse(Success(None))
    } catch {
      case json: JsonParseException => Failure(new Error(jsonParseError(itemMetadataFilename)))
    }
  }

  def files(itemId: VersionedId[ObjectId], itemJson: JsValue): Validation[Error, Option[Resource]] = {
    upload(itemId, sources.filter{ case (filename, source) => (itemJson \ "files").asOpt[Seq[String]].getOrElse(Seq.empty).contains(filename) }) match {
      case Success(files) if files.nonEmpty => Success(Some(Resource(name = "data", files = files)))
      case Success(files) => Success(None)
      case Failure(error) => Failure(error)
    }
  }

  def itemJson: Validation[Error, JsValue] = {
    val filename = itemJsonFilename
    try {
      val itemJson = sources.get(filename).map(item => Json.parse(item.mkString))
        .getOrElse(throw new Exception(errors.fileMissing(filename)))
      JsonValidator.validateItem(itemJson) match {
        case Left(errorMessages) => Failure(new Error(errorMessages.mkString("\n")))
        case Right(itemJson) => Success(itemJson)
      }
    } catch {
      case json: JsonParseException => Failure(new Error(jsonParseError(filename)))
      case e: Exception => Failure(new Error(e.getMessage))
    }
  }

}
