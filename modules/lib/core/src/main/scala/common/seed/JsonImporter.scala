package common.seed

import com.mongodb.DBObject
import com.mongodb.casbah.MongoCollection
import com.mongodb.util.JSON
import common.utils.string
import java.io.File
import java.nio.charset.Charset
import play.api.Play
import play.api.Play.current
import scala.io.Codec


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
  def jsonFileToDb(jsonPath: String, coll: MongoCollection) {
    val s = io.Source.fromFile(Play.getFile(jsonPath))(new Codec(Charset.forName("UTF-8"))).mkString
    coll.insert(JSON.parse(s).asInstanceOf[DBObject])
  }

  def jsonFileToItem(jsonPath: String, coll: MongoCollection) {
    val s = io.Source.fromFile(Play.getFile(jsonPath))(new Codec(Charset.forName("UTF-8"))).mkString
    val finalObject: String = replaceLinksWithContent(s)
    insertString(finalObject, coll)
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

  /** Given a directory - insert each json file within as a dbo
    * Also check if any of the files contain duplicate ids
    * @param path
    * @param collection
    */
  def insertFilesInFolder(path: String, collection: MongoCollection) {

    val folder: File = Play.getFile(path)
    val files: List[File] = folder.listFiles.toList
    val dbos: List[DBObject] = files.map((f: File) => fileToDbo(path + "/" + f.getName))

    val hasDuplicates: Boolean = {
      val ids = dbos.map {
        (dbo: DBObject) =>
          val id = dbo.get("_id")
          if (id != null) Some(id) else None
      }.flatten
      ids.length != ids.distinct.length
    }

    if (hasDuplicates) {
      throw new RuntimeException("Contains duplicate ids: " + files)
    }

    dbos.foreach(dbo => collection.insert(dbo, collection.writeConcern))
  }

  /**
   * replace any interpolated keys with the path that they point to eg:
   * $[interpolate{/path/to/file.xml}] will be replaced with the contents of file.xml
   * @param s
   * @return
   */
  private def replaceLinksWithContent(s: String): String = {

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

    val interpolated = string.interpolate(s, loadString)
    interpolated
  }

  private def insertString(s: String, coll: MongoCollection) = {
    val dbo: DBObject = JSON.parse(s).asInstanceOf[DBObject]
    coll.insert(dbo, coll.writeConcern)
  }


  private def fileToDbo(path: String): DBObject = {
    val s = io.Source.fromFile(Play.getFile(path))(new Codec(Charset.forName("UTF8"))).mkString
    val finalObject: String = replaceLinksWithContent(s)
    JSON.parse(finalObject).asInstanceOf[DBObject]
  }

}


