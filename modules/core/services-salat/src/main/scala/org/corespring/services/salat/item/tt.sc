def mkPropertyTest(field:String) = {
  val parts = field.split("\\.")
  def addBits(acc: Seq[String], str:String) = acc :+ s"${acc.last}.$str"
  val added = parts.foldLeft(Seq("this"))(addBits).drop(1)
  added.mkString(" && ")
}

fieldCheck( "taskInfo.itemType")