import _root_.controllers.ConcreteS3Service
import _root_.models._
import _root_.models.StringItemResponse
import _root_.models.TempSessions.sessionList
import _root_.models.ArrayItemResponse
import org.joda.time.DateTime
import play.api.mvc.Results._
import scala.Some
import util.Random
import web.controllers.utils.ConfigLoader
import common.seed.SeedDb._
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import org.bson.types.ObjectId
import play.api._
import mvc._
import mvc.SimpleResult
import play.api.Play.current
import play.api.Application


/**
  */
object Global extends GlobalSettings {

  val INIT_DATA: String = "INIT_DATA"

  val AccessControlAllowEverything = ("Access-Control-Allow-Origin", "*")

  def AccessControlAction[A](action: Action[A]): Action[A] = Action(action.parser) {
    request =>
      action(request) match {
        case s: SimpleResult[_] =>
          s
            .withHeaders(AccessControlAllowEverything)
            .withHeaders(("Access-Control-Allow-Methods", "PUT, GET, POST, DELETE, OPTIONS"))
            .withHeaders(("Access-Control-Allow-Headers", "x-requested-with,Content-Type,Authorization"))

        case result => result
      }
  }

  def AjaxFilterAction[A](action: Action[A]): Action[A] = Action(action.parser) {
    request =>
      if (request.headers.get("X-Requested-With") == Some("XMLHttpRequest")) {
        action(request) match {
          case s: SimpleResult[_] => s.withHeaders(("Cache-Control", "no-cache"))
          case result => result
        }
      }
      else {
        action(request)
      }
  }

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {

    request.method match {
      //return the default access control headers for all OPTION requests.
      case "OPTIONS" => Some(AccessControlAction(Action(new play.api.mvc.Results.Status(200))))
      case _ => {
        super.onRouteRequest(request).map {
          case action: Action[_] => AjaxFilterAction(AccessControlAction(action))
          case other => other
        }
      }
    }
  }

  // 500 - internal server error
  override def onError(request: RequestHeader, throwable: Throwable) = {

    val uid = new ObjectId().toString
    Logger.error(uid)
    Logger.error(throwable.getMessage)

    if (Logger.isDebugEnabled) {
      throwable.printStackTrace()
    }
    InternalServerError(common.views.html.onError(uid, throwable))
  }


  override def onHandlerNotFound(request: play.api.mvc.RequestHeader): Result = {
    val result = super.onHandlerNotFound(request)

    result match {
      case s: SimpleResult[_] => s.withHeaders(AccessControlAllowEverything)
      case _ => result
    }
  }

  override def onBadRequest(request: play.api.mvc.RequestHeader, error: scala.Predef.String): play.api.mvc.Result = {
    val result = super.onBadRequest(request, error)
    result match {
      case s: SimpleResult[_] => s.withHeaders(AccessControlAllowEverything)
      case _ => result
    }
  }

  override def onStart(app: Application) {

    // support JodaTime
    RegisterJodaTimeConversionHelpers()

    val amazonProperties = Play.getFile("/conf/AwsCredentials.properties")
    ConcreteS3Service.init(amazonProperties)

    val initData = ConfigLoader.get(INIT_DATA).getOrElse("true") == "true"

    def onlyIfLocalDb(fn: (() => Unit)) {
      if (isLocalDb)
        fn()
      else
        throw new RuntimeException("You're trying to seed against a remote db - bad idea")
    }

    if (Play.isTest(app)) {
      onlyIfLocalDb(seedTestData)
    } else {
      if (Play.isDev(app)) {
        if (initData) onlyIfLocalDb(seedDevData)
      } else if(Play.isProd(app)) {
        if (initData) seedDevData()
      }
      addDemoDataToDb()
    }


  }

  /** Add demo data models to the the db to allow end users to be able to
    * view the content as a demo.
    * This involves:
    * 1. adding a demo access token that is associated with a demo organization
    * 2. adding a demo organiztion
    *
    * TODO: the demo orgs listed are hardcoded
    */
  private def addDemoDataToDb() {
    seedData("conf/seed-data/demo")
  }


  private def isLocalDb: Boolean = {
    ConfigLoader.get("mongodb.default.uri") match {
      case Some(url) => (url.contains("localhost") || url.contains("127.0.0.1") || url == "mongodb://bleezmo:Basic333@ds035907.mongolab.com:35907/sib")
      case None => false
    }
  }


  private def seedTestData() {
    emptyData()
    seedData("conf/seed-data/test")
  }


  // TODO: remove this, this is only for testing purposes for the instructor view
  private def addTestSessions() {

    def chooseOneRandomly(items:Array[String]):String = {
      val idx = new Random().nextInt(items.length)
      items(idx)
    }

    def chooseNRandomly(items:Array[String]):Seq[String] = {
      val chosenNumber = new Random().nextInt(items.length)
      Random.shuffle(items.toSeq).take(chosenNumber).sortWith(_<_)
    }

    def upToNWords(base:String, n:Int):Seq[String] = {
      for (i <- 0 to new Random().nextInt(n)) yield base + i.toString
    }

    val presidents = Array("obama", "cameron", "calderon")
    val colors = Array("blue","violet","white","red")
    val winterList = (Seq("york","York") ++ upToNWords("someWord", 10))

    val rand = new Random()
    for (i <- 1 to 10) {
      println("Iteration: " + i)
      val random_index = rand.nextInt(winterList.length)
      val winter = winterList(random_index)

      val response1 = ArrayItemResponse("mexicanPresident", Seq(chooseOneRandomly(presidents)))
      val response2 = ArrayItemResponse("rainbowColors", chooseNRandomly(colors), None)
      val response3 = StringItemResponse("winterDiscontent", winter)
      println("Winter: "+winter)
      val session = ItemSession(responses = Seq(response1, response2, response3), itemId = new ObjectId("507c9fb3a0eee12a21a88912"), finish = Some(new DateTime()))
      sessionList ::= session.id.toString
      ItemSession.save(session)
    }
  }


  private def seedDevData() {
    emptyData()
    seedData("conf/seed-data/common")
    seedData("conf/seed-data/dev")
    seedData("conf/seed-data/exemplar-content")

    // TODO: remove this, this is only for testing purposes for the instructor view
    addTestSessions()
  }

}


