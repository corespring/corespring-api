package common.seed

import com.mongodb.casbah.MongoCollection
import play.api.Play
import scala.io.Codec
import java.nio.charset.Charset
import com.mongodb.DBObject
import com.mongodb.util.JSON
import play.api.Play.current
import java.io.File
import org.bson.types.ObjectId

object JsonImporter {


  /**
   * Insert each line of the file as a single object
   * @param jsonPath
   * @param coll
   */
  def jsonLinesToDb(jsonPath: String, coll: MongoCollection) {

    val lines: Iterator[String] = io.Source.fromFile(Play.getFile(jsonPath))(new Codec(Charset.forName("UTF-8"))).getLines()
    for (line <- lines) {
      if (line != null && line != "") {
        insertString(line, coll)
      }
    }
  }

  /**
   * Read a complete file as a single json object
   * @param jsonPath
   * @param coll
   */
  def jsonFileToDb(jsonPath: String, coll: MongoCollection ) {
    val s = io.Source.fromFile(Play.getFile(jsonPath))(new Codec(Charset.forName("UTF-8"))).mkString
    coll.insert(JSON.parse(s).asInstanceOf[DBObject])
  }

  def jsonFileToItem(jsonPath: String, coll: MongoCollection) {
    val s = io.Source.fromFile(Play.getFile(jsonPath))(new Codec(Charset.forName("UTF-8"))).mkString
    val finalObject: String = replaceLinksWithContent(s)
    insertString(finalObject, coll, true)
  }

  /**
   * Insert a json file that is a json array of items into the db
   * @param path = path to json
   * @param coll
   */
  def jsonFileListToDb(path: String, coll: MongoCollection) {
    coll.drop()
    val listString = io.Source.fromFile(Play.getFile(path))(new Codec(Charset.forName("UTF-8"))).mkString
    val dbList = com.mongodb.util.JSON.parse(listString).asInstanceOf[com.mongodb.BasicDBList]
    dbList.toArray.toList.foreach(dbo => coll.insert(dbo.asInstanceOf[DBObject]))
  }

  /**
   * replace any interpolated keys with the path that they point to eg:
   * $[interpolate{/path/to/file.xml}] will be replaced with the contents of file.xml
   * @param s
   * @return
   */
  def replaceLinksWithContent(s: String): String = {

    /**
     * Load a string from a given path, remove new lines and escape "
     * @param path
     * @return
     */
    def loadString(path: String): String = {
      val s = io.Source.fromFile(Play.getFile(path))(new Codec(Charset.forName("UTF-8"))).mkString
      val lines = s
        .replace("\n", "\\\n")
        .replace("\"", "'")
        .replace("$", "\\\\\\$")
        .replace("{", "\\\\{")
        .replace("}", "\\\\}")
      lines

    }

    val interpolated = StringUtils.interpolate(s, loadString)
    interpolated
  }

  def insertString(s: String, coll: MongoCollection, printId: Boolean = false) = {
    val dbo: DBObject = JSON.parse(s).asInstanceOf[DBObject]

    val NO_ID = "NO_ID"
    val id = if (dbo.get("_id") != null) dbo.get("_id").toString else NO_ID

    if (NO_ID.equals(id)) {
      coll.insert(dbo, coll.writeConcern)
    } else {
      coll.findOneByID(new ObjectId(id)) match {
        case Some(obj) => throw new RuntimeException("Item already exisits: " + id + " collection: " + coll.name)
        case _ => {
          coll.insert(dbo, coll.writeConcern)
        }
      }
    }
  }


  def insertFilesInFolder(path: String, collection : MongoCollection) {
    val folder: File = Play.getFile(path)
    for (file <- folder.listFiles) {
      jsonFileToItem(path + "/" + file.getName, collection)
    }
  }


}

