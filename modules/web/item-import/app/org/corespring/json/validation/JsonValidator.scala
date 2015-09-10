package org.corespring.json.validation

import java.io.StringReader

import com.github.fge.jackson.JsonNodeReader
import com.github.fge.jsonschema.core.util.AsJson
import com.github.fge.jsonschema.main.JsonSchemaFactory
import play.api.libs.json.{ JsArray, JsValue, Json }

private[validation] abstract class JsonValidator(schemaFilename: String, readFile: String => Option[String]) {

  private val validator = JsonSchemaFactory.byDefault().getValidator()
  private val jsonNodeReader = new JsonNodeReader()

  private def schema(filename: String) = {
    val schemaString = readFile(filename).getOrElse(throw new IllegalArgumentException(s"File $filename not found"))
    jsonNodeReader.fromReader(new StringReader(schemaString))
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

private[corespring] class MetadataValidator(readFile: String => Option[String])
  extends JsonValidator("schema/metadata-schema.json", readFile)

private[corespring] class ItemValidator(readFile: String => Option[String])
  extends JsonValidator("schema/item-schema.json", readFile)

