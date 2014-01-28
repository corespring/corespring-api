package org.corespring.v2player.integration.transformers.qti.interactions.equation

import play.api.libs.json._

trait DomainParser {

  def parseDomain(domain: String): JsObject = {
    flattenObj(
      "included" -> (domain.split(",").map(_.trim).toSeq.filter(_.contains("->")).map(_.replaceAll("->", ",")) match {
        case empty: Seq[String] if empty.isEmpty => None
        case nonEmpty: Seq[String] => Some(JsArray(nonEmpty.map(JsString(_))))
      }),
      "excluded" -> (domain.split(",").map(_.trim).toSeq.map(optInt(_)).flatten match {
        case empty: Seq[Int] if empty.isEmpty => None
        case nonEmpty: Seq[Int] => Some(JsArray(nonEmpty.map(JsNumber(_))))
      })
    )
  }

  private def flattenObj(fields : (String, Option[JsValue])*): JsObject =
    JsObject(fields.filter{ case (_, v) => v.nonEmpty }.map{ case (a,b) => (a, b.get) })

  private def optInt(s: String): Option[Int] = try {
    Some(s.toInt)
  } catch {
    case _ : java.lang.NumberFormatException => None
  }

}

