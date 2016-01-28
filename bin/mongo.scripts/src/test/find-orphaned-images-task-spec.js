/* global FindOrphanedImagesTask */

describe("FindOrphanedImagesTask", function () {
  var sut;

  beforeEach(function () {
    sut = new FindOrphanedImagesTask();
  });

  describe("findString", function () {
    var html = "<img src=\"Cindy Bought a Pencil Item_Choice C.JPG\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.imsglobal.org/xsd/imsqti_v2p1\"/>";
    var str = "Cindy Bought a Pencil Item_Choice C.JPG";

    it("should return true if o contains the string", function () {
      expect(sut.findString(html, str)).toBe(true);
    });
    it("should return true if o.o contains the string", function () {
      expect(sut.findString({o: str}, str)).toBe(true);
    });
    it("should return true if o.o.o contains the string", function () {
      expect(sut.findString({o: {o: html}}, str)).toBe(true);
    });
    it("should return true if o[0] contains the string", function () {
      expect(sut.findString([html], str)).toBe(true);
    });
    it("should return true if o.o[0] contains the string", function () {
      expect(sut.findString({o: [html]}, str)).toBe(true);
    });
    it("should return false if o is null", function () {
      expect(sut.findString(null, str)).toBe(false);
    });
    it("should return false if o is undefined", function () {
      expect(sut.findString(undefined, str)).toBe(false);
    });
    it("should return false if o is a boolean", function () {
      expect(sut.findString(true, str)).toBe(false);
    });
    it("should return false if o is a number", function () {
      expect(sut.findString(123, str)).toBe(false);
    });

    describe("encoded strings", function(){
      it("should return true if str is encoded", function () {
        expect(sut.findString([html], encodeURIComponent(str))).toBe(true);
      });
      it("should return true if html is encoded", function () {
        expect(sut.findString([encodeURIComponent(html)], str)).toBe(true);
      });
      it("should return true if both are encoded", function () {
        expect(sut.findString([encodeURIComponent(html)], encodeURIComponent(str))).toBe(true);
      });
    })

  });

  describe("findOrphanedFiles", function () {
    function itemWithoutOrphanedImages() {
      return {
        "_id": {
          "_id": "4ffde711e4b0accb1d7e07cc",
          "version": 1
        },
        "playerDefinition": {
          "files": [
            {
              "name": "Cindy Bought a Pencil Item_Choice A.JPG",
              "contentType": "image/jpeg",
              "isMain": false,
              "storageKey": ""
            },
            {
              "name": "Cindy Bought a Pencil Item_Choice B.JPG",
              "contentType": "image/jpeg",
              "isMain": false,
              "storageKey": ""
            },
            {
              "name": "Cindy Bought a Pencil Item_Choice D.JPG",
              "contentType": "image/jpeg",
              "isMain": false,
              "storageKey": ""
            },
            {
              "name": "Cindy Bought a Pencil Item_Choice C.JPG",
              "contentType": "image/jpeg",
              "isMain": false,
              "storageKey": ""
            }
          ],
          "xhtml": "<div class=\"item-body qti\"><div class=\"para intro-2\">Cindy had $1.00. Then she bought a pencil for $0.37.</div>                               <div class=\"para prompt\">How much money does she have now?</div>                                                         <corespring-multiple-choice id=\"RESPONSE\"></corespring-multiple-choice>    </div>",
          "components": {
            "RESPONSE": {
              "componentType": "corespring-multiple-choice",
              "model": {
                "config": {
                  "shuffle": "false",
                  "orientation": "vertical",
                  "choiceType": "radio",
                  "choiceLabels": "letters",
                  "choiceStyle": "",
                  "showCorrectAnswer": "inline"
                },
                "choices": [
                  {
                    "label": "<img src=\"Cindy Bought a Pencil Item_Choice A.JPG\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.imsglobal.org/xsd/imsqti_v2p1\"/>",
                    "value": "ChoiceA"
                  },
                  {
                    "label": "<img src=\"Cindy Bought a Pencil Item_Choice B.JPG\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.imsglobal.org/xsd/imsqti_v2p1\"/>",
                    "value": "ChoiceB"
                  },
                  {
                    "label": "<img src=\"Cindy Bought a Pencil Item_Choice C.JPG\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.imsglobal.org/xsd/imsqti_v2p1\"/>",
                    "value": "ChoiceC"
                  },
                  {
                    "label": "<img src=\"Cindy Bought a Pencil Item_Choice D.JPG\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.imsglobal.org/xsd/imsqti_v2p1\"/>",
                    "value": "ChoiceD"
                  }
                ]
              },
              "feedback": [
                {
                  "value": "ChoiceA",
                  "feedback": "Correct!"
                },
                {
                  "value": "ChoiceB",
                  "feedback": "Your answer."
                },
                {
                  "value": "ChoiceC",
                  "feedback": "Your answer."
                },
                {
                  "value": "ChoiceD",
                  "feedback": "Your answer."
                }
              ],
              "correctResponse": {
                "value": [
                  "ChoiceA"
                ]
              }
            }
          },
          "summaryFeedback": ""
        }
      };
    }

    function itemWithOrphanedImages() {
      return {
        "_id": {
          "_id": "5687ec18e4b0d4525c542a39",
          "version": 0
        },
        "playerDefinition": {
          "files": [
            {
              "name": "1.PNG",
              "contentType": "image/png",
              "isMain": false,
              "storageKey": "1.PNG"
            },
            {
              "name": "FullSizeRender.jpg",
              "contentType": "image/jpeg",
              "isMain": false,
              "storageKey": "FullSizeRender.jpg"
            },
            {
              "name": "FullSizeRender%20(1).jpg",
              "contentType": "image/jpeg",
              "isMain": false,
              "storageKey": "FullSizeRender%20(1).jpg"
            }
          ],
          "xhtml": "<div>Match the quotient, models, and problem situations to the equation.<div></div>​<div id=\"0\" corespring-dnd-categorize=\"\"></div>​<div>y</div></div>",
          "components": {
            "0": {
              "weight": 1,
              "componentType": "corespring-dnd-categorize",
              "title": "Drag and Drop Categorize",
              "minimumWidth": 600,
              "correctResponse": {
                "cat_1": [
                  "choice_1",
                  "choice_8"
                ],
                "cat_2": [
                  "choice_2",
                  "choice_9"
                ],
                "cat_3": [
                  "choice_3",
                  "choice_10"
                ]
              },
              "feedback": {
                "correctFeedbackType": "default",
                "partialFeedbackType": "default",
                "incorrectFeedbackType": "default"
              },
              "allowPartialScoring": false,
              "partialScoring": {
                "sections": [
                  {
                    "catId": "cat_1",
                    "partialScoring": [
                      {
                        "numberOfCorrect": 1,
                        "scorePercentage": 0
                      }
                    ]
                  },
                  {
                    "catId": "cat_2",
                    "partialScoring": [
                      {
                        "numberOfCorrect": 1,
                        "scorePercentage": 0
                      }
                    ]
                  },
                  {
                    "catId": "cat_3",
                    "partialScoring": [
                      {
                        "numberOfCorrect": 1,
                        "scorePercentage": 0
                      }
                    ]
                  }
                ]
              },
              "allowWeighting": true,
              "weighting": {
                "cat_1": 1,
                "cat_2": 1,
                "cat_3": 1
              },
              "model": {
                "categories": [
                  {
                    "id": "cat_1",
                    "label": "<div>​<span mathjax=\"\">\\(5 \\div \\frac14=\\)</span>​<br>​</div>"
                  },
                  {
                    "id": "cat_2",
                    "label": "<div>​<span mathjax=\"\">\\(7 \\div \\frac15=\\)</span>​<div><br></div></div>"
                  },
                  {
                    "id": "cat_3",
                    "label": "<div><div>​<span mathjax=\"\">\\(8 \\div \\frac13=\\)</span>​​​​​​​</div></div>"
                  }
                ],
                "choices": [
                  {
                    "id": "choice_1",
                    "label": "<div>20<div></div></div>",
                    "moveOnDrag": false
                  },
                  {
                    "id": "choice_2",
                    "label": "<div>35<div></div></div>",
                    "moveOnDrag": false
                  },
                  {
                    "id": "choice_3",
                    "label": "<div>24<div></div></div>",
                    "moveOnDrag": true
                  },
                  {
                    "id": "choice_8",
                    "label": "<div>Maggie cuts her pizzas into four equivalent slices. If she does this to five pizzas, how many slices will there be?</div>",
                    "moveOnDrag": false
                  },
                  {
                    "id": "choice_9",
                    "label": "<div>Mark cuts his pies into five equivalent slices. If he does this to seven pies, how many slices of pie will there be?</div>",
                    "moveOnDrag": false
                  },
                  {
                    "id": "choice_10",
                    "label": "<div>Noah cuts his oranges into eight equivalent slices. If he does this to two oranges, how may slices will there be?</div>",
                    "moveOnDrag": false
                  }
                ],
                "config": {
                  "shuffle": true,
                  "answerAreaPosition": "below",
                  "categoriesPerRow": 3,
                  "choicesPerRow": 3,
                  "choicesLabel": ""
                }
              }
            }
          },
          "summaryFeedback": ""
        }
      };
    }

    it("should find orphaned images", function () {
      expect(sut.findOrphanedImages(itemWithOrphanedImages())).toEqual([
        {name: '1.PNG', contentType: 'image/png', isMain: false, storageKey: '1.PNG'},
        {name: 'FullSizeRender.jpg', contentType: 'image/jpeg', isMain: false, storageKey: 'FullSizeRender.jpg'},
        {
          name: 'FullSizeRender%20(1).jpg',
          contentType: 'image/jpeg',
          isMain: false,
          storageKey: 'FullSizeRender%20(1).jpg'
        }
      ]);
    });
    it("should not find orphaned images", function () {
      expect(sut.findOrphanedImages(itemWithoutOrphanedImages())).toEqual([]);
    });
  });
});