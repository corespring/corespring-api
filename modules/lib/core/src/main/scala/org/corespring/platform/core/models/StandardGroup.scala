package org.corespring.platform.core.models

/**
 * Defines a property 'group' by which Standards are categorized.
 */
trait StandardGroup {

  def subCategory: Option[String]
  def grades: Seq[String]
  def dotNotation: Option[String]

  /**
   * Returns true if the Standards belong to the same group, that is if they have the same grades, same subcategory,
   * and begin with the same first two letters.
   */
  def sameGroupAs(standard: StandardGroup) =
    (this.subCategory == standard.subCategory) && (standard.grades == this.grades) &&
      ((this.dotNotation, standard.dotNotation) match {
        case (Some(dot), Some(other)) => dot.startsWith(other.substring(0, 2))
        case (None, None) => true
        case _ => false
      })

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
      inSameGroup.length match {
        case 0 => throw new IllegalArgumentException(
          s"Could not find group for ${dotNotation.getOrElse("missing dot notation")}")
        case _ => inSameGroup.foldLeft(inSameGroup(0)) { longestCommonPrefix(_,_) }
      }
    }

    val suffixes = inSameGroup.map({s => s.substring(prefix.length, s.length) }).filterNot(_.isEmpty).sorted

    (dotNotation, subCategory) match {
      case (Some(dotNotation), Some(subCategory)) => {
        suffixes.length match {
          case 0 => Some(s"$dotNotation.$subCategory")
          case 1 => Some(s"$prefix.$subCategory}")
          case _ => prefix.endsWith(".") match {
            case true => Some(s"$prefix$subCategory")
            case _ => Some(s"$prefix.$subCategory")
          }
        }
      }
      case _ => throw new IllegalArgumentException("subCategory and dotNotation are required")
    }
  }

}
