package models

import se.radley.plugin.salat
import play.api.Play.current

/**
 * An Item model
 */
object Item {
  val collection = Content.collection

  val Author = "author"
  val CollId = "collId"
  val ContentType = "contentType"
  val Contributor = "contributor"
  val CopyrightOwner = "copyrightOwner"
  val CopyrightYear = "copyrightYear"
  val Credentials = "credentials"
  val Files = "files"
  val GradeLevel = "gradeLevel"
  val ItemType = "itemType"
  val ItemTypeOther = "itemTypeOther"
  val KeySkills = "keySkills"
  val LicenseType = "licenseType"
  val PrimarySubject = "primarySubject"
  val PriorUse = "priorUse"
  val ReviewsPassed = "reviewsPassed"
  val SourceUrl = "sourceUrl"
  val Standards = "standards"
  val Title = "title"
  val XmlData = "xmlData"

  val queryFields = Map(
      Author -> "String",
      CollId -> "ObjectId",
      ContentType -> "String",
      Contributor -> "String",
      CopyrightOwner -> "String",
      CopyrightYear -> "Int",
      Credentials -> "String",
      Files -> "Array[String]",
      GradeLevel -> "Array[String]",
      ItemType -> "String",
      ItemTypeOther -> "String",
      KeySkills -> "Array[String]",
      LicenseType -> "String",
      PrimarySubject -> "Map[String, String]",
      PriorUse -> "String",
      ReviewsPassed -> "Array[String]",
      SourceUrl -> "String",
      Standards -> "Array[String]",
      Title -> "String",
      XmlData -> "String"
  )
}
