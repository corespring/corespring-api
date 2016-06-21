import sbt._
import sbt.Keys._
import org.corespring.elasticsearch.{ BatchCli, BatchConfig, BatchContentIndexer }

import scala.concurrent.ExecutionContext
import sbt.complete.Parsers._
object Indexing {

  val index = inputKey[Unit]("Index Elastic search")

  val indexTask = index := {
    val s = streams.value
    val args: Seq[String] = spaceDelimited("<arg>").parsed

    val defaultConfig = BatchConfig(
      mongoUri = "mongodb://localhost:27017/api",
      elasticSearchUri = "http://localhost:9200",
      componentPath = "corespring-components/components")

    BatchCli.parse(args, defaultConfig) match {
      case Some(config) => {
        s.log.info(s"New indexing... args: $args")
        val isRemoteIndexingAllowed = System.getProperty("allow.remote.indexing", "false") == "true"
        val isRemoteUrl = {
          val host = config.elasticSearchURL.map(_.getHost).get
          !Seq("localhost", "127.0.0.1").contains(host)
        }

        if (!isRemoteIndexingAllowed && isRemoteUrl) {
          s.log.error(s"[safeIndex] - Not allowed to index to a remote elasticsearch. Add -Dallow.remote.indexing=true to override.")
        } else {

          s.log.info(s"config: $config")
          BatchContentIndexer.reindex(config)(ExecutionContext.global)
        }
      }
      case _ => {
        s.log.error(s"[index] can't parse config")
      }
    }
  }
}
