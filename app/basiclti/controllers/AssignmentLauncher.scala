package basiclti.controllers

import play.api.mvc.{AnyContent, Request, Action, Controller}
import play.Logger
import basiclti.models.{LtiData, Assignment, LtiOAuthConsumer}
import play.api.libs.ws.WS
import play.api.libs.oauth.{RequestToken, ConsumerKey, OAuthCalculator}
import org.bson.types.ObjectId
import com.mongodb.casbah.commons.MongoDBObject
import models.{ItemSessionSettings, ItemSession}
import play.api.libs.json.Json._

import testplayer.controllers.routes.{ ItemPlayer => ItemPlayerRoutes }
import basiclti.controllers.routes.{ AssignmentPlayer => AssignmentPlayerRoutes }
import basiclti.controllers.routes.{ AssignmentLauncher => AssignmentLauncherRoutes }
import oauth.signpost.signature.AuthorizationHeaderSigningStrategy

object AssignmentLauncher extends Controller {

  private def tokenize(url:String,token:String) = "%s?access_token=%s".format(url,token)

  val defaultSessionSettings = ItemSessionSettings(
    maxNoOfAttempts = 1,
    showFeedback = true,
    highlightCorrectResponse = true,
    highlightUserResponse = true,
    allowEmptyResponses = true
  )

  private def isSignedCorrectly(request:Request[AnyContent]) : Boolean = {

    val requestSignature = request.body.asFormUrlEncoded.get("oauth_signature").head

    val consumer = new LtiOAuthConsumer("1234", "secret")
    consumer.sign(request)
    consumer.setSigningStrategy(new AuthorizationHeaderSigningStrategy())

    consumer.getOAuthSignature() match {
      case Some(signature) => signature.equals( requestSignature )
      case _ => false
    }
  }

  def launch(itemId:ObjectId) = Action{ request =>

  /**
   * Find assignment in the db - if its not there create it.
   */
    def getOrCreateAssignment(data:LtiData) : Assignment = {

      def makeAssignment(data:LtiData, sessionId : ObjectId, id : ObjectId = new ObjectId()) = {
        val newAssignment = new Assignment(
          itemSessionId = sessionId,
          resultSourcedId = data.resultSourcedId.get,
          gradePassbackUrl = data.outcomeUrl.get,
          onFinishedUrl = data.returnUrl.get,
          id = id
        )
        Assignment.save(newAssignment)
        newAssignment
      }

      Assignment.findOne( MongoDBObject("resultSourcedId" -> data.resultSourcedId )) match {
        case Some(a) => makeAssignment(data,a.itemSessionId,a.id)
        case _ => {
          val newSession = new ItemSession( itemId = itemId, settings = defaultSessionSettings )
          ItemSession.newSession( itemId, newSession)
          makeAssignment(data, newSession.id)
        }
      }
    }

    if ( isSignedCorrectly(request) ){
      LtiData(request) match {
        case Some(d) => {
          require(d.outcomeUrl.isDefined, "no outcome url is defined")
          require(d.resultSourcedId.isDefined, "sourcedid is defined")
          val assignment = getOrCreateAssignment(d)
          val call = AssignmentPlayerRoutes.runByAssignmentId(assignment.id)
          Redirect( tokenize(call.url, common.mock.MockToken ) )
        }
        case _ => {
          Logger.info("its a teacher")
          val testPlayerCall = ItemPlayerRoutes.previewItem(itemId.toString)
          Redirect( tokenize(testPlayerCall.url, common.mock.MockToken))
        }
      }
    } else {
      BadRequest("Invalid oauth signature")
    }
  }


  def responseXml(sourcedId:String,score:String) = <imsx_POXEnvelopeRequest>
    <imsx_POXHeader>
      <imsx_POXRequestHeaderInfo>
        <imsx_version>V1.0</imsx_version>
        <imsx_messageIdentifier>12341234</imsx_messageIdentifier>
      </imsx_POXRequestHeaderInfo>
    </imsx_POXHeader>
    <imsx_POXBody>
      <replaceResultRequest>
        <resultRecord>
          <sourcedGUID>
            <sourcedId>{sourcedId}</sourcedId>
          </sourcedGUID>
          <result>
            <resultScore>
              <language>en</language>
              <textString>{score}</textString>
            </resultScore>
          </result>
        </resultRecord>
      </replaceResultRequest>
    </imsx_POXBody>
  </imsx_POXEnvelopeRequest>

  /**
   * TODO: token secret
   * @param assignmentId
   * @return
   */
  def process(assignmentId:ObjectId) = Action{ request =>

     Assignment.findOneById(assignmentId) match {
       case Some(assignment) => {

        ItemSession.findOneById(assignment.itemSessionId) match {
          case Some(session) => {

              val consumer = new LtiOAuthConsumer("1234", "secret")
              val score = getScore(session)

              def sendResultsToPassback = {
                WS.url(assignment.gradePassbackUrl)
                  .sign(
                  OAuthCalculator(ConsumerKey("1234","secret"),
                    RequestToken(consumer.getToken, consumer.getTokenSecret)))
                  .withHeaders(("Content-Type", "application/xml"))
                  .post(responseXml(assignment.resultSourcedId, score))
              }

              sendResultsToPassback.await(10000).fold(
                error => throw new RuntimeException( error.getMessage ),
                response => {
                 Logger.debug(response.body)
                 val returnUrl = response.body match {
                   case e : String if e.contains("Invalid authorization header") => {
                     AssignmentLauncherRoutes.authorizationError().url
                   }
                   case _ => assignment.onFinishedUrl
                }
                  Ok(toJson(Map("returnUrl" -> returnUrl)))
                }
              )
          }
          case _ => BadRequest("no item session found")
        }
       }
       case _ =>  BadRequest("no assignment found")
     }
  }

  def authorizationError = Action{ request =>
    Ok(basiclti.views.html.authorizationError())
  }

  private def getScore(session:ItemSession) : String = {
    val (score,maxScore) = ItemSession.getTotalScore(session)
    (score / maxScore).toString
  }
}
