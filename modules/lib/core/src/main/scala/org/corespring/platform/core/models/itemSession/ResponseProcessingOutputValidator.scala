package org.corespring.platform.core.models.itemSession

import play.api.libs.json._
import org.corespring.qti.models.{ QtiItem, ResponseDeclaration }
import play.api.libs.json.JsObject
import scala.Some
import play.api.libs.json.JsUndefined
import play.api.libs.json.JsNumber
import org.corespring.platform.core.models.error.CorespringInternalError
import scalaz.{ Validation, Success, Failure }

/*
 * Validates that the response from a <responseProcessing> node's Javascript matches the following pattern:
 *
 * {
 *   score: 1,
 *   isCorrect: true,
 *   isComplete: false,
 *   id1: {
 *     score: 1,
 *     isCorrect: true,
 *     isComplete: true
 *   }
 * };
 *
 * In which score, isCorrect, and isComplete are required at the top level. All other top-level keys in the JsObject are
 * required to be matching identifiers from the provided ResponseDeclaration objects. Their values are to be JsObjects
 * as well, containing the same required fields from the top-level JsObject.
 */
object ResponseProcessingOutputValidator {

  def apply(jsObject: JsObject, qtiItem: QtiItem)(implicit debugMode: Boolean): Validation[CorespringInternalError, SessionOutcome] = {
    validateJsResponse(jsObject, qtiItem.responseDeclarations) match {
      case Some(internalError) => Failure(internalError)
      case _ => SessionOutcome.fromJsObject(jsObject, qtiItem.responseDeclarations)
        .fold(errors => Failure(CorespringInternalError(JsError.toFlatJson(errors).toString)), so => Success(so))
    }
  }

  private def validateJsResponse(jsValue: JsValue, responseDeclarations: Seq[ResponseDeclaration]): Option[CorespringInternalError] =
    validateJsValue(jsValue) match {
      case Some(internalError) => Some(internalError)
      case _ => {
        val identifiers = responseDeclarations.filter(_.hasDefaultCorrectResponse).map(_.identifier)
        identifiers.find(identifier => (jsValue \ "identifierOutcomes" \ identifier).isInstanceOf[JsUndefined]) match {
          case Some(identifier) =>
            Some(CorespringInternalError(s"""Response for identifier $identifier is required in JsObject"""))
          case _ => {
            val errors = identifiers.flatMap(identifier => validateJsValue((jsValue \ "identifierOutcomes" \ identifier), Some(identifier)))
            errors match {
              case errors: Seq[CorespringInternalError] if errors.nonEmpty => {
                Some(errors.head)
              }
              case _ => None
            }
          }
        }
      }
    }

  /*
   * Provided a jsValue, ensure that it has the required fields score, isComplete, and isCorrect. Return
   * Some[InternalError] if any fields are missing, None otherwise.
   */
  private def validateJsValue(jsValue: JsValue, identifier: Option[String] = None): Option[CorespringInternalError] = {
    val identifierString: String = identifier match {
      case Some(string) => s""" for responseDeclaration identifier $string"""
      case None => ""
    }
    jsValue match {
      case jsObject: JsObject => {
        (jsObject \ "score") match {
          case JsNumber(_) => {
            (jsObject \ "isComplete") match {
              case JsBoolean(_) => {
                (jsObject \ "isCorrect") match {
                  case JsBoolean(_) => None
                  case JsUndefined() =>
                    Some(CorespringInternalError(s"""isCorrect is required in Javascript response object$identifierString"""))
                  case _ =>
                    Some(CorespringInternalError(s"""isCorrect is required to be a JsBoolean object$identifierString"""))
                }
              }
              case JsUndefined() =>
                Some(CorespringInternalError(s"""isComplete is required in Javascript response object$identifierString"""))
              case _ =>
                Some(CorespringInternalError(s"""isComplete is required to be a JsBoolean object$identifierString"""))
            }
          }
          case JsUndefined() =>
            Some(CorespringInternalError(s"""score is required in Javascript response object$identifierString"""))
          case _ => Some(CorespringInternalError(s"""score is required to be a JsNumber object$identifierString"""))
        }
      }
      case _ => Some(CorespringInternalError(s"""Response processed by Javascript was not a JsObject$identifierString"""))
    }
  }

}
