package org.corespring.models

import com.mongodb.casbah.Imports._

case class Standard(dotNotation: Option[String] = None,
  guid: Option[String] = None,
  subject: Option[String] = None,
  category: Option[String] = None,
  subCategory: Option[String] = None,
  standard: Option[String] = None,
  id: ObjectId = new ObjectId(),
  grades: Seq[String] = Seq.empty[String],
  legacyItem: Boolean = false) {

  val kAbbrev = "[K|\\d].([\\w|-]+)\\..*".r
  val abbrev = "([\\w|-]+)..*".r
  val last = ".*\\.(\\w+)$".r

  def abbreviation: Option[String] = dotNotation match {
    case Some(notation) => notation match {
      case kAbbrev(a) => Some(a)
      case abbrev(a) => Some(a)
      case _ => None
    }
    case _ => None
  }

  def code: Option[String] = dotNotation match {
    case Some(notation) => notation match {
      case last(code) => Some(code)
      case _ => None
    }
    case _ => None
  }

  val domain = {

    import Standard.Subjects._

    (subject match {
      case Some(subj) => subj match {
        case ELALiteracy => subCategory
        case ELA => subCategory
        case Math => category
        case _ => None
      }
      case _ => None
    })
  }

}

object Standard {

  val description = "common core state standards"

  object Keys {
    val Id = "id"
    val DotNotation = "dotNotation"
    val Subject = "subject"
    val Category = "category"
    val SubCategory = "subCategory"
    val Standard = "standard"
    val guid = "guid"
    val grades = "grades"
  }

  object Subjects {
    val ELA = "ELA"
    val ELALiteracy = "ELA-Literacy"
    val Math = "Math"
  }
}

