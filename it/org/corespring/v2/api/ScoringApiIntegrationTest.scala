package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.it.scopes._
import org.corespring.models.item.PlayerDefinition
import org.specs2.specification.BeforeAfter
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.mvc.{ AnyContent, AnyContentAsJson, Call }

class ScoringApiIntegrationTest extends IntegrationSpecification with WithV2SessionHelper {
  val Routes = org.corespring.v2.api.routes.ScoringApi

  lazy val itemService = main.itemService

//  "ScoringApi" should {

    def playerDef(customScoring: Option[String] = None) = PlayerDefinition(
      Seq.empty,
      "html",
      obj(
        "1" -> obj(
          "componentType" -> "corespring-multiple-choice",
          "correctResponse" -> obj("value" -> Json.arr("carrot")),
          "model" -> obj(
            "config" -> obj(
              "singleChoice" -> true),
            "prompt" -> "Carrot?",
            "choices" -> Json.arr(
              obj("label" -> "carrot", "value" -> "carrot"),
              obj("label" -> "banana", "value" -> "banana"))))),
      "",
      customScoring,
      obj("prop" -> "value"))

    /*"when calling load score" should {

      s"1: return $OK and 100% - for multiple choice" in new token_loadScore(AnyContentAsJson(obj())) {

        val item = itemService.findOneById(itemId).get
        //Note: We have to remove the qti or else the ItemTransformer will overrwrite the v2 data
        val update = item.copy(data = None, playerDefinition = Some(playerDef()))
        val resultString = s"""{"summary":{"maxPoints":1,"points":1.0,"percentage":100.0},"components":{"1":{"weight":1,"score":1.0,"weightedScore":1.0}}}"""
        val resultJson = Json.parse(resultString)
        itemService.save(update)
        println(s"playerDefinition: ${update.playerDefinition}")
        v2SessionHelper.update(sessionId, obj("itemId" -> itemId.toString, "components" -> obj(
          "1" -> obj("answers" -> Json.arr("carrot")))))
        status(result) === OK
        contentAsJson(result) === resultJson
      }

      s"2: return $OK and 0% - for multiple choice" in new token_loadScore(AnyContentAsJson(obj())) {
        val item = itemService.findOneById(itemId).get
        val update = item.copy(data = None, playerDefinition = Some(playerDef()))
        val resultJson = obj(
          "summary" -> obj(
            "maxPoints" -> 0,
            "points" -> 0.0,
            "percentage" -> 0.0),
          "components" -> obj(
            "1" -> obj(
              "weight" -> 0,
              "score" -> 0.0,
              "weightedScore" -> 0.0
              )
          )
        )

        itemService.save(update)
        v2SessionHelper.update(sessionId, obj("itemId" -> itemId.toString, "components" -> obj(
          "1" -> obj("answers" -> Json.arr("banana")))))
        status(result) === OK
        contentAsJson(result) === resultJson
      }
    }

    "when calling load score for extendedTextEntry" should {

      val components = obj(
        "1" -> obj(
          "componentType" -> "corespring-extended-text-entry"))
      val extendedTextEntryDef = PlayerDefinition("html", components)

      "return OK and null score because it's not scorable" in new token_loadScore(AnyContentAsJson(obj())) {
        val item = itemService.findOneById(itemId).get
        //Note: We have to remove the qti or else the ItemTransformer will overrwrite the v2 data
        val update = item.copy(data = None, playerDefinition = Some(extendedTextEntryDef))
        val resultString = s"""{"summary":{"maxPoints":0,"points":0,"percentage":0.0},"components":{"1":{"weight":0,"score":null}}}"""
        val resultJson = Json.parse(resultString)
        itemService.save(update)
        println(s"playerDefinition: ${update.playerDefinition}")
        v2SessionHelper.update(sessionId, obj("itemId" -> itemId.toString, "components" -> obj(
          "1" -> obj("answers" -> Json.arr("carrot")))))
        status(result) === OK
        contentAsJson(result) === resultJson
      }
    }

    "when calling load score with a custom scoring item" should {

      val customScoring =
        """
      exports.process = function(item, session, outcomes){
        if(session.components[1].answers.indexOf('carrot') !== -1){
          return { summary: { numcorrect: 1, score: 1.0}};
        } else {
          return { summary: { numcorrect: 0, score: 0}};
        }
      }
        """

      s"return $OK and 100% - for multiple choice" in new token_loadScore(AnyContentAsJson(obj())) {

        val item = itemService.findOneById(itemId).get
        val update = item.copy(data = None, playerDefinition = Some(playerDef(Some(customScoring))))
        val resultString =
          s"""{ "components":{"1":{"weight":1,"score":1.0,"weightedScore":1.0}}, "summary":{"numcorrect" : 1, "score" : 1.0}}"""
        val resultJson = Json.parse(resultString)
        itemService.save(update)
        v2SessionHelper.update(sessionId, obj("itemId" -> itemId.toString, "components" -> obj(
          "1" -> obj("answers" -> Json.arr("carrot")))))
        status(result) === OK
        contentAsJson(result) === resultJson
      }

    }

    "when calling load score with a custom scoring that uses outcomes" should {

      val customScoring =
        """
      exports.process = function(item, session, outcomes){
        if(outcomes["1"].correctness === "correct"){
          return { summary: { numcorrect: 1, score: 1.0}};
        } else {
          return { summary: { numcorrect: 0, score: 0}};
        }
      }
        """
      s"return $OK and 100% - for multiple choice" in new token_loadScore(AnyContentAsJson(obj())) {

        val item = itemService.findOneById(itemId).get
        val update = item.copy(data = None, playerDefinition = Some(playerDef(Some(customScoring))))
        val resultString =
          s"""{ "components":{"1":{"weight":1,"score":1.0,"weightedScore":1.0}}, "summary":{"numcorrect" : 1, "score" : 1.0}}"""
        val resultJson = Json.parse(resultString)
        itemService.save(update)
        v2SessionHelper.update(sessionId, obj("itemId" -> itemId.toString, "components" -> obj(
          "1" -> obj("answers" -> Json.arr("carrot")))))
        status(result) === OK
        contentAsJson(result) === resultJson
      }

      s"return $OK and 0% - for multiple choice" in new token_loadScore(AnyContentAsJson(obj())) {

        val item = itemService.findOneById(itemId).get
        val update = item.copy(data = None, playerDefinition = Some(playerDef(Some(customScoring))))
        val resultString =
          s"""{ "components":{"1":{"weight":1,"score":0.0,"weightedScore":0.0}}, "summary":{"numcorrect" : 0, "score" : 0}}"""
        val resultJson = Json.parse(resultString)
        itemService.save(update)
        v2SessionHelper.update(sessionId, obj("itemId" -> itemId.toString, "components" -> obj(
          "1" -> obj("answers" -> Json.arr("banana")))))

        status(result) === OK
        contentAsJson(result) === resultJson
      }

    }

    "when calling load multiple scores" should {

      s"1: return $OK and 100% - for multiple choice" in new token_loadMultipleScores() {

        val item = itemService.findOneById(itemId).get
        //Note: We have to remove the qti or else the ItemTransformer will overrwrite the v2 data
        val update = item.copy(data = None, playerDefinition = Some(playerDef()))
        val resultString = s"""[{"sessionId":"${sessionIds(0)}","result":{"summary":{"maxPoints":1,"points":1.0,"percentage":100.0},"components":{"1":{"weight":1,"score":1.0,"weightedScore":1.0}}}},{"sessionId":"${sessionIds(1)}","result":{"summary":{"maxPoints":1,"points":1.0,"percentage":100.0},"components":{"1":{"weight":1,"score":1.0,"weightedScore":1.0}}}}]"""
        val resultJson = Json.parse(resultString)
        itemService.save(update)

        v2SessionHelper.update(sessionIds(0), obj("itemId" -> itemId.toString, "components" -> obj(
          "1" -> obj("answers" -> Json.arr("carrot")))))
        v2SessionHelper.update(sessionIds(1), obj("itemId" -> itemId.toString, "components" -> obj(
          "1" -> obj("answers" -> Json.arr("carrot")))))

        status(result) === OK
        contentAsJson(result) === resultJson
      }

      s"2: return $OK and 0% - for multiple choice" in new token_loadMultipleScores() {
        val item = itemService.findOneById(itemId).get
        val update = item.copy(data = None, playerDefinition = Some(playerDef()))
        val resultString = s"""[{"sessionId":"${sessionIds(0)}","result":{"summary":{"maxPoints":1,"points":0.0,"percentage":0.0},"components":{"1":{"weight":1,"score":0.0,"weightedScore":0.0}}}},{"sessionId":"${sessionIds(1)}","result":{"summary":{"maxPoints":1,"points":0.0,"percentage":0.0},"components":{"1":{"weight":1,"score":0.0,"weightedScore":0.0}}}}]"""
        val resultJson = Json.parse(resultString)
        itemService.save(update)

        v2SessionHelper.update(sessionIds(0), obj("itemId" -> itemId.toString, "components" -> obj(
          "1" -> obj("answers" -> Json.arr("banana")))))
        v2SessionHelper.update(sessionIds(1), obj("itemId" -> itemId.toString, "components" -> obj(
          "1" -> obj("answers" -> Json.arr("banana")))))

        status(result) === OK
        contentAsJson(result) === resultJson
      }
    }
  */
//  }

  class token_loadScore(json: AnyContent) extends BeforeAfter with sessionLoader with TokenRequestBuilder with orgWithAccessTokenItemAndSession {
    override def getCall(sessionId: ObjectId): Call = Routes.loadScore(sessionId.toString)

    override def requestBody = json
  }

  class token_loadMultipleScores() extends BeforeAfter with multiSessionLoader with TokenRequestBuilder with orgWithAccessTokenItemAndMultipleSessions {
    override def getCall(): Call = Routes.loadMultipleScores()

    override def getJsonBody(sessionIds: Seq[ObjectId]) =
      obj("sessionIds" -> Json.toJson(sessionIds.map(s => s.toString)))
  }

}
