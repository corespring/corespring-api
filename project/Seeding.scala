import sbt._
import sbt.Keys._
import MongoDbSeederPlugin._

object Seeding {

  import Utils._
  import MongoDbSeederPlugin._

  lazy val settings: Seq[Setting[_]] = newSettings ++ Seq(
    seederLogLevel := "INFO",
    testUri := "mongodb://localhost/api",
    seedCustomTask,
    seedDevTask,
    seedProdTask,
    seedDebugDataTask,
    seedDemoDataTask,
    seedDevDataTask,
    seedSampleDataTask,
    seedStaticDataTask) ++ seederSettings

  def safeSeed(clear: Boolean)(paths: String, name: String, logLevel: String, s: TaskStreams): Unit = {
    lazy val isRemoteSeedingAllowed = System.getProperty("allow.remote.seeding", "false") == "true"
    lazy val overrideClear = System.getProperty("clear.before.seeding", "false") == "true"
    s.log.info(s"[safeSeed] $paths - Allow remote seeding? $isRemoteSeedingAllowed - Clear collection before seed? $clear")
    val uriString = getEnv("ENV_MONGO_URI").getOrElse("mongodb://localhost/api")
    s.log.info(s"[safeSeed] uriString: $uriString")
    val uri = new URI(uriString)
    s.log.info(s"[safeSeed] uri: $uri")
    val host = uri.getHost
    s.log.info(s"[safeSeed] host: $host")
    if (host == "127.0.0.1" || host == "localhost" || isRemoteSeedingAllowed) {
      MongoDbSeederPlugin.seed(uriString, paths, name, logLevel, clear || overrideClear)
      s.log.info(s"[safeSeed] $paths - seeding complete")
    } else {
      s.log.error(s"[safeSeed] $paths - Not allowed to seed a remote db. Add -Dallow.remote.seeding=true to override.")
    }
  }

  val devData = SettingKey[String]("dev-data")
  val demoData = SettingKey[String]("demo-data")
  val debugData = SettingKey[String]("debug-data")
  val sampleData = SettingKey[String]("sample-data")
  val staticData = SettingKey[String]("static-data")

  lazy val seederSettings = Seq(
    devData := Seq(
      "conf/seed-data/common",
      "conf/seed-data/dev",
      "conf/seed-data/exemplar-content").mkString(","),
    demoData := "conf/seed-data/demo",
    debugData := "conf/seed-data/debug",
    sampleData := "conf/seed-data/sample",
    staticData := "conf/seed-data/static")

  val seedDevData = TaskKey[Unit]("seed-dev-data")
  val seedDevDataTask = seedDevData <<= (devData, name, MongoDbSeederPlugin.seederLogLevel, streams) map safeSeed(false)

  val seedDemoData = TaskKey[Unit]("seed-demo-data")
  val seedDemoDataTask = seedDemoData <<= (demoData, name, MongoDbSeederPlugin.seederLogLevel, streams) map safeSeed(false)

  val seedDebugData = TaskKey[Unit]("seed-debug-data")
  val seedDebugDataTask = seedDebugData <<= (debugData, name, MongoDbSeederPlugin.seederLogLevel, streams) map safeSeed(false)

  val seedSampleData = TaskKey[Unit]("seed-sample-data")
  val seedSampleDataTask = seedSampleData <<= (sampleData, name, MongoDbSeederPlugin.seederLogLevel, streams) map safeSeed(false)

  val seedStaticData = TaskKey[Unit]("seed-static-data")
  val seedStaticDataTask = seedStaticData <<= (staticData, name, MongoDbSeederPlugin.seederLogLevel, streams) map safeSeed(true)

  val seedCustom = inputKey[Unit]("seed-custom")

  val seedCustomTask = seedCustom := {

    val st = streams.value
    val args = sbt.complete.Parsers.spaceDelimited("<arg>").parsed

    args match {
      case Seq(path) => {

        if (file(path).isDirectory) {
          safeSeed(true)(path, "custom", MongoDbSeederPlugin.seederLogLevel.value, streams.value)

          st.log.info("auto-indexing")
          Indexing.index.value

        } else {
          sys.error(s"path: $path doesnt exist")
        }
      }
      case _ => sys.error("You must specify a path to seed")
    }
  }

  val seedDev = TaskKey[Unit]("seed-dev")
  val seedDevTask = seedDev := {
    (seedStaticData.value,
      seedDevData.value,
      seedDemoData.value,
      seedDebugData.value,
      seedSampleData.value)
  }

  val seedProd = TaskKey[Unit]("seed-prod")
  val seedProdTask = seedProd := {
    (seedStaticData.value,
      seedSampleData.value)
  }

}