import sbt._
import sbt.Keys._
import org.corespring.elasticsearch.ContentIndexer
import sbt.{ Plugin, SettingKey, TaskKey }

object ElasticSearchIndexerPlugin extends Plugin {
  val indexKey = TaskKey[Unit]("elasticsearch-index")

  val mongoUri = SettingKey[String]("mongoUri")
  val elasticSearchUri = SettingKey[String]("elasticSearchUri")
  val componentPath = SettingKey[String]("container.components.path")

  private val defaultMongoUri = "mongodb://localhost:27017"
  private val defaultElasticsearchUri = "http://localhost:9200"
  private val defaultComponentPath = "corespring-components/components"

  def index(mongoUri: String, elasticsearchUri: String, componentPath: String): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    ContentIndexer.reindex(new java.net.URL(elasticsearchUri), mongoUri, componentPath)
  }

  val newSettings = Seq(
    mongoUri := defaultMongoUri,
    elasticSearchUri := defaultElasticsearchUri,
    componentPath := defaultComponentPath,
    indexKey <<= (mongoUri, elasticSearchUri, componentPath) map (index))
}

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
      ElasticSearchIndexerPlugin.index(mongoUri, elasticSearchUri, componentPath)
      s.log.info(s"[safeIndex] Indexing $elasticSearchUri complete")
    } else {
      s.log.error(
        s"[safeIndex] - Not allowed to index to a remote elasticsearch. Add -Dallow.remote.indexing=true to override.")
    }
  }
}
