package org.corespring.json.validation

import play.api.Play

import scala.io.Source
import com.github.fge.jackson.JsonNodeReader
import java.io.StringReader
import play.api.libs.json.{JsArray, Json, JsValue}
import com.github.fge.jsonschema.main.JsonSchemaFactory
import com.github.fge.jsonschema.core.util.AsJson

class JsonValidator(schemaFilename: String) {

  import play.api.Play.current

  private val validator = JsonSchemaFactory.byDefault().getValidator()
  private val jsonNodeReader = new JsonNodeReader()

  private def schema(filename: String) = {
    val inputStream = Play.application.resourceAsStream(filename).getOrElse(throw new IllegalArgumentException(s"File $filename not found"))
    val schemaJson = Source.fromInputStream(inputStream).getLines.mkString
    jsonNodeReader.fromReader(new StringReader(schemaJson))
  }

  private def json(json: JsValue) = jsonNodeReader.fromReader(new StringReader(json.toString))

  def validate(jsValue: JsValue): Either[Seq[String], JsValue] = {
    val processingReport = validator.validateUnchecked(schema(schemaFilename), json(jsValue))
    Json.parse(processingReport.asInstanceOf[AsJson].asJson.toString) match {
      case errorArray: JsArray if errorArray.value.length != 0 =>
        Left(errorArray.value.map(error => (error \ "message").as[String]))
      case _ => Right(jsValue)
    }
  }

}

object JsonValidator {

  // Schema locations
  private val ITEM_SCHEMA = "item-schema.json"
  private val METADATA_SCHEMA = "metadata-schema.json"

  /**
   * Returns a Seq[String] representing validation errors found with the JSON according to the provided schema file.
   * The Seq is empty if there are no validation errors.
   */
  private def validate(schemaFilename: String, json: JsValue) = new JsonValidator(schemaFilename).validate(json)

  def validateItem(json: JsValue) = validate(s"schema/$ITEM_SCHEMA", json)
  def validateMetadata(json: JsValue) = validate(s"schema/$METADATA_SCHEMA", json)

}