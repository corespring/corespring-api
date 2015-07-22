import sbt._
import sbt.Keys._

object Indexing{

  import Utils._

  val index = TaskKey[Unit]("index")
  val indexTask = index <<= (streams) map safeIndex

  def safeIndex(s: TaskStreams): Unit = {
    lazy val isRemoteIndexingAllowed = System.getProperty("allow.remote.indexing", "false") == "true"
    val mongoUri = getEnv("ENV_MONGO_URI").getOrElse("mongodb://localhost:27017/api")
    val elasticSearchUri = getEnv("BONSAI_URL").getOrElse("http://localhost:9200")
    if (isRemoteIndexingAllowed || elasticSearchUri.contains("localhost") || elasticSearchUri.contains("127.0.0.1")) {
      ElasticsearchIndexerPlugin.index(mongoUri, elasticSearchUri)
      s.log.info(s"[safeIndex] Indexing $elasticSearchUri complete")
    } else {
      s.log.error(
        s"[safeIndex] - Not allowed to index to a remote elasticsearch. Add -Dallow.remote.indexing=true to override.")
    }
    ElasticsearchIndexerPlugin.index(mongoUri, elasticSearchUri)
  }
}