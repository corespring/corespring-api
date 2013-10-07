# POC Integration - Session mapping

## POC

### Submit Answers:

#### Request

    {
      "answers": {
        "3": {
          "value": "2"
        }
      }
    }

#### Response

    {
      "outcome": {
        "components": {
          "3": {
            "score": 0.0,
            "weight": 4,
            "weightedScore": 0.0
          }
        },
        "summary": {
          "maxPoints": 15,
          "percentage": 6.7,
          "points": 1.0
        }
      },
      "responses": {
        "3": {
          "correctness": "incorrect",
          "feedback": [
            {
              "correct": true,
              "feedback": "It was this one silly!",
              "value": "2"
            },
            {
              "correct": false,
              "feedback": "Huh?",
              "value": "1"
            }
          ],
          "score": 0
        }
      },
      "session": {
        "_id": {
          "$oid": "5252ebf030042bb20692084b"
        },
        "answers": {
          "3": {
            "value": "1"
          }
        },
        "isFinished": true,
        "itemId": "522267c2554f43f858000001",
        "maxNoOfAttempts": 2,
        "remainingAttempts": 0,
        "showCorrectResponse": true,
        "showFeedback": true,
        "showUserResponse": true
      }
    }


## Corespring API

#### Request


    {
      "id": "5252ea163004303bcbbb3d94",
      "itemId": "5252c7c108738c6d85653725:0",
      "responses": [
        {
          "id": "Q_01",
          "value": "ChoiceA"
        }
      ],
      "settings": {
        "allowEmptyResponses": false,
        "highlightCorrectResponse": true,
        "highlightUserResponse": true,
        "maxNoOfAttempts": 1,
        "showFeedback": true,
        "submitCompleteMessage": "Ok! Your response was submitted.",
        "submitIncorrectMessage": "You may revise your work before you submit your final response."
      }
    }


#### Response

    {
      "dateModified": 1381165608244,
      "finish": 1381165608244,
      "id": "5252ea163004303bcbbb3d94",
      "isFinished": true,
      "isStarted": true,
      "itemId": "5252c7c108738c6d85653725:0",
      "outcome": {
        "Q_01": {
          "isComplete": true,
          "isCorrect": true,
          "score": 1.0
        },
        "isComplete": true,
        "isCorrect": true,
        "score": 1.0
      },
      "responses": [
        {
          "id": "Q_01",
          "outcome": {
            "isCorrect": true,
            "score": 1.0
          },
          "value": "ChoiceA"
        }
      ],
      "sessionData": {
        "correctResponses": [
          {
            "id": "Q_01",
            "value": "ChoiceA"
          }
        ],
        "feedbackContents": {
          "1": "Correct!"
        }
      },
      "settings": {
        "allowEmptyResponses": false,
        "highlightCorrectResponse": true,
        "highlightUserResponse": true,
        "maxNoOfAttempts": 1,
        "showFeedback": true,
        "submitCompleteMessage": "Ok! Your response was submitted.",
        "submitIncorrectMessage": "You may revise your work before you submit your final response."
      },
      "start": 1381165608060
    }

