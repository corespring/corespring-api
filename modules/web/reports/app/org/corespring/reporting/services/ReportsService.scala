package org.corespring.reporting.services

import com.mongodb.casbah.Implicits._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.map_reduce._
import com.mongodb.{ BasicDBObject, DBObject }
import org.bson.types.ObjectId
import org.corespring.common.utils.string
import org.corespring.platform.core.models.item.TaskInfo
import org.corespring.platform.core.models.{Subject, Standard, ContentCollection}
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.reporting.models.ReportLineResult
import org.corespring.reporting.models.ReportLineResult.{KeyCount, LineResult}
import org.corespring.reporting.utils.CsvWriter
import scala.Some

object ReportsService extends ReportsService(ItemServiceWired.collection, Subject.collection, ContentCollection.collection, Standard.collection)

class ReportsService(ItemCollection: MongoCollection,
                     SubjectCollection: MongoCollection,
                     CollectionsCollection: MongoCollection,
                     StandardCollection: MongoCollection) extends CsvWriter{


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

    val loadValues : PartialFunction[DBObject,(String, String)] = if (queryType.toLowerCase == "collectionid") {
      case o:DBObject => (collectionIdToName(o.get("_id").toString), o.get("value").toString)
    } else {
      case foundDbo: DBObject =>  (foundDbo.get("_id").toString, foundDbo.get("value").toString)
    }

    val values = inlineResult.toList.map(loadValues)
    List((collectionName + ": " + queryType, "Total")) ::: values
  }

  val defaultSorter = (a: String, b: String) => a < b

  def mapToDistinctList(field: String, sorter: (String, String) => Boolean = defaultSorter): List[String] = {
    val distResult = ItemCollection.distinct(field)
    if (distResult == null) return List()
    val distStringResult = distResult.map(p => if (p != null) p.toString else "")
    if (distStringResult == null) return List()

    distStringResult.filter(_ != "").toList.sortWith(sorter)
  }

  def populateHeaders {
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
  def buildPrimarySubjectReport: String = {

    populateHeaders

    val lineResults: List[LineResult] = SubjectCollection.map((dbo: DBObject) => {

      val subject = dbo.get("subject").asInstanceOf[String]
      val category = dbo.get("category").asInstanceOf[String]
      val finalKey = buildSubjectString(category, subject)

      val query = baseQuery
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

  def buildStandardsReport: String = {

    populateHeaders

    val lineResults: List[LineResult] = StandardCollection.map((dbo: DBObject) => {

      val dotNotation = dbo.get("dotNotation").asInstanceOf[String]
      val subject = dbo.get("subject").asInstanceOf[String]
      val category = dbo.get("category").asInstanceOf[String]
      val finalKey = List(dotNotation, subject, category).filterNot(_.isEmpty).mkString(":")

      val query = baseQuery
      query.put("standards", dbo.get("dotNotation").asInstanceOf[String])
      buildLineResult(query, finalKey)
    }).toList
    ReportLineResult.buildCsv("Standards", lineResults,
      (a: String, b: String) => Standard.sorter(a.split(":").head ,b.split(":").head))

  }

  /**
   * Build a csv where each line is a contributor and the columns are counts of a specific set of item properties.
   * @return
   */
  def buildContributorReport: String = {

    populateHeaders

    val mapFn: JSFunction = JSFunctions.SimplePropertyMapFnTemplate("contributorDetails.contributor")

    val cmd = MapReduceCommand(ItemCollection.name, mapFn, JSFunctions.ReduceFn, MapReduceInlineOutput)
    val result: MapReduceResult = ItemCollection.mapReduce(cmd)
    val inlineResult: MapReduceInlineResult = result.asInstanceOf[MapReduceInlineResult]

    val lineResults: List[LineResult] = inlineResult.map((dbo: DBObject) => {
      val query = baseQuery
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
  def buildCollectionReport: String = {

    populateHeaders

    val lineResults: List[LineResult] = CollectionsCollection.map((dbo: DBObject) => {
      val name = dbo.get("name").asInstanceOf[String]
      val query = baseQuery
      query.put("collectionId", dbo.get("_id").toString)
      buildLineResult(query, name)
    }).toList
    ReportLineResult.buildCsv("Collection", lineResults)
  }

  def buildLineResult(query: BasicDBObject, finalKey: String, sorter: (String, String) => Boolean = defaultSorter) = {

    ItemCollection.count(query) match {
      case 0 => new LineResult(finalKey)
      case c: Long if c > 0 => buildLineResultFromQuery(query, c.toInt, finalKey, sorter)
    }
  }

  def buildStandardsByCollectionReport() = {
    val collections = CollectionsCollection.find().toIterator.toSeq
    val header = "Standards" :: collections.map(_.get("_id").asInstanceOf[ObjectId].toString).toList
    val collectionIds = collections.map(_.get("_id").asInstanceOf[ObjectId])
    val lines = mapToDistinctList("standards", Standard.sorter).map(standard => {
      val collectionsKeyCounts = ReportLineResult.zeroedKeyCountList(collectionIds.map(_.toString).toList)
      runMapReduceForProperty(collectionsKeyCounts, new BasicDBObject("standards", standard), JSFunctions.SimplePropertyMapFnTemplate("collectionId"))
      standard +: ReportLineResult.createValueList(collectionsKeyCounts)
    })
    (List(header) ++ lines).toCsv
  }

  def getCollections: List[(String, String)] = ContentCollection.findAll().toList.map {
    c => (c.name.toString, c.id.toString)
  }

  /**
   * Run the map reduce for the given property and with the given query, then place the counts for each value into
   * the KeyCount that matches the item.
   */
  private def runMapReduceForProperty[T](keyCounts: List[KeyCount[T]],
    query: BasicDBObject,
    mapTemplateFn: JSFunction ) {
    val cmd = MapReduceCommand(ItemCollection.name, mapTemplateFn, JSFunctions.ReduceFn, MapReduceInlineOutput, Some(query))
    val result: MapReduceResult = ItemCollection.mapReduce(cmd)
    val inlineResult: MapReduceInlineResult = result.asInstanceOf[MapReduceInlineResult]

    /**
     * Put the value into the corresponding KeyCount holder.
     * @param keyCounts
     * @param key
     * @param value
     */
    def putValueIntoKeyCount[T](keyCounts: List[KeyCount[T]], key: T, value: Int) = {
      val keyCountList = keyCounts.filter((kc: KeyCount[T]) => kc.key == key)

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
          putValueIntoKeyCount[T](keyCounts, d.toInt.toString.asInstanceOf[T], intValue)
        } else {
          putValueIntoKeyCount[T](keyCounts, ("0" + d.toInt.toString).asInstanceOf[T], intValue)
        }
      } else if (id.isInstanceOf[String]) {
        putValueIntoKeyCount[T](keyCounts, id.asInstanceOf[T], intValue)
      }
    })
  }

  object JSFunctions {

    def SimplePropertyMapFnTemplate(property: String): JSFunction =
      s"""function m() {
        try {
          if (this.$property !== undefined) {
            emit(this.$property.toString(), 1);
          } else {
            emit("false", 1);
          }
        } catch (e) {
        }
      }"""

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
   */
  def buildLineResultFromQuery(query: BasicDBObject, total: Int, key: String,
                               sorter: (String, String) => Boolean): LineResult = {

    val itemTypeKeyCounts = ReportLineResult.zeroedKeyCountList[String](ReportLineResult.ItemTypes)
    runMapReduceForProperty[String](itemTypeKeyCounts, query, JSFunctions.SimplePropertyMapFnTemplate("taskInfo.itemType"))

    val gradeLevelKeyCounts = ReportLineResult.zeroedKeyCountList[String](ReportLineResult.GradeLevel)
    runMapReduceForProperty[String](gradeLevelKeyCounts, query, JSFunctions.ArrayPropertyMapTemplateFn("taskInfo.gradeLevel"))

    val priorUseKeyCounts = ReportLineResult.zeroedKeyCountList[String](ReportLineResult.PriorUse)
    runMapReduceForProperty[String](priorUseKeyCounts, query, JSFunctions.SimplePropertyMapFnTemplate("priorUse"))

    val credentialsKeyCount = ReportLineResult.zeroedKeyCountList[String](ReportLineResult.Credentials)
    runMapReduceForProperty[String](credentialsKeyCount, query, JSFunctions.SimplePropertyMapFnTemplate("contributorDetails.credentials"))

    val licenseTypeKeyCount = ReportLineResult.zeroedKeyCountList[String](ReportLineResult.LicenseType)
    runMapReduceForProperty[String](licenseTypeKeyCount, query, JSFunctions.SimplePropertyMapFnTemplate("contributorDetails.licenseType"))

    val publishedKeyCount = ReportLineResult.zeroedKeyCountList[String](ReportLineResult.Published)
    runMapReduceForProperty[String](publishedKeyCount, query, JSFunctions.SimplePropertyMapFnTemplate("published"))

    new LineResult(key,
      total,
      itemTypeKeyCounts,
      gradeLevelKeyCounts,
      priorUseKeyCounts,
      credentialsKeyCount,
      licenseTypeKeyCount,
      publishedKeyCount)
  }

  private def interpolate(text: String, vars: Map[String, String]) = {
    string.interpolate(text, (k) => vars.getOrElse(k, ""), """\$\{([^}]+)\}""".r)
  }

  private def baseQuery = new BasicDBObject("collectionId",
    new BasicDBObject("$ne", ContentCollection.archiveCollId.toString))

}
