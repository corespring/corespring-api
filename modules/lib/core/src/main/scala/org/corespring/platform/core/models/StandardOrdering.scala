package org.corespring.platform.core.models

/**
 * This object defines an ordering for Standard. The Standard case class should implement its compare method based on
 * StandardOrder's compare.
 */
object StandardOrdering extends Ordering[Standard] {

  val subjectOrdering = List("ELA-Literacy", "Math")

  val gradeOrdering =
    List("PK", "KG", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "AP", "PS", "UG")

  val abbreviationOrdering = List("RL", "RI", "RF", "W", "SL", "L", "RH", "RST", "WHST", "CC", "OA", "NBT",
    "NF", "MD", "G", "RP", "NS", "EE", "F", "SP", "HSN-RN", "HSN-Q", "HSN-CN", "HSN-VM", "HSA-SSE", "HSA-APR",
    "HSA-CED", "HSA-REI", "HSF-IF", "HSF-BF", "HSF-LE", "HSF-TF", "HSG-CO", "HSG-SRT", "HSG-C", "HSG-GPE", "HSG-GMD",
    "HSG-MG", "HSS-ID", "HSS-IC", "HSS-CP", "HSS-MD")

  def compare(standard: Standard, other: Standard): Int = {
    compare(standard.subject, other.subject, subjectOrdering)(() => {
      compare(standard.grades.headOption, other.grades.headOption ,gradeOrdering)(() => {
        standard.category.compareTo(other.subCategory) match {
          case 0 => standard.subCategory.compareTo(other.subCategory) match {
            case 0 => compare(standard.abbreviation, other.abbreviation, abbreviationOrdering)(() => 0)
            case int: Int => int
          }
          case int: Int => int
        }
      })
    })
  }

  /**
   * Takes two Options and if both are Some, compares them based on their position in a provided List. If both are the
   * same, execute a block yielding a result. If either is None, the other takes precedence. If both are None, execute
   * the block yielding a result.
   */
  private def compare[T](one: Option[T], two: Option[T], list: List[T])(block: () => Int) = (one, two) match {
    case (Some(valueOne), Some(valueTwo)) => list.indexOf(valueOne) - list.indexOf(valueTwo) match {
      case 0 => block()
      case int: Int => int
    }
    case (None, Some(_)) => -1
    case (Some(_), None) => 1
    case _ => block()
  }

  implicit class StringThing(stringOpt: Option[String]) {
    def compareTo(other: Option[String]) = stringOpt.getOrElse("").compareTo(other.getOrElse(""))
  }

}
