package org.corespring.container.client.component

import org.corespring.container.components.model.Component

/**
 * Expand url directive to a list of component types.
 * Eg:
 *   corespring[all] => Seq(corespring-comp-1, ...) //all the comps for corespring
 *   corespring[comp-one,comp-two] => Seq(corespring-comp-one,corespring-comp-two)
 */
object ComponentUrlDirective {
  val Regex = """(.*)\[(.*)\]""".r
  def apply(d: String, comps: Seq[Component]): Seq[String] = {
    val orgDirectives = d.split("\\+").toSeq.map { od =>
      val Regex(org, directive) = od
      OrgDirective(org, directive, comps.filter(_.id.org == org))
    }
    val all = orgDirectives.fold(Seq())(_ ++ _)
    all
  }

  def unapply(types: Seq[String], comps: Seq[Component]): Option[String] = {

    val groupedTypes: Map[String, Seq[String]] = types.groupBy {
      t =>
        comps.find(_.componentType == t).map {
          _.id.org
        }.getOrElse("unknown_org")
    }

    val directives: Seq[String] = groupedTypes.toSeq.map(kv => OrgDirective.unapply(kv._1, kv._2, comps.filter(_.id.org == kv._1))).flatten
    Some(directives.mkString("+"))
  }
}

object OrgDirective {

  def apply(org: String, directive: String, comps: Seq[Component]): Seq[String] = {
    val filteredComps = directive match {
      case "all" => comps
      case _ => {
        val names = directive.split(",")
        def nameListed(c: Component): Boolean = names.exists(_ == c.id.name)
        comps.filter(nameListed)
      }
    }
    filteredComps.map(_.componentType)
  }

  def unapply(org: String, types: Seq[String], comps: Seq[Component]): Option[String] = {

    val componentTypes: Seq[String] = comps.map(_.componentType)

    val validComps = types.filter(kc => componentTypes.exists(_ == kc))

    if (validComps.length == 0) {
      None
    } else {

      val diff: Seq[String] = componentTypes.diff(validComps)
      if (diff.length == 0) {
        Some(s"$org[all]")
      } else {
        val trimmed = validComps.map(_.replace(s"$org-", ""))
        Some(s"$org[${trimmed.sorted.distinct.mkString(",")}]")
      }
    }
  }
}

