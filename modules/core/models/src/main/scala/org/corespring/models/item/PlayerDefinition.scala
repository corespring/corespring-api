package org.corespring.models.item

import org.apache.commons.lang3.builder.HashCodeBuilder
import org.corespring.models.item.resource.{BaseFile, StoredFile}
import play.api.libs.json.{JsObject, JsValue, Json}

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
  val customScoring: Option[String],
  val config: JsValue) {

  def mergeAllButFiles(other: PlayerDefinition): PlayerDefinition = {
    new PlayerDefinition(
      this.files,
      other.xhtml,
      other.components,
      other.summaryFeedback,
      other.customScoring,
      other.config)
  }

  def storedFiles: Seq[StoredFile] = files.filter(_.isInstanceOf[StoredFile]).map(_.asInstanceOf[StoredFile])

  override def toString = s"""PlayerDefinition(${files}, $xhtml, ${Json.stringify(components)}, $config, $summaryFeedback)"""

  override def hashCode() = {
    new HashCodeBuilder(17, 31)
      .append(files)
      .append(xhtml)
      .append(components)
      .append(summaryFeedback)
      .append(customScoring)
      .append(config)
      .toHashCode
  }

  override def equals(other: Any) = other match {
    case p: PlayerDefinition =>
      p.files == files &&
        p.xhtml == xhtml &&
        p.components.equals(components) &&
        p.summaryFeedback == summaryFeedback &&
        p.customScoring == customScoring &&
        p.config.equals(config)
    case _ => false
  }
}

object PlayerDefinition {
  def apply(xhtml: String) = new PlayerDefinition(Seq.empty, xhtml, Json.obj(), "", None, Json.obj())

  def apply(xhtml: String, components: JsValue) = {
    new PlayerDefinition(Seq.empty, xhtml, components, "", None, Json.obj())
  }

  def apply(xhtml: String, components: JsValue, config: JsValue) = {
    new PlayerDefinition(Seq.empty, xhtml, components, "", None, config)
  }

  def apply(
    files: Seq[BaseFile],
    xhtml: String,
    components: JsValue,
    summaryFeedback: String,
    customScoring: Option[String],
    config: JsValue) = new PlayerDefinition(files, xhtml, components, summaryFeedback, customScoring, config)

  def empty = PlayerDefinition("")
}
