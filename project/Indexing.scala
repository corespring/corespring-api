import sbt._
import sbt.Keys._
import org.corespring.elasticsearch.{ BatchConfig, BatchContentIndexer }
import sbt.{ Plugin, SettingKey, TaskKey }
import scala.concurrent.ExecutionContext

object Indexing {

  import Utils._

  val index = TaskKey[Unit]("index")
  val indexTask = index <<= (streams) map safeIndex

  def safeIndex(s: TaskStreams): Unit = {
    lazy val isRemoteIndexingAllowed = System.getProperty("allow.remote.indexing", "false") == "true"
    val mongoUri = getEnv("ENV_MONGO_URI").getOrElse("mongodb://localhost:27017/api")
    val elasticSearchUri = getEnv("BONSAI_URL").getOrElse("http://localhost:9200")
    val componentPath = getEnv("CONTAINER_COMPONENTS_PATH").getOrElse("corespring-components/components")
    if (isRemoteIndexingAllowed || elasticSearchUri.contains("localhost") || elasticSearchUri.contains("127.0.0.1")) {
      val config = BatchConfig(mongoUri, elasticSearchUri, componentPath)
      BatchContentIndexer.reindex(config)(ExecutionContext.global)
      s.log.info(s"[safeIndex] Indexing $elasticSearchUri complete")
    } else {
      s.log.error(
        s"[safeIndex] - Not allowed to index to a remote elasticsearch. Add -Dallow.remote.indexing=true to override.")
    }
  }
}
