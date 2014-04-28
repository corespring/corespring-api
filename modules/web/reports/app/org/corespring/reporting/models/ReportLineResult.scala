package org.corespring.reporting.models

import org.corespring.platform.core.models.item.TaskInfo
import org.corespring.reporting.utils.CsvWriter

object ReportLineResult extends CsvWriter {

  var ItemTypes: List[String] = List.empty[String]
  var GradeLevel: List[String] = List.empty[String]
  var PriorUse: List[String] = List.empty[String]
  var LicenseType: List[String] = List.empty[String]
  var Credentials: List[String] = List.empty[String]
  var Published: List[String] = List("false", "true")

  class KeyCount[T](var key: T, var count: Int) {
    override def toString = key + "," + count
  }

  def zeroedKeyCountList[T](l: List[T]): List[KeyCount[T]] = l.map((t: T) => new KeyCount(t, 0))

  def buildCsv(title: String, list: List[LineResult],
               sorter: (String, String) => Boolean = (a, b) => a <= b): String = {
    val header = List(List(title), List("Total # of Items"), ItemTypes, GradeLevel.sortWith(TaskInfo.gradeLevelSorter),
      PriorUse, Credentials, LicenseType, List("Unpublished", "Published")).flatten
    val lines: List[List[String]] = list.map(buildLine).sortWith((a,b) => sorter(a.head, b.head))
    (List(header) ::: lines).toCsv
  }

  def createValueList(l: List[KeyCount[String]]): List[String] =
    createValueList(l, (a: KeyCount[String], b: KeyCount[String]) => a.key <= b.key)

  def createValueList[T](l: List[KeyCount[T]], sorter: (KeyCount[T], KeyCount[T]) => Boolean) =
    l.sortWith(sorter).map(_.count.toString)

  private def buildLine(result: LineResult): List[String] = {
    List(List(result.subject), List(result.total.toString),
      createValueList(result.itemType),
      createValueList(result.gradeLevel, (a: KeyCount[String], b: KeyCount[String]) => { TaskInfo.gradeLevelSorter(a.key, b.key) }) :::
      createValueList(result.priorUse),
      createValueList(result.credentials),
      createValueList(result.licenseType),
      createValueList(result.published)).flatten
  }

  case class LineResult(subject: String,
    total: Int = 0,
    itemType: List[KeyCount[String]] = zeroedKeyCountList(ItemTypes),
    gradeLevel: List[KeyCount[String]] = zeroedKeyCountList(GradeLevel),
    priorUse: List[KeyCount[String]] = zeroedKeyCountList(PriorUse),
    credentials: List[KeyCount[String]] = zeroedKeyCountList(Credentials),
    licenseType: List[KeyCount[String]] = zeroedKeyCountList(LicenseType),
    published: List[KeyCount[String]] = zeroedKeyCountList(Published))

}

