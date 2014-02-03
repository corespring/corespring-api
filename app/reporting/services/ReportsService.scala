package reporting.services

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Implicits._
import com.mongodb.casbah.map_reduce._
import com.mongodb.{ BasicDBObject, DBObject }
import reporting.models.ReportLineResult.KeyCount
import reporting.models.ReportLineResult
import org.bson.types.ObjectId

import org.corespring.platform.core.models.{Standard, Subject, ContentCollection}
import org.corespring.common.utils.string
import org.corespring.platform.core.services.item.ItemServiceImpl
import reporting.models.ReportLineResult.LineResult
import scala.Some
import scala.util.Sorting
import org.corespring.platform.core.models.item.TaskInfo

object ReportsService extends ReportsService(ItemServiceImpl.collection, Subject.collection,
  ContentCollection.collection, Standard.collection)

class ReportsService(ItemCollection: MongoCollection,
  SubjectCollection: MongoCollection,
  CollectionsCollection: MongoCollection,
  StandardCollection: MongoCollection) {

  def getReport(collectionId: String, queryType: String): List[(String, String)] = {

    populateHeaders

    val mapTemplateFn: JSFunction =
      """ function m() {
            if( "${collectionId}" != "all" && (!this.collectionId || !(this.collectionId == "${collectionId}")) ){
              return;
            }
            try {
            if(!this.${property}){
              emit("unknown", 1);
              return;
            }
            emit(this.${property}, 1);
            } catch (e) {
              emit("unknown", 1);
              return;
            }
          };"""

    val mapFn: JSFunction = interpolate(mapTemplateFn, Map("collectionId" -> collectionId, "property" -> queryType))
    val cmd = MapReduceCommand(ItemCollection.name, mapFn, JSFunctions.ReduceFn, MapReduceInlineOutput)

    val result: MapReduceResult = ItemCollection.mapReduce(cmd)
    val inlineResult: MapReduceInlineResult = result.asInstanceOf[MapReduceInlineResult]

    def collectionIdToName(id: String): String = {
      if (id == "unknown") return "?"
      ContentCollection.findOneById(new ObjectId(id)) match {
        case Some(c) => c.name
        case _ => "?"
      }
    }

    val collectionName = collectionId match {
      case "all" => "?"
      case _ => collectionIdToName(collectionId)
    }

    if (queryType.toLowerCase == "collectionid") {
      List((collectionName + ": " + queryType, "Total")) ::: inlineResult.map((dbo: Any) => {
        dbo match {
          case foundDbo: DBObject => {
            println(foundDbo.get("_id").toString)
            (collectionIdToName(foundDbo.get("_id").toString), foundDbo.get("value").toString)
          }
        }
      }).toList
    } else {
      List((collectionName + ": " + queryType, "Total")) ::: inlineResult.map((dbo: Any) => {
        dbo match {
          case foundDbo: DBObject => {
            (foundDbo.get("_id").toString, foundDbo.get("value").toString)
          }
        }
      }).toList
    }
  }

  def populateHeaders {
    val defaultSorter = (a: String, b: String) => a < b

    def mapToDistinctList(field: String, sorter: (String, String) => Boolean = defaultSorter): List[String] = {
      val distResult = ItemCollection.distinct(field)
      if (distResult == null) return List()
      val distStringResult = distResult.map(p => if (p != null) p.toString else "")
      if (distStringResult == null) return List()

      distStringResult.filter(_ != "").toList.sortWith(sorter)
    }

    ReportLineResult.ItemTypes = mapToDistinctList("taskInfo.itemType")
    ReportLineResult.GradeLevel = mapToDistinctList("taskInfo.gradeLevel", TaskInfo.gradeLevelSorter)
    ReportLineResult.PriorUse = mapToDistinctList("priorUse")
    ReportLineResult.LicenseType = mapToDistinctList("contributorDetails.licenseType")
    ReportLineResult.Credentials = mapToDistinctList("contributorDetails.credentials")
  }

  /**
   * Build a csv where each line is for a primary subject and the columns are counts of a specific set of item properties.
   * @return
   */
  def buildPrimarySubjectReport() = {
    populateHeaders

    val lineResults: List[LineResult] = SubjectCollection.map((dbo: DBObject) => {

      val subject = dbo.get("subject").asInstanceOf[String]
      val category = dbo.get("category").asInstanceOf[String]
      val finalKey = buildSubjectString(category, subject)

      val query = new BasicDBObject()
      query.put("taskInfo.subjects.primary", dbo.get("_id").asInstanceOf[ObjectId])
      buildLineResult(query, finalKey)
    }).toList
    ReportLineResult.buildCsv("Primary Subject", lineResults)
  }

  private def buildSubjectString(category: String, subject: String): String = {
    if (subject == null || subject.length == 0) {
      return category.replaceAll(",", "")
    }
    (category + ": " + subject).replaceAll(",", "")
  }

  def buildStandardsReport() = {

    populateHeaders

    val lineResults: List[LineResult] = StandardCollection.map((dbo: DBObject) => {

      val dotNotation = dbo.get("dotNotation").asInstanceOf[String]
      val subject = dbo.get("subject").asInstanceOf[String]
      val category = dbo.get("category").asInstanceOf[String]
      val finalKey = List(dotNotation, subject, category).filterNot(_.isEmpty).mkString(":")

      val query = new BasicDBObject()
      query.put("standards", dbo.get("dotNotation").asInstanceOf[String])
      buildLineResult(query, finalKey)
    }).toList
    ReportLineResult.buildCsv("Standards", lineResults)

  }

  /**
   * Build a csv where each line is a contributor and the columns are counts of a specific set of item properties.
   * @return
   */
  def buildContributorReport() = {

    populateHeaders

    val mapFn: JSFunction = JSFunctions.SimplePropertyMapFnTemplate("contributorDetails.contributor")

    val cmd = MapReduceCommand(ItemCollection.name, mapFn, JSFunctions.ReduceFn, MapReduceInlineOutput)
    val result: MapReduceResult = ItemCollection.mapReduce(cmd)
    val inlineResult: MapReduceInlineResult = result.asInstanceOf[MapReduceInlineResult]

    val lineResults: List[LineResult] = inlineResult.map((dbo: DBObject) => {
      val query = new BasicDBObject()
      val finalKey = dbo.get("_id").asInstanceOf[String]
      query.put("contributorDetails.contributor", dbo.get("_id").asInstanceOf[String])
      buildLineResult(query, finalKey)
    }).toList
    ReportLineResult.buildCsv("Contributor", lineResults)
  }

  /**
   * Build a csv where each line is a collectionId and the columns are counts of a specific set of item properties.
   * @return
   */
  def buildCollectionReport() = {
    populateHeaders

    val lineResults: List[LineResult] = CollectionsCollection.map((dbo: DBObject) => {
      val name = dbo.get("name").asInstanceOf[String]
      val query = new BasicDBObject()
      query.put("collectionId", dbo.get("_id").toString)
      buildLineResult(query, name)
    }).toList
    ReportLineResult.buildCsv("Collection", lineResults)
  }

  def buildLineResult(query: BasicDBObject, finalKey: String) = {

    ItemCollection.count(query) match {
      case 0 => new LineResult(finalKey)
      case c: Long if c > 0 => buildLineResultFromQuery(query, c.toInt, finalKey)
    }
  }

  /**
   * Cached data for all reports.
   *
   * TODO: These should be written to the filesystem if they start to take up too much memory.
   */
  private var reportCache = Map.empty[String, String]

  def getCollections: List[(String, String)] = ContentCollection.findAll().toList.map {
    c => (c.name.toString, c.id.toString)
  }

  /**
   * Run the map reduce for the given property and with the given query, then place the counts for each value into
   * the KeyCount that matches the item.
   * @param keyCounts
   * @param query
   */
  private def runMapReduceForProperty(keyCounts: List[KeyCount],
    query: BasicDBObject,
    mapFn: JSFunction) {

    val cmd = MapReduceCommand(ItemCollection.name, mapFn, JSFunctions.ReduceFn, MapReduceInlineOutput, Some(query))
    val result: MapReduceResult = ItemCollection.mapReduce(cmd)
    val inlineResult: MapReduceInlineResult = result match {
      case result: MapReduceInlineResult => result
      case error: MapReduceError =>
        throw new RuntimeException(s"There was an error in the map-reduce operation:\n${error.toString}")
    }

    /**
     * Put the value into the corresponding KeyCount holder.
     * @param keyCounts
     * @param key
     * @param value
     */
    def putValueIntoKeyCount(keyCounts: List[KeyCount], key: String, value: Int) = {

      val keyCountList = keyCounts.filter((kc: KeyCount) => kc.key == key)

      keyCountList.length match {
        case 0 => //do nothing
        case 1 => keyCountList(0).count = value
        case _ => throw new RuntimeException("unexpected count for key: " + key)
      }
    }

    inlineResult.foreach((dbo: DBObject) => {
      val intValue = dbo.get("value").asInstanceOf[Double].toInt
      val id = dbo.get("_id")

      if (id.isInstanceOf[Double]) {
        val d = id.asInstanceOf[Double]
        if (d < 10) {
          putValueIntoKeyCount(keyCounts, d.toInt.toString, intValue)
        } else {
          putValueIntoKeyCount(keyCounts, "0" + d.toInt, intValue)
        }
      } else if (id.isInstanceOf[String]) {
        putValueIntoKeyCount(keyCounts, id.asInstanceOf[String], intValue)
      }
    })
  }

  object JSFunctions {

    def SimplePropertyMapFnTemplate(property: String): JSFunction =
      s"""function m() {
        try {
          if (this.$property) {
            emit(this.$property, 1);
          }
        } catch (e) {
        }
      };"""

    def ArrayPropertyMapTemplateFn(property: String): JSFunction = {
      val fieldCheck =
        property.split("\\.").foldLeft(Seq.empty[String])((acc, str) =>
          acc :+ (if (acc.isEmpty) s"this.$str" else s"${acc.last}.$str")).mkString(" && ")
      s"""function m() {
        if ($fieldCheck && (Object.prototype.toString.call(this.$property) === '[object Array]')) {
          for (var i = 0; i < this.$property.length; i++) {
            emit(this.${property}[i], 1);
          }
        }
      }"""
    }

    val ReduceFn: JSFunction =
      """function r(key, values) {
        var count = 0;
        for (index in values) {
          count += values[index];
        }
        return count;
      }"""
  }

  /**
   * Build a single line of counts for the given query.
   * @param query
   * @param total
   * @param key
   * @return
   */
  def buildLineResultFromQuery(query: BasicDBObject, total: Int, key: String): LineResult = {

    val itemTypeKeyCounts = ReportLineResult.zeroedKeyCountList(ReportLineResult.ItemTypes)
    runMapReduceForProperty(itemTypeKeyCounts, query, JSFunctions.SimplePropertyMapFnTemplate("taskInfo.itemType"))

    val gradeLevelKeyCounts = ReportLineResult.zeroedKeyCountList(ReportLineResult.GradeLevel)
    runMapReduceForProperty(gradeLevelKeyCounts, query, JSFunctions.ArrayPropertyMapTemplateFn("taskInfo.gradeLevel"))

    val priorUseKeyCounts = ReportLineResult.zeroedKeyCountList(ReportLineResult.PriorUse)
    runMapReduceForProperty(priorUseKeyCounts, query, JSFunctions.SimplePropertyMapFnTemplate("priorUse"))

    val credentialsKeyCount = ReportLineResult.zeroedKeyCountList(ReportLineResult.Credentials)
    runMapReduceForProperty(credentialsKeyCount, query, JSFunctions.SimplePropertyMapFnTemplate("contributorDetails.credentials"))

    val licenseTypeKeyCount = ReportLineResult.zeroedKeyCountList(ReportLineResult.LicenseType)
    runMapReduceForProperty(licenseTypeKeyCount, query, JSFunctions.SimplePropertyMapFnTemplate("contributorDetails.licenseType"))

    new LineResult(key,
      total,
      itemTypeKeyCounts,
      gradeLevelKeyCounts,
      priorUseKeyCounts,
      credentialsKeyCount,
      licenseTypeKeyCount)
  }

  private def interpolate(text: String, vars: Map[String, String]) = {
    string.interpolate(text, (k) => vars.getOrElse(k, ""), """\$\{([^}]+)\}""".r)
  }

}

