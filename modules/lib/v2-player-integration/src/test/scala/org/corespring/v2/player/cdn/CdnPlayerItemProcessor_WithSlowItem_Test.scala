package org.corespring.v2.player.cdn

import org.corespring.models.item.PlayerDefinition
import org.corespring.models.item.resource.StoredFile
import org.corespring.models.json.JsonFormatting
import org.corespring.models.json.item.PlayerDefinitionFormat
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{JsArray, JsString, Json}

class CdnPlayerItemProcessor_WithSlowItem_Test extends Specification with Mockito {

  "makePlayerDefinitionJson" should {

    trait scope extends Scope {

      val mockJsonFormatting = {
        val m = mock[JsonFormatting]
        m.formatPlayerDefinition returns PlayerDefinitionFormat
        m
      }

      val mockItemAssetResolver = {
        val m = mock[ItemAssetResolver]
        m.resolve(anyString)(anyString).answers { (args, value) =>
          "CDN/" + args.asInstanceOf[Array[Object]](1).asInstanceOf[String]
        }
        m
      }

      val sut = new CdnPlayerItemProcessor(mockItemAssetResolver, mockJsonFormatting)

      def session = Json.obj("id" -> "sessionId", "itemId" -> "itemId")

      val playerDefinitionJson =
      """
        |{
        |    "customScoring" : "exports.process = function(item, session, outcomes) {\n  var answers = session.components;\n\n  function isCorrect(key) {\n    return (outcomes && outcomes[key]) ? (outcomes[key].correctness === 'correct' || outcomes[key].correctness === 'all_correct') : false;\n  }\n\n  function contains(a1, a2) {\n    for (var i in a2) {\n      if (a1.indexOf(a2[i]) < 0) {\n        return false;\n      }\n    }\n    return true;\n  }\n\n  function mapResponse(key) {\n    return (outcomes && outcomes[key]) ? (outcomes[key].correctNum) : undefined;\n  }\n\n  var RESPONSE11 = answers['RESPONSE11'].answers;\n  var RESPONSE12 = answers['RESPONSE12'].answers;\n  var RESPONSE13 = answers['RESPONSE13'].answers;\n  var RESPONSE2 = answers['RESPONSE2'].answers;\n  var RESPONSE31 = answers['RESPONSE31'].answers;\n\n  var NUMCORRECT2 = 0;\n  var NUMCORRECT = 0.0;\n  var SCORE = 0.0;\n\n  if ((RESPONSE11 === \"330\") && (RESPONSE12 === \"880\") && (RESPONSE13 === \"20\")) { NUMCORRECT = NUMCORRECT + 1; }\n  if (RESPONSE2 == undefined) { NUMCORRECT2 = 0; } else { NUMCORRECT2 = mapResponse('RESPONSE2'); }\n  if ((RESPONSE2.length === 4) && (NUMCORRECT2 === 4)) { NUMCORRECT = NUMCORRECT + 1; }\n  if (RESPONSE31 === \"D\") { NUMCORRECT = NUMCORRECT + 1; }\n  if (NUMCORRECT === 3) { SCORE = 1; } else { SCORE = 0; }\n\n  return {\n    summary: {\n      numcorrect2: NUMCORRECT2,\n      numcorrect: NUMCORRECT,\n      score: SCORE\n    }\n  };\n};",
        |    "xhtml" : "<style type=\"text/css\">.kds table,.kds table th{color:initial}.kds table td a,.kds table td a:hover{text-decoration:initial}.kds table tfoot td,.kds table th{background:initial}.kds table{border-collapse:initial;line-height:initial;margin:initial}.kds table td,.kds table th{padding:initial;vertical-align:initial;min-width:initial}.kds table td a{color:inherit}</style><div class=\"item-body kds qti\"><corespring-calculator id=\"CALCULATOR\"></corespring-calculator><br></br><br></br><style type=\"text/css\">@media print { .qti.kds .noprint { display:none; } } .qti.kds object { height:400; width:100%; } .qti.kds .Verdana2t { vertical-align:top; text-align:left; text-decoration:none; border:1px solid white; } .qti.kds .newradical { border-collapse:collapse; display:inline-block; *display:inline; vertical-align:middle; } .qti.kds .newradical td { border:0px; display:inline; padding:0px; vertical-align:top; } .qti.kds .newradical td.vinculum { border-top:1px solid black; padding:0px; vertical-align:top; } .qti.kds .newradical div.exp { font-size:.7em; position:relative; left:7px; bottom:5px; } .qti.kds .under { text-decoration:underline; } .qti.kds .center { text-align:center; margin:0; } .qti.kds b.frac { font-weight:normal; font-size:300%; font-family:arial narrow; position:relative; top:+8px; } .qti.kds sup.frac { position:relative; top:-1.4em; font-size:70%; margin-left:-0.4em; } .qti.kds .indent { text-indent:3em; } .qti.kds .abs { border-left:#000000 1px solid; border-right:#000000 1px solid; padding:0px 0.3em 0px 0.3em; } .qti.kds .whole { display:inline-block; *display:inline; margin:0em 0em 0em 0.2em; font-size:120%; } .qti.kds table.frac { display:inline-block; *display:inline; vertical-align:middle; font-size:85%; border:0px; margin:0px; padding:0px; } .qti.kds .frac .nu { border-left:0px; border-top:0px; border-right:0px; border-bottom:#000000 1px solid; text-align:center; } .qti.kds .frac .de { border:0px; text-align:center; } .qti.kds .fillin { margin:0em 0.2em; padding-left:1em; border:#000000 1px solid; display:inline-block; *display:inline; } .qti.kds table.KdsTable01 { border:1px solid black; border-spacing:0px; border-collapse:collapse; } .qti.kds table.KdsTable01>tbody>tr>th { border:1px solid black; padding:2px; background-color:#D3D3D3; text-align:center; font-weight:normal; } .qti.kds table.KdsTable01>tbody>tr>th.bold { border:1px solid black; padding:2px; background-color:#D3D3D3; text-align:center; font-weight:bold; } .qti.kds table.KdsTable01>tbody>tr>td { border:1px solid black; padding:2px; text-align:center; } .qti.kds table.KdsTable01>tbody>tr>td.bold { border:1px solid black; padding:2px; text-align:center; font-weight:bold; }</style><div></div><corespring-teacher-instructions id=\"teacher-instructions-1501207744\"></corespring-teacher-instructions><div><strong>Part A:<br></br><br></br>A reporter is writing a report about fuel efficiency.&#160; Within his report he will be reviewing 4 different cars.&#160; The table below shows the number of miles driven by Car <em>A</em> within his report.&#160; If Car <em>A</em> uses gas at a constant rate, fill in the blanks to complete the table. </strong><br></br><br></br><table class=\"KdsTable01\"><tr>  <th width=\"100px\"><strong>Miles Driven</strong></th>  <td width=\"100px\"> <corespring-text-entry class=\"\" id=\"RESPONSE11\"></corespring-text-entry> </td>  <td width=\"100px\">440</td><td width=\"100px\">550</td><td width=\"100px\"> <corespring-text-entry class=\"\" id=\"RESPONSE12\"></corespring-text-entry> </td></tr><tr>  <th><strong>Gallons of Gas Consumed</strong></th>  <td>12</td>  <td>16</td>  <td> <corespring-text-entry class=\"\" id=\"RESPONSE13\"></corespring-text-entry> </td>  <td>32</td></tr></table></div><br></br><p class=\"prompt\"><strong>Part B:<br></br><br></br>Four cars in total were included within the report.&#160; The three additional cars that were also evaluated are listed below with statistics about their fuel efficiency.</strong><br></br><br></br><table class=\"KdsTable01\"><tr><th width=\"50px\"><strong>Car <em>B</em></strong></th><th width=\"100px\"><strong>Car <em>C</em></strong></th><th width=\"100px\"><strong>Car <em>D</em></strong></th></tr><tr>  <td>The following equation can be used to illustrate the fuel efficiency of Car <em>B</em>, <em>D</em> = 15<em>g</em>.&#160; Where <em>D</em> represents the distance traveled in miles, and <em>g</em> represents gallons of gas consumed. </td>  <td>Car <em>C</em> can travel 296 miles on a 16 gallon tank.</td>  <td>Car <em>D</em> gets 30mpg</td></tr></table><br></br><strong>Based on the fuel efficiency statistics provided for each car within the report, order the four cars from greatest miles per gallon to least miles per gallon within the table below.</strong></p><corespring-graphic-gap-match id=\"RESPONSE2\"></corespring-graphic-gap-match><br></br><div><strong>Part C:<br></br><br></br>Dillon bought one of these cars.&#160; He drove 360 miles and used 12 gallons of gas.&#160; Based on his fuel consumption, which car did he most likely buy? <br></br><br></br>Car</strong> <corespring-text-entry class=\"\" id=\"RESPONSE31\"></corespring-text-entry></div><br></br></div>",
        |    "components" : {
        |        "RESPONSE13" : {
        |            "weight" : 1,
        |            "componentType" : "corespring-text-entry",
        |            "model" : {
        |                "answerBlankSize" : 10,
        |                "answerAlignment" : "left"
        |            },
        |            "feedback" : {
        |                "correctFeedbackType" : "none",
        |                "incorrectFeedbackType" : "none"
        |            },
        |            "correctResponses" : {
        |                "award" : 100,
        |                "values" : [
        |                    "20"
        |                ],
        |                "ignoreWhitespace" : true,
        |                "ignoreCase" : true,
        |                "feedback" : {
        |                    "type" : "none",
        |                    "specific" : []
        |                }
        |            },
        |            "incorrectResponses" : {
        |                "award" : 0,
        |                "feedback" : {
        |                    "type" : "none",
        |                    "value" : ""
        |                }
        |            }
        |        },
        |        "CALCULATOR" : {
        |            "weight" : 0,
        |            "clean" : true,
        |            "title" : "Calculator",
        |            "isTool" : true,
        |            "componentType" : "corespring-calculator",
        |            "model" : {
        |                "config" : {
        |                    "type" : "basic"
        |                }
        |            }
        |        },
        |        "RESPONSE31" : {
        |            "weight" : 1,
        |            "componentType" : "corespring-text-entry",
        |            "model" : {
        |                "answerBlankSize" : 10,
        |                "answerAlignment" : "left"
        |            },
        |            "feedback" : {
        |                "correctFeedbackType" : "none",
        |                "incorrectFeedbackType" : "none"
        |            },
        |            "correctResponses" : {
        |                "award" : 100,
        |                "values" : [
        |                    "D"
        |                ],
        |                "ignoreWhitespace" : true,
        |                "ignoreCase" : true,
        |                "feedback" : {
        |                    "type" : "none",
        |                    "specific" : []
        |                }
        |            },
        |            "incorrectResponses" : {
        |                "award" : 0,
        |                "feedback" : {
        |                    "type" : "none",
        |                    "value" : ""
        |                }
        |            }
        |        },
        |        "RESPONSE2" : {
        |            "componentType" : "corespring-graphic-gap-match",
        |            "model" : {
        |                "config" : {
        |                    "shuffle" : false,
        |                    "snapEnabled" : true,
        |                    "snapSensitivity" : 0.2,
        |                    "choiceAreaPosition" : "bottom",
        |                    "backgroundImage" : {
        |                        "path" : "665010_p02.gif",
        |                        "width" : 405,
        |                        "height" : 212
        |                    },
        |                    "showHotspots" : false
        |                },
        |                "hotspots" : [
        |                    {
        |                        "id" : "Hotspot01",
        |                        "shape" : "rect",
        |                        "coords" : {
        |                            "left" : 193,
        |                            "top" : 7,
        |                            "width" : 206,
        |                            "height" : 51
        |                        }
        |                    },
        |                    {
        |                        "id" : "Hotspot02",
        |                        "shape" : "rect",
        |                        "coords" : {
        |                            "left" : 195,
        |                            "top" : 59,
        |                            "width" : 203,
        |                            "height" : 53
        |                        }
        |                    },
        |                    {
        |                        "id" : "Hotspot03",
        |                        "shape" : "rect",
        |                        "coords" : {
        |                            "left" : 195,
        |                            "top" : 113,
        |                            "width" : 205,
        |                            "height" : 48
        |                        }
        |                    },
        |                    {
        |                        "id" : "Hotspot04",
        |                        "shape" : "rect",
        |                        "coords" : {
        |                            "left" : 195,
        |                            "top" : 161,
        |                            "width" : 205,
        |                            "height" : 48
        |                        }
        |                    }
        |                ],
        |                "choices" : [
        |                    {
        |                        "id" : "Choice01",
        |                        "label" : "<img height=\"20\" width=\"65.0\" src=\"665010_p02_gi01.gif\"></img>",
        |                        "matchMax" : 0,
        |                        "matchMin" : 0
        |                    },
        |                    {
        |                        "id" : "Choice02",
        |                        "label" : "<img height=\"19\" width=\"65.0\" src=\"665010_p02_gi02.gif\"></img>",
        |                        "matchMax" : 0,
        |                        "matchMin" : 0
        |                    },
        |                    {
        |                        "id" : "Choice03",
        |                        "label" : "<img height=\"22\" width=\"58.0\" src=\"665010_p02_gi03.gif\"></img>",
        |                        "matchMax" : 0,
        |                        "matchMin" : 0
        |                    },
        |                    {
        |                        "id" : "Choice04",
        |                        "label" : "<img height=\"22\" width=\"59.0\" src=\"665010_p02_gi04.gif\"></img>",
        |                        "matchMax" : 0,
        |                        "matchMin" : 0
        |                    }
        |                ]
        |            },
        |            "feedback" : {
        |                "correctFeedbackType" : "default",
        |                "partialFeedbackType" : "default",
        |                "incorrectFeedbackType" : "default"
        |            },
        |            "correctResponse" : [
        |                {
        |                    "id" : "Choice04",
        |                    "hotspot" : "Hotspot01"
        |                },
        |                {
        |                    "id" : "Choice01",
        |                    "hotspot" : "Hotspot02"
        |                },
        |                {
        |                    "id" : "Choice03",
        |                    "hotspot" : "Hotspot03"
        |                },
        |                {
        |                    "id" : "Choice02",
        |                    "hotspot" : "Hotspot04"
        |                }
        |            ]
        |        },
        |        "RESPONSE11" : {
        |            "weight" : 1,
        |            "componentType" : "corespring-text-entry",
        |            "model" : {
        |                "answerBlankSize" : 10,
        |                "answerAlignment" : "left"
        |            },
        |            "feedback" : {
        |                "correctFeedbackType" : "none",
        |                "incorrectFeedbackType" : "none"
        |            },
        |            "correctResponses" : {
        |                "award" : 100,
        |                "values" : [
        |                    "330"
        |                ],
        |                "ignoreWhitespace" : true,
        |                "ignoreCase" : true,
        |                "feedback" : {
        |                    "type" : "none",
        |                    "specific" : []
        |                }
        |            },
        |            "incorrectResponses" : {
        |                "award" : 0,
        |                "feedback" : {
        |                    "type" : "none",
        |                    "value" : ""
        |                }
        |            }
        |        },
        |        "teacher-instructions-1501207744" : {
        |            "componentType" : "corespring-teacher-instructions",
        |            "teacherInstructions" : "TEACHER READS:Read and complete the task that follows."
        |        },
        |        "RESPONSE12" : {
        |            "weight" : 1,
        |            "componentType" : "corespring-text-entry",
        |            "model" : {
        |                "answerBlankSize" : 10,
        |                "answerAlignment" : "left"
        |            },
        |            "feedback" : {
        |                "correctFeedbackType" : "none",
        |                "incorrectFeedbackType" : "none"
        |            },
        |            "correctResponses" : {
        |                "award" : 100,
        |                "values" : [
        |                    "880"
        |                ],
        |                "ignoreWhitespace" : true,
        |                "ignoreCase" : true,
        |                "feedback" : {
        |                    "type" : "none",
        |                    "specific" : []
        |                }
        |            },
        |            "incorrectResponses" : {
        |                "award" : 0,
        |                "feedback" : {
        |                    "type" : "none",
        |                    "value" : ""
        |                }
        |            }
        |        }
        |    },
        |    "summaryFeedback" : "",
        |    "files" : [
        |        {
        |            "_t" : "org.corespring.platform.core.models.item.resource.StoredFile",
        |            "contentType" : "image/gif",
        |            "name" : "665010_p02.gif",
        |            "isMain" : false,
        |            "storageKey" : "5566882e2be4b9e8ca24daf6/9/data/665010_p02.gif"
        |        },
        |        {
        |            "_t" : "org.corespring.platform.core.models.item.resource.StoredFile",
        |            "contentType" : "image/gif",
        |            "name" : "665010_p02_gi01.gif",
        |            "isMain" : false,
        |            "storageKey" : "5566882e2be4b9e8ca24daf6/9/data/665010_p02_gi01.gif"
        |        },
        |        {
        |            "_t" : "org.corespring.platform.core.models.item.resource.StoredFile",
        |            "contentType" : "image/gif",
        |            "name" : "665010_p02_gi02.gif",
        |            "isMain" : false,
        |            "storageKey" : "5566882e2be4b9e8ca24daf6/9/data/665010_p02_gi02.gif"
        |        },
        |        {
        |            "_t" : "org.corespring.platform.core.models.item.resource.StoredFile",
        |            "contentType" : "image/gif",
        |            "name" : "665010_p02_gi03.gif",
        |            "isMain" : false,
        |            "storageKey" : "5566882e2be4b9e8ca24daf6/9/data/665010_p02_gi03.gif"
        |        },
        |        {
        |            "_t" : "org.corespring.platform.core.models.item.resource.StoredFile",
        |            "contentType" : "image/gif",
        |            "name" : "665010_p02_gi04.gif",
        |            "isMain" : false,
        |            "storageKey" : "5566882e2be4b9e8ca24daf6/9/data/665010_p02_gi04.gif"
        |        },
        |        {
        |            "_t" : "org.corespring.platform.core.models.item.resource.StoredFile",
        |            "contentType" : "text/css",
        |            "name" : "LiveInspect.css",
        |            "isMain" : false,
        |            "storageKey" : "5566882e2be4b9e8ca24daf6/9/data/LiveInspect.css"
        |        }
        |    ]
        |}
      """.stripMargin

      val playerDefinition = PlayerDefinitionFormat.reads(Json.parse(playerDefinitionJson)).asOpt

    }

    "replace url in playerDefinition" in new scope {
      val json = sut.makePlayerDefinitionJson(session, playerDefinition)

      (json \ "components" \ "RESPONSE2" \ "model" \ "config" \ "backgroundImage" \ "path").as[String] must_== "CDN/665010_p02.gif"
      val choices = (json \ "components" \ "RESPONSE2" \ "model" \ "choices").as[JsArray]
      (choices(0) \ "label" ).as[String] must_== "<img height=\"20\" width=\"65.0\" src=\"CDN/665010_p02_gi01.gif\"></img>"
      (choices(1) \ "label" ).as[String] must_== "<img height=\"19\" width=\"65.0\" src=\"CDN/665010_p02_gi02.gif\"></img>"
      (choices(2) \ "label" ).as[String] must_== "<img height=\"22\" width=\"58.0\" src=\"CDN/665010_p02_gi03.gif\"></img>"
      (choices(3) \ "label" ).as[String] must_== "<img height=\"22\" width=\"59.0\" src=\"CDN/665010_p02_gi04.gif\"></img>"
    }

    "call resolve 5 times only" in new scope {
      val json = sut.makePlayerDefinitionJson(session, playerDefinition)
      there was 5.times(mockItemAssetResolver).resolve(anyString)(anyString)
    }


  }
}
