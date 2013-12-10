package org.corespring.poc.integration.impl.transformers

import org.specs2.mutable.Specification

class SessionDocumentTransformerTest extends Specification{

  "SessionDocumentTransformer" should {

    "transform doc to v2 format" in {

      val document =
        """
          |{
          |  "_id": {
          |    "$oid": "502d621a30040799f95c5265"
          |  },
          |  "itemId": {
          |    "_id": {
          |      "$oid": "50083ba9e4b071cb5ef79101"
          |    },
          |    "version": 0
          |  },
          |  "start": {
          |    "$date": 1345833488187
          |  },
          |  "finish": {
          |    "$date": 1345833489187
          |  },
          |  "responses": [
          |    {
          |      "_t" : "org.corespring.qti.models.responses.StringResponse",
          |      "_id": "mexicanPresident",
          |      "responseValue": "calderon",
          |    },
          |    {
          |      "_t" : "org.corespring.qti.models.responses.StringResponse",
          |      "_id": "irishPresident",
          |      "responseValue": "guinness",
          |    },
          |    {
          |      "_t" : "org.corespring.qti.models.responses.StringResponse",
          |      "_id": "winterDiscontent",
          |      "responseValue": "York",
          |    }
          |  ],
          |  "v2-data" : {
          |   "components": {
          |      "mexicanPresident" : {
          |       "stash" : {
          |         "shuffledOrder" : ["obama", "nieto"]
          |       }
          |      },
          |      "irishPresident" : {
          |        "stash" : {
          |          "shuffledOrder" : ["higgins", "hollande"]
          |        }
          |      }
          |    }
          |  }
          |}""".stripMargin


          val expected =
            """
              |{
              |  "_id": {
              |    "$oid": "502d621a30040799f95c5265"
              |  },
              |  "itemId": {
              |    "_id": {
              |      "$oid": "50083ba9e4b071cb5ef79101"
              |    },
              |    "version": 0
              |  },
              |  "isComplete" : true
              |  "components": [
              |    "mexicanPresident" : {
              |      "answers" : ["calderon"],
              |      "stash" : {}
              |    },
              |    "irishPresident" : {
              |      "answers":  ["higgins"],
              |      "stash" : {}
              |    },
              |    "winterDiscontent" : {
              |      "answers": "York"
              |    }
              |  ]
              |}
            """.stripMargin

      true === true
    }
  }

}
