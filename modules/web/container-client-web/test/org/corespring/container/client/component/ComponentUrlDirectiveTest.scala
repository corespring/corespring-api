package org.corespring.container.client.component

import org.specs2.mutable.Specification
import org.corespring.container.components.model.dependencies.ComponentMaker

class ComponentUrlDirectiveTest extends Specification with ComponentMaker {

  val comps = Seq(uiComp("1", Seq.empty), uiComp("2", Seq.empty), uiComp("3", Seq.empty, org = "org-2"))

  def applyAndUnapply(d: String, names: Seq[String]) = {
    ComponentUrlDirective(d, comps) === names and ComponentUrlDirective.unapply(names, comps) === Some(d)
  }

  def orgApplyAndUnapply(org: String, d: String, names: Seq[String]) = {
    val orgComps = comps.filter(_.id.org == org)
    OrgDirective(org, d, orgComps) === names and OrgDirective.unapply(org, names, orgComps) === Some(s"$org[$d]")
  }

  "url directive" should {
    "org[all]" in applyAndUnapply("org[all]", Seq("org-1", "org-2"))
    "org[1]" in applyAndUnapply("org[1]", Seq("org-1"))
    "org[1]+org-2[3]" in applyAndUnapply("org[1]+org-2[all]", Seq("org-1", "org-2-3"))

    "unknown-1[all]" in ComponentUrlDirective("unknown-1[all]", comps) === Seq.empty
    "unknown-1[all]+unknown2[all]" in ComponentUrlDirective("unknown-1[all]+unknown2[all]", comps) === Seq.empty
    "unknown-1[all]+org[1]" in ComponentUrlDirective("unknown-1[all]+org[1]", comps) === Seq("org-1")
  }

  "org directive" should {

    "org[all]" in orgApplyAndUnapply("org", "all", Seq("org-1", "org-2"))
    "org-2[all]" in orgApplyAndUnapply("org-2", "all", Seq("org-2-3"))

    "apply - unknown orgs are ignored" in OrgDirective("unknown", "all", comps.filter(_.id.org == "unknown")) === Seq.empty
    "unapply - unknown orgs are ignored" in OrgDirective.unapply("unknown", Seq("unknown-1"), comps.filter(_.id.org == "unknown")) === None
  }

}
