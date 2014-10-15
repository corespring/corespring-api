package org.corespring.json.validation

import scala.io.Source
import com.github.fge.jackson.JsonNodeReader
import java.io.StringReader
import play.api.libs.json.{JsArray, Json, JsValue}
import com.github.fge.jsonschema.main.JsonSchemaFactory
import com.github.fge.jsonschema.core.util.AsJson

class JsonValidator(schemaFilename: String) {

  private val validator = JsonSchemaFactory.byDefault().getValidator()
  private val jsonNodeReader = new JsonNodeReader()

  private def schema(filename: String) = {
    val schemaJson = Source.fromURL(getClass.getResource("/item-schema.json")).mkString
    jsonNodeReader.fromReader(new StringReader(schemaJson))
  }

  private def json(json: JsValue) = jsonNodeReader.fromReader(new StringReader(json.toString))

  def validate(jsValue: JsValue): Seq[String] = {
    val processingReport = validator.validateUnchecked(schema(schemaFilename), json(jsValue))
    Json.parse(processingReport.asInstanceOf[AsJson].asJson.toString) match {
      case errorArray: JsArray if errorArray.value.length != 0 =>
        errorArray.value.map(error => (error \ "message").as[String])
      case _ => Seq.empty
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

  def validateItem(json: JsValue) = validate(ITEM_SCHEMA, json)
  def validateMetadata(json: JsValue) = validate(METADATA_SCHEMA, json)

}