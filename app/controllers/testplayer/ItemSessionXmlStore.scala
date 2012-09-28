package controllers.testplayer

import xml.Elem
import api.processors.FeedbackProcessor
import play.api.cache._
import play.api.Play.current

object ItemSessionXmlStore {

  val OneHourInSeconds = 60 * 60

  def addCsFeedbackIds(xml:Elem) :Elem = {
    val xmlStringWithCsFeedbackIds = FeedbackProcessor.addFeedbackIds(xml.toString())
    scala.xml.XML.loadString(xmlStringWithCsFeedbackIds)
  }

  def xmlCacheKey(itemId:String, sessionId: String) = """qti_itemId[%s]_sessionId[%s]""".format(itemId, sessionId)


  def cacheXml(xml:Elem, itemId:String, sessionId:String) {
    Cache.set( xmlCacheKey(itemId, sessionId ), xml, OneHourInSeconds )
  }

  def getCachedXml(itemId:String, sessionId:String) : Option[Elem] = Cache.getAs[Elem](xmlCacheKey(itemId, sessionId))

}
