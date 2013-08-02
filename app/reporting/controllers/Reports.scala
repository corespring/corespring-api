package reporting.controllers

import controllers.auth.BaseApi
import org.corespring.platform.core.models._
import reporting.services.ReportsService
import org.corespring.platform.core.models.item.service.ItemServiceImpl
import org.corespring.platform.core.models.{Subject, Standard, ContentCollection}
import org.corespring.platform.core.models.item.Item

object Reports extends BaseApi {

  private val service: ReportsService = new ReportsService(
    ItemServiceImpl.collection,
    Subject.collection,
    ContentCollection.collection,
    Standard.collection
  )

  def index = ApiAction {
    request =>
      val availableCollections = service.getCollections
      Ok(reporting.views.html.index(availableCollections))
  }

  def getCsv(collection: String, queryType: String) = ApiAction {
    request =>
      Logger.info("getCsv: " + collection + " type: " + queryType)
      val result = service.getReport(collection, queryType)
      val out = ("" /: result) {
        (a, i) => a + i._1 + "," + i._2 + "\n"
      }
      Logger.info(out)
      //text/csv
      Ok(out).withHeaders(("Content-type", "text/csv"))
  }

  def getPrimarySubjectItemReport = ApiAction(request => Ok(service.buildPrimarySubjectItemReport).withHeaders(("Content-type", "text/csv")))

  def getStandardItemReport = ApiAction(request => Ok(service.buildStandardsItemReport).withHeaders(("Content-type", "text/csv")))

  def getContributorReport = ApiAction(request => Ok(service.buildContributorReport).withHeaders(("Content-type", "text/csv")))

  def getCollectionReport = ApiAction(request => Ok(service.buildCollectionReport).withHeaders(("Content-type", "text/csv")))
}
