package org.corespring.reporting.utils

import java.io.StringWriter
import net.quux00.simplecsv.{ CsvWriter => JavaCsvWriter }


trait CsvWriter {

  implicit class ListsToCsv(lists: List[List[String]]) {

    def toCsv: String = {
      val stringWriter = new StringWriter()
      val csvWriter = new JavaCsvWriter(stringWriter)

      try {
        lists.map(_.toArray).foreach(csvWriter.writeNext(_))
      } finally {
        csvWriter.close()
      }
      stringWriter.toString
    }

  }

}
