package reporting.controllers

import play.api.Logger
import controllers.auth.BaseApi
import reporting.services.ReportsService
import models._
import item.Item

object Reports extends BaseApi {

  private val service: ReportsService = new ReportsService(
    Item.collection,
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
