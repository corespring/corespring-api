package reporting.utils

import net.quux00.simplecsv.{ CsvWriter => JavaCsvWriter }
import java.io.StringWriter

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
