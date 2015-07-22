package org.corespring.models.item

import com.mongodb.casbah.Imports._

case class TaskInfo(
  extended: Map[String, DBObject] = Map(),
  subjects: Option[Subjects] = None,
  gradeLevel: Seq[String] = Seq.empty,
  title: Option[String] = None,
  description: Option[String] = None,
  itemType: Option[String] = None,
  domains: Set[String] = Set.empty) {
  def cloneInfo(titlePrefix: String): TaskInfo = {
    require(titlePrefix != null)
    copy(title = title.map(t => if (t.isEmpty) titlePrefix else titlePrefix + " " + t) orElse Some(titlePrefix))
  }
}

object TaskInfo {

  val gradeLevelSorter: (String, String) => Boolean = (a, b) => {
    val reference = List("PK", "KG", "01", "02", "03", "04", "05", "06", "07", "08", "09",
      "10", "11", "12", "13", "PS", "AP", "UG")
    (Option(reference.indexOf(a)), Option(reference.indexOf(b))) match {
      case (Some(one), Some(two)) => (one <= two)
      case _ => a <= b
    }
  }
}
