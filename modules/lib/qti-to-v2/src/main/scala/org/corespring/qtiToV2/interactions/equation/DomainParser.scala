package org.corespring.qtiToV2.interactions.equation

import play.api.libs.json._

/**
 * Parses a "domain" string from the old QTI format to the new container format. For example, the old QTI format would
 * is of the comma-separated style "-10->10,0", where two numbers separated by a '->' denote an included range, whereas
 * individual numbers correspond to excluded integers from the range. The new container format representation uses the
 * following JSON format:
 * <pre>
 *   {
 *     "included" : ["-10,10"],
 *     "excluded" : [0]
 *   }
 * </pre>
 *
 * In which included ranges are comma-delimited in their own strings, and excluded integers are provided in an array.
 */
trait DomainParser {

  /**
   * Parses a "domain" string from the old QTI format, returning a JSON representation of the new container format.
   */
  def parseDomain(domain: String): JsObject = {
    flattenObj(
      "included" -> (domain.split(",").map(_.trim).toSeq.filter(_.contains("->")).map(_.replaceAll("->", ",")) match {
        case empty: Seq[String] if empty.isEmpty => None
        case nonEmpty: Seq[String] => Some(JsArray(nonEmpty.map(JsString(_))))
      }),
      "excluded" -> (domain.split(",").map(_.trim).toSeq.map(optInt(_)).flatten match {
        case empty: Seq[Int] if empty.isEmpty => None
        case nonEmpty: Seq[Int] => Some(JsArray(nonEmpty.map(JsNumber(_))))
      }))
  }

  private def flattenObj(fields: (String, Option[JsValue])*): JsObject =
    JsObject(fields.filter { case (_, v) => v.nonEmpty }.map { case (a, b) => (a, b.get) })

  private def optInt(s: String): Option[Int] = try {
    Some(s.toInt)
  } catch {
    case _: java.lang.NumberFormatException => None
  }

}

