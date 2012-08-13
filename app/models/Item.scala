package models

import se.radley.plugin.salat
import play.api.Play.current

/**
 * An Item model
 */
object Item {
  val collection = Content.collection

  val queryFields = Map(
    "gradeLevel" -> "String",
    "keySkills" -> "Seq[String]",
    "files" -> "Seq[String]",
    "primarySubject" -> "???",
    "title" -> "String",
    "demonstratedKnowledge" -> "String",
    "itemType" -> "String",
    "copyrightOwner" -> "String",
    "copyrightYear" -> "String",
    "materialType" -> "String",
    "contributor" -> "String",
    "credentials" -> "String",
    "author" -> "String",
    "licenseType" -> "String",
    "bloomsTaxonomy" -> "String",
    "priorUse" -> "String",
    "sourceUrl" -> "String",
    "collId" -> "ObjectId",
    "contentType" -> "String"
  )
}
