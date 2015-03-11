package org.corespring.reporting.utils

trait MongoMapReduceUtils {

  def fieldCheck(property: String) = property.split("\\.").foldLeft(Seq.empty[String])((acc, str) =>
    acc :+ (if (acc.isEmpty) s"this.$str" else s"${acc.last}.$str")).mkString(" && ")

}
