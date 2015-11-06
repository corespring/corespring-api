package org.corespring.importing.validation

import java.io.StringReader

import com.github.fge.jackson.JsonNodeReader
import com.github.fge.jsonschema.core.util.AsJson
import com.github.fge.jsonschema.main.JsonSchemaFactory
import play.api.libs.json.{JsArray, Json, JsValue}

import scalaz.{Failure, Success, Validation}

case class ItemSchema(src:String)
class ItemJsonValidator(schema : ItemSchema ) {

  private lazy val validator = JsonSchemaFactory.byDefault().getValidator()

  private lazy val jsonNodeReader = new JsonNodeReader()

  private lazy val schemaJson = jsonNodeReader.fromReader(new StringReader(schema.src))

  private def json(json: JsValue) = jsonNodeReader.fromReader(new StringReader(json.toString))

  def validate(jsValue: JsValue): Validation[Seq[String], JsValue] = {
      val processingReport = validator.validateUnchecked(schemaJson, json(jsValue))
      Json.parse(processingReport.asInstanceOf[AsJson].asJson.toString) match {
        case errorArray: JsArray if errorArray.value.length != 0 =>
          Failure(errorArray.value.map(error => (error \ "message").as[String]))
        case _ => Success(jsValue)
      }
    }

}
