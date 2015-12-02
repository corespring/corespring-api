package org.corespring.models.item

import org.apache.commons.lang3.builder.HashCodeBuilder
import org.corespring.models.item.resource.BaseFile
import play.api.libs.json.{ Json, JsValue }

/**
 * Model to contain the new v2 player model
 * Note: this is not a case class as we need to support custom serialization w/ salat
 * @param files
 * @param xhtml
 * @param components
 * @param summaryFeedback
 */

class PlayerDefinition(
  val files: Seq[BaseFile],
  val xhtml: String,
  val components: JsValue,
  val summaryFeedback: String,
  val customScoring: Option[String]) {

  def mergeAllButFiles(other: PlayerDefinition): PlayerDefinition = {
    new PlayerDefinition(
      this.files,
      other.xhtml,
      other.components,
      other.summaryFeedback,
      other.customScoring)
  }

  override def toString = s"""PlayerDefinition(${files}, $xhtml, ${Json.stringify(components)}, $summaryFeedback)"""

  override def hashCode() = {
    new HashCodeBuilder(17, 31)
      .append(files)
      .append(xhtml)
      .append(components)
      .append(summaryFeedback)
      .append(customScoring)
      .toHashCode
  }

  override def equals(other: Any) = other match {
    case p: PlayerDefinition => p.files == files && p.xhtml == xhtml && p.components.equals(components) &&
      p.summaryFeedback == summaryFeedback && p.customScoring == customScoring
    case _ => false
  }
}

object PlayerDefinition {
  def apply(xhtml: String) = new PlayerDefinition(Seq.empty, xhtml, Json.obj(), "", None)

  def apply(xhtml: String, components: JsValue) = {
    new PlayerDefinition(Seq.empty, xhtml, components, "", None)
  }

  def apply(
    files: Seq[BaseFile],
    xhtml: String,
    components: JsValue,
    summaryFeedback: String,
    customScoring: Option[String]) = new PlayerDefinition(files, xhtml, components, summaryFeedback, customScoring)

  def empty = new PlayerDefinition(Seq(), "", Json.obj(), "", None)
}
