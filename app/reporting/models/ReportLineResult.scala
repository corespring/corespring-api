package reporting.models

import org.corespring.platform.core.models.item.TaskInfo
import java.io.StringWriter
import reporting.utils.CsvWriter

object ReportLineResult extends CsvWriter {

  var ItemTypes: List[String] = List.empty[String]
  var GradeLevel: List[String] = List.empty[String]
  var PriorUse: List[String] = List.empty[String]
  var LicenseType: List[String] = List.empty[String]
  var Credentials: List[String] = List.empty[String]

  class KeyCount(var key: String, var count: Int) {
    override def toString = key + "," + count
  }

  def zeroedKeyCountList(l: List[String]): List[KeyCount] = l.map((s: String) => new KeyCount(s, 0))

  def buildCsv(title: String, list: List[LineResult]): String = {
    val header = List(List(title), List("Total # of Items"), ItemTypes, GradeLevel.sortWith(TaskInfo.gradeLevelSorter),
      PriorUse, Credentials, LicenseType).flatten
    val lines: List[List[String]] = list.map(buildLine)
    (List(header) ::: lines).toCsv
  }

  def createValueList(l: List[KeyCount], sorter: (KeyCount, KeyCount) => Boolean = (a,b) => a.key <= b.key) =
    l.sortWith(sorter).map(_.count.toString)

  private def buildLine(result: LineResult): List[String] = {
    List(List(result.subject), List(result.total.toString),
      createValueList(result.itemType),
      createValueList(result.gradeLevel, (a: KeyCount, b: KeyCount) => {TaskInfo.gradeLevelSorter(a.key, b.key) }) :::
      createValueList(result.priorUse),
      createValueList(result.credentials),
      createValueList(result.licenseType)).flatten
  }

  case class LineResult(subject: String,
    total: Int = 0,
    itemType: List[KeyCount] = zeroedKeyCountList(ItemTypes),
    gradeLevel: List[KeyCount] = zeroedKeyCountList(GradeLevel),
    priorUse: List[KeyCount] = zeroedKeyCountList(PriorUse),
    credentials: List[KeyCount] = zeroedKeyCountList(Credentials),
    licenseType: List[KeyCount] = zeroedKeyCountList(LicenseType))

}

