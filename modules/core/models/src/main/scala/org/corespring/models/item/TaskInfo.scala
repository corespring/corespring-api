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
