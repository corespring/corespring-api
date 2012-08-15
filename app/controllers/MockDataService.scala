package controllers

import com.mongodb.casbah.Imports._
import scala.Some
import io.Codec
import java.nio.charset.Charset
import com.mongodb.util.JSON
import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: josh
 * Date: 8/13/12
 * Time: 12:32 PM
 * To change this template use File | Settings | File Templates.
 */

object MockDataService {
  private def jsonToDB(jsonPath:String, coll:MongoCollection) = {
    coll.drop()
    val lines:Iterator[String] = io.Source.fromFile(new File(jsonPath))(new Codec(Charset.defaultCharset())).getLines()
    for (line <- lines) {
      coll.insert(JSON.parse(line).asInstanceOf[DBObject],coll.writeConcern)
    }
  }
  private def getDb:Option[MongoDB] = {
    val conn = MongoConnection("ds037047.mongolab.com",37047)
    val mongoDb = conn.getDB("api-test")
    if(mongoDb.authenticate("corespring","api-test")){
      Some(mongoDb)
    }else None
  }

  def insertMockData = {
    val basePath = "/Users/josh/git/corespring-api/conf/test-data/"
    getDb match {
      case Some(mongoDb) => {
        jsonToDB(basePath+"orgs.json", mongoDb("orgs"))
        jsonToDB(basePath+"items.json", mongoDb("content"))
        jsonToDB(basePath+"collections.json", mongoDb("contentcolls"))
        jsonToDB(basePath+"apiClients.json", mongoDb("apiClients"))
        jsonToDB(basePath+"accessTokens.json", mongoDb("accessTokens"))
      }
      case None => throw new RuntimeException("could not create mongodb instance")
    }

  }

}
