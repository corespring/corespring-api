import com.mongodb.casbah.Imports._
import com.mongodb.DBObject
import com.mongodb.util.JSON
import io.Codec
import java.io.File
import java.nio.charset.Charset

/**
 * Created with IntelliJ IDEA.
 * User: josh
 * Date: 8/14/12
 * Time: 3:47 PM
 * To change this template use File | Settings | File Templates.
 */

object Main {
  def main(args: Array[String]) = {
    insertMockData
  }

  def jsonToDB(jsonPath: String, coll: MongoCollection) = {
    coll.drop()
    val lines: Iterator[String] = io.Source.fromFile(new File(jsonPath))(new Codec(Charset.defaultCharset())).getLines()
    for (line <- lines) {
      println("adding document: "+line+" to collection"+coll.name)
      val wr = coll.insert(JSON.parse(line).asInstanceOf[DBObject], coll.writeConcern)
      if (!wr.getLastError.ok()){
        println("FATAL: error occured when inserting mock document")
      }
    }
  }

  def getDb: Option[MongoDB] = {
    val conn = MongoConnection("ds037047.mongolab.com", 37047)
    val mongoDb = conn.getDB("api-test")
    if (mongoDb.authenticate("corespring", "api-test")) {
      Some(mongoDb)
    } else None
  }

  def insertMockData = {
    val basePath = if (System.getenv("ON_HEROKU") == "true") "/app/conf/test-data/"
                    else "/Users/josh/git/corespring-api/conf/test-data/"
    println("using basePath: "+basePath)
    getDb match {
      case Some(mongoDb) => {
        jsonToDB(basePath + "orgs.json", mongoDb("orgs"))
        jsonToDB(basePath + "items.json", mongoDb("content"))
        jsonToDB(basePath + "collections.json", mongoDb("contentcolls"))
        jsonToDB(basePath + "apiClients.json", mongoDb("apiClients"))
        jsonToDB(basePath + "accessTokens.json", mongoDb("accessTokens"))
      }
      case None => throw new RuntimeException("could not create mongodb instance")
    }

  }
}
