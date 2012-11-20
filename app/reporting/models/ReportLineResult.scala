package reporting.models

object ReportLineResult {


  //TODO - These should really be in the db
  val ItemTypes: List[String] = "Multiple Choice,True/False,Yes/No,Matching,Ordering,Text with Questions,Multiple True/False,Constructed Response - Short Answer,Constructed Response - Essay,Project,Performance Task,Activity,Other".split(",").toList

  val GradeLevel: List[String] = "PK,KG,01,02,03,04,05,06,07,08,09,10,11,12,13,PS,AP,UG,Other".split(",").toList

  val PriorUse: List[String] = "Formative,Interim,Benchmark,Summative,Other".split(",").toList

  val LicenseType: List[String] = "CC BY,CC BY-SA,CC BY-NC,CC BY-ND,CC BY-NC-SA".split(",").toList

  val Credentials: List[String] = "Assessment Developer,Test Item Writer,State Department of Education,District Item Writer,Teacher,Student,School Network,CMO,Other".split(",").toList

  class KeyCount(var key:String, var count: Int){
    override def toString = key + "," + count
  }

  def zeroedKeyCountList(l: List[String]) : List[KeyCount] = l.map( (s:String) => new KeyCount(s,0))

  def buildCsv(title:String, list: List[LineResult]): String = {
    val header: String =
      List(title, "Total # of Items").mkString(",") + "," +
        ItemTypes.mkString(",") + "," +
        GradeLevel.mkString(",") + "," +
        PriorUse.mkString(",") + "," +
        Credentials.mkString(",") + "," +
        LicenseType.mkString(",") + "\n"

    header + list.map(buildLineString).mkString("\n")
  }

  private def createValueList(l: List[KeyCount]) = l.map((kc:KeyCount) => kc.count)

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
                               licenseType: List[KeyCount] = zeroedKeyCountList(LicenseType)
                                )

}

