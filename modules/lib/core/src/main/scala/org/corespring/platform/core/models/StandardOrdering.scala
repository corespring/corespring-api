package org.corespring.platform.core.models

/**
 * This object defines an ordering for Standard. The Standard case class should implement its compare method based on
 * StandardOrder's compare.
 */
object StandardOrdering extends Ordering[Standard] {

  val subjectOrdering = List("ELA", "Math")

  val gradeOrdering =
    List("PK", "KG", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "AP", "PS", "UG")

  val abbreviationOrdering = List("RL", "RI", "RF", "W", "SL", "L", "RH", "RST", "WHST", "CC", "OA", "NBT",
    "NF", "MD", "G", "RP", "NS", "EE", "F", "SP", "HSN-RN", "HSN-Q", "HSN-CN", "HSN-VM", "HSA-SSE", "HSA-APR",
    "HSA-CED", "HSA-REI", "HSF-IF", "HSF-BF", "HSF-LE", "HSF-TF", "HSG-CO", "HSG-SRT", "HSG-C", "HSG-GPE", "HSG-GMD",
    "HSG-MG", "HSS-ID", "HSS-IC", "HSS-CP", "HSS-MD")

  private implicit class CodeCompare(one: Standard) {
    val numberWithoutLetters = "(\\d+)$".r
    val numberWithLetters = "(\\d+)([a-z])$".r
    val letter = "([a-z])".r
    def compareCode(two: Standard) = {
      (one.code, two.code) match {
        case (Some(first), Some(second)) => (first, second) match {
          case (numberWithoutLetters(firstNumber), numberWithoutLetters(secondNumber)) =>
            firstNumber.toInt.compareTo(secondNumber.toInt)
          case (numberWithoutLetters(firstNumber), numberWithLetters(secondNumber, secondLetter)) =>
            firstNumber.toInt.compareTo(secondNumber.toInt) match {
              case 0 => -1
              case int: Int => int
            }
          case (numberWithLetters(firstNumber, firstLetter), numberWithoutLetters(secondNumber)) =>
            firstNumber.toInt.compareTo(secondNumber.toInt) match {
              case 0 => 1
              case int: Int => int
            }
          case (numberWithLetters(firstNumber, firstLetter), numberWithLetters(secondNumber, secondLetter)) => {
            firstNumber.toInt.compareTo(secondNumber.toInt) match {
              case 0 => firstLetter.compareTo(secondLetter)
              case int: Int => int
            }
          }
          case (letter(firstLetter), letter(secondLetter)) => firstLetter.compareTo(secondLetter)
          case (letter(_*), _) => 1
          case (_, letter(_*)) => -1
          case _ => throw new IllegalArgumentException(s"Couldn't match ${first}, ${second}")
        }
        case (None, Some(_)) => -1
        case (Some(_), None) => 1
        case (None, None) => 0
      }
    }
  }

  private def normalizeSubject(standard: Standard): Standard =
    standard.copy(subject = standard.subject.map(subject => if (subject == "ELA-Literacy") "ELA" else subject))

  def compare(standard: Standard, other: Standard): Int = {
    val one = normalizeSubject(standard)
    val two = normalizeSubject(other)
    compare(one.subject, two.subject, subjectOrdering)(() =>
      gradesCompare(one.grades, two.grades)(() => {
        compare(one.abbreviation, two.abbreviation, abbreviationOrdering)(() => {
          one.category.compareOpt(two.category) match {
            case 0 => one.compareCode(two)
            case int: Int => int
          }
        })
      }))
  }

  /**
   * Takes two Options and if both are Some, compares them based on their position in a provided List. If both are the
   * same, execute a block yielding a result. If either is None, the other takes precedence. If both are None, execute
   * the block yielding a result.
   */
  private def compare[T](one: Option[T], two: Option[T], list: List[T])(block: () => Int) = {
    (one, two) match {
      case (Some(valueOne), Some(valueTwo)) => list.indexOf(valueOne) - list.indexOf(valueTwo) match {
        case 0 => block()
        case int: Int => if (int > 0) 1 else -1
      }
      case (None, Some(_)) => -1
      case (Some(_), None) => 1
      case _ => block()
    }
  }

  private def gradesCompare(one: Seq[String], two: Seq[String])(block: () => Int) =
    compare(one.headOption, two.headOption, gradeOrdering)(() => one.length.compare(two.length)) match {
      case 0 => block()
      case int: Int => int
    }

  implicit class StringThing(stringOpt: Option[String]) {
    def compareOpt(other: Option[String]) = stringOpt.getOrElse("").compare(other.getOrElse(""))
  }

}
