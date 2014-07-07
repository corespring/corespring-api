package org.corespring.platform.core.models

/**
 * Defines a property 'group' by which Standards are categorized.
 */
trait StandardGroup {

  def subCategory: Option[String]
  def grades: Seq[String]
  def dotNotation: Option[String]

  /**
   * Returns true if the Standards belong to the same group, that is if they have the same grades and subcategory.
   */
  def sameGroupAs(standard: StandardGroup) =
    (this.subCategory == standard.subCategory) && (standard.grades == this.grades)

  /**
   * The group property for the Standard
   */
  def group: Option[String] = {
    // All Standards within the same group as the current Standard
    val inSameGroup = Standard.cachedStandards().filter(sameGroupAs(_)).map(_.dotNotation).flatten

    val prefix = {
      def longestCommonPrefix(a: String, b: String) = {
        var same = true
        var i = 0
        while (same && i < math.min(a.length, b.length)) {
          if (a.charAt(i) != b.charAt(i)) {
            same = false
          } else {
            i += 1
          }
        }
        a.substring(0, i)
      }

      inSameGroup.foldLeft(inSameGroup(0)) { longestCommonPrefix(_,_) }
    }

    val suffixes = inSameGroup.map({s => s.substring(prefix.length, s.length) }).filterNot(_.isEmpty).sorted

    val suffix = {
      def isLowerCaseChar(s: String) = s.matches("[a-z]")
      def isNumber(s: String) = s.matches("[0-9]")
      def isNumberWithLowerCaseChar(s: String) = s.matches("[0-9][a-z]")

      suffixes.length match {
        case 0 => ""
        case 1 => suffixes.head
        case _ => {
          if (isLowerCaseChar(suffixes.head) && isLowerCaseChar(suffixes.last)) {
            None
          } else if (isNumber(suffixes.head) && isNumberWithLowerCaseChar(suffixes.last)) {
            Some(s"${suffixes.head}-${suffixes.last.head}")
          } else {
            Some(s"${suffixes.head}-${suffixes.last}")
          }
        }
      }
    }

    suffixes.length match {
      case 0 => dotNotation
      case 1 => dotNotation
      case _ => (prefix.endsWith("."), suffix) match {
        case (true, Some(suffixVal)) => Some(s"$prefix$suffixVal")
        case (false, Some(suffixVal)) => Some(s"$prefix.$suffixVal")
        case (_, None) => Some(prefix)
      }
    }
  }

}
