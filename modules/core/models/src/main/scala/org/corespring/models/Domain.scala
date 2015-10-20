package org.corespring.models

case class StandardDomains(ela: Seq[Domain], math: Seq[Domain])
case class Domain(name: String, standards: Seq[String])

object Domain {

  private def toDomain(key: String, standards: Seq[Standard]) = {
    Domain(key, standards.map(_.dotNotation).flatten.sorted)
  }

  def fromStandards(standards: Seq[Standard], getDomain: Standard => Option[String]): Seq[Domain] = {
    val grouped = standards.groupBy(s => getDomain(s).filterNot(_.isEmpty).getOrElse("no-category"))
    grouped.toSeq.map(Function.tupled(toDomain)).sortBy(_.name)
  }
}
