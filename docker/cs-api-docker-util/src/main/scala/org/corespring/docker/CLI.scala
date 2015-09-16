package org.corespring.docker

import scala.Some
import org.rogach.scallop._;

object CLI extends App {

  import scala.concurrent.ExecutionContext.Implicits.global

  object Conf extends ScallopConf(args.toList) {

    val mongoUri: ScallopOption[String] = opt[String](
      "mongoUri",
      descr = "the mongo uri to use for seeding + indexing",
      required = false,
      default = Some("mongodb://localhost:27017/api"))

    val elasticSearchUri: ScallopOption[String] = opt[String](
      "elasticSearchUri",
      descr = "the elasticsearch uri to use for indexing",
      required = false,
      default = Some("http://localhost:9200"))

    val componentPath: ScallopOption[String] = opt[String](
      "componentPath",
      descr = "the path to locate corespring-components",
      required = false,
      default = Some("corespring-components/components"))
  }

  def handleError(e: Throwable) = throw e

  def getEnv(prop: String): Option[String] = {
    val env = System.getenv(prop)
    if (env == null) None else Some(env)
  }

  def seedDevData() = {

    println(s"seed db - you can only seed the local api db.")

    if (Conf.mongoUri().contains("localhost")) {
      val content = List("static", "demo", "debug", "sample", "common", "dev", "exemplar-content").map(s => s"conf/seed-data/$s")

      println(s"seed: ${content}")
      import com.ee.seeder.log.ConsoleLogger.Level
      com.ee.seeder.MongoDbSeeder.logLevel = Level.DEBUG
      com.ee.seeder.MongoDbSeeder.seed(Conf.mongoUri(), content, true)
    } else {
      throw new RuntimeException(s"[seedDevData] bad mongo uri - can only seed localhost: uri: ${Conf.mongoUri()}")
    }
  }

  def indexElasticSearch() = {
    import org.corespring.elasticsearch.ContentIndexer
    println(s"running content indexer for: ${Conf.elasticSearchUri()}")
    ContentIndexer.reindex(new java.net.URL(Conf.elasticSearchUri()), Conf.mongoUri(), Conf.componentPath())
    println("done.")
  }

  try {
    val argsList = args.toList
    println(s"CsApi Docker Util - Args ${argsList.mkString(" ")}")
    seedDevData()
    indexElasticSearch()
    System.exit(0)
  } catch {
    case io: java.io.IOException => handleError(io)
    case e: Throwable => handleError(e)
  }
}