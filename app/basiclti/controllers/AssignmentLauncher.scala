package basiclti.controllers

import play.api.mvc.{AnyContent, Request, Action, Controller}
import play.Logger
import basiclti.models.{LtiData, Assignment, LtiOAuthConsumer}
import play.api.libs.ws.WS
import play.api.libs.oauth.{RequestToken, ConsumerKey, OAuthCalculator}
import org.bson.types.ObjectId
import com.mongodb.casbah.commons.MongoDBObject
import models.ItemSession
import play.api.libs.json.Json._

import testplayer.controllers.routes.{ ItemPlayer => ItemPlayerRoutes }
import basiclti.controllers.routes.{ AssignmentPlayer => AssignmentPlayerRoutes }

object AssignmentLauncher extends Controller {

  private def tokenize(url:String,token:String) = "%s?access_token=%s".format(url,token)

  def launch(itemId:ObjectId) = Action{ request =>

  /**
   * Find assignment in the db - if its not there create it.
   */
    def getOrCreateAssignment(data:LtiData) : Assignment = {
      Assignment.findOne( MongoDBObject("resultSourcedId" -> data.resultSourcedId )) match {
        case Some(a) => a
        case _ => {
          val newSession = new ItemSession( itemId = itemId )
          ItemSession.save(newSession)
          val newAssignment = new Assignment(
            itemSessionId = newSession.id,
            resultSourcedId = data.resultSourcedId.get,
            gradePassbackUrl = data.outcomeUrl.get,
            onFinishedUrl = data.returnUrl.get
          )
          Assignment.save(newAssignment)
          newAssignment
        }
      }
    }

    //TODO: Token access?

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
                  Logger.debug("response from lms:")
                  Logger.debug(response.body)
                  Ok(toJson(Map("returnUrl" -> assignment.onFinishedUrl)))
                }
              )
          }
          case _ => BadRequest("no item session found")
        }
       }
       case _ =>  BadRequest("no assignment found")
     }
  }

  //TODO: calculate score
  private def getScore(session:ItemSession) : String = {
    "0.3"
  }
}
