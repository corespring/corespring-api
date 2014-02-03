package reporting.models

import org.corespring.platform.core.models.item.TaskInfo

object ReportLineResult {

  var ItemTypes: List[String] = List()
  var GradeLevel: List[String] = List()
  var PriorUse: List[String] = List()
  var LicenseType: List[String] = List()
  var Credentials: List[String] = List()

  class KeyCount(var key: String, var count: Int) {
    override def toString = key + "," + count
  }

  def zeroedKeyCountList(l: List[String]): List[KeyCount] = l.map((s: String) => new KeyCount(s, 0))

  def buildCsv(title: String, list: List[LineResult]): String = {
    val header: String =
      List(title, "Total # of Items").mkString(",") + "," +
        ItemTypes.map(_.replaceAll(",", " ")).mkString(",") + "," +
        GradeLevel.mkString(",") + "," +
        PriorUse.mkString(",") + "," +
        Credentials.mkString(",") + "," +
        LicenseType.mkString(",") + "\n"

    header + list.map(buildLineString).mkString("\n")

  }

  private def createValueList(l: List[KeyCount], sorter: (KeyCount, KeyCount) => Boolean = (a,b) => a.key < b.key) =
    l.sortWith(sorter).map(kc => kc.count)

  private def buildLineString(result: LineResult): String = {
    val outList = List(result.subject, result.total) :::
      createValueList(result.itemType, (a,b) => {TaskInfo.gradeLevelSorter(a.key, b.key) }) :::
      createValueList(result.gradeLevel) :::
      createValueList(result.priorUse) :::
      createValueList(result.credentials) :::
      createValueList(result.licenseType)

    outList.mkString(",")
  }

  case class LineResult(subject: String,
    total: Int = 0,
    itemType: List[KeyCount] = zeroedKeyCountList(ItemTypes),
    gradeLevel: List[KeyCount] = zeroedKeyCountList(GradeLevel),
    priorUse: List[KeyCount] = zeroedKeyCountList(PriorUse),
    credentials: List[KeyCount] = zeroedKeyCountList(Credentials),
    licenseType: List[KeyCount] = zeroedKeyCountList(LicenseType))

}

