package reporting.models

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
        ItemTypes.mkString(",") + "," +
        GradeLevel.mkString(",") + "," +
        PriorUse.mkString(",") + "," +
        Credentials.mkString(",") + "," +
        LicenseType.mkString(",") + "\n"

    header + list.map(buildLineString).mkString("\n")
  }

  private def createValueList(l: List[KeyCount]) = l.map((kc: KeyCount) => kc.count)

  private def buildLineString(result: LineResult): String = {
    val outList = List(result.subject, result.total) :::
      createValueList(result.itemType) :::
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

