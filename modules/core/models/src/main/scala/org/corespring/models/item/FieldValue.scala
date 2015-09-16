package org.corespring.models.item

import org.bson.types.ObjectId

case class ListKeyValue(key: String, value: Seq[String])

case class StringKeyValue(key: String, value: String)

case class FieldValue(
  version: Option[String] = None,
  gradeLevels: Seq[StringKeyValue] = Seq(),
  reviewsPassed: Seq[StringKeyValue] = Seq(),
  mediaType: Seq[StringKeyValue] = Seq(),
  keySkills: Seq[ListKeyValue] = Seq(),
  itemTypes: Seq[ListKeyValue] = Seq(),
  licenseTypes: Seq[StringKeyValue] = Seq(),
  priorUses: Seq[StringKeyValue] = Seq(),
  depthOfKnowledge: Seq[StringKeyValue] = Seq(),
  credentials: Seq[StringKeyValue] = Seq(),
  bloomsTaxonomy: Seq[StringKeyValue] = Seq(),
  id: ObjectId = new ObjectId())

object FieldValue {
  val Version = "version"
  val KeySkills = "keySkills"
  val MediaType = "mediaType"
  val GradeLevel = "gradeLevels"
  val ReviewsPassed = "reviewsPassed"
  val ItemTypes = "itemTypes"
  val LicenseTypes = "licenseTypes"
  val PriorUses = "priorUses"
  val Credentials = "credentials"
  val BloomsTaxonomy = "bloomsTaxonomy"
  val DepthOfKnowledge = "depthOfKnowledge"

  val descriptions = Map(
    KeySkills -> "valid keyskills",
    GradeLevel -> "valid grade levels",
    ReviewsPassed -> "valid reviews passed",
    ItemTypes -> "valid item types (note: if you specify 'Other' you can enter freetext)",
    LicenseTypes -> "license types",
    PriorUses -> "prior uses",
    Credentials -> "credentials",
    BloomsTaxonomy -> "bloomsTaxonomy stuff",
    DepthOfKnowledge -> "Depth of Knowledge")
}
