'use strict';

angular.module('qti.services', ['ngResource']);

angular.module('qti.services').factory('AggregateService', ['$resource', function ($resource) {
  var api = PlayerRoutes;

  var calls = {
    aggregate: api.aggregate(":quizId", ":itemId")
  };

  calls.aggregate.params = {"quizId": '@quizId', 'itemId': '@itemId'};

  return $resource(
    calls.aggregate.url,
    {},
    { aggregate: calls.aggregate }
  );

}]);

angular.module('qti.services').factory('AssessmentSessionService', ['$resource', function ($resource) {

  var api = PlayerRoutes;
  var calls = {
    get: api.read(":itemId", ":sessionId"),
    create: api.create(":itemId"),
    update: api.update(":itemId", ":sessionId")
  };

  return $resource(
    calls.get.url,
    {},
    {
      create: calls.create,
      save: calls.update,
      begin: { method: calls.update.method, params: { action: "begin"} },
      updateSettings: { method: calls.update.method, params: { action: "updateSettings"} }
    }
  );

}]);

angular.module('qti.services')
  .factory('QtiUtils', function () {
    var QtiUtils = {};

    QtiUtils.ERROR = {
      undefinedElements: "Error: can't compare 2 undefined elements"
    };

    // TODO - this will need to support other comparisons... e.g. two arrays for orderInteraction to ensure correct order & other QTI response types like matching?
    // function checks if value == response, or if response is array it checks if the array contains the value
    QtiUtils.compare = function (choiceValue, response) {

      if (choiceValue === undefined && response === undefined) {
        throw QtiUtils.ERROR.undefinedElements;
      }

      if (response instanceof Array) {
        return (response.indexOf(choiceValue) != -1)
      }
      return (response === choiceValue)
    };

    QtiUtils.compareArrays = function(arr1, arr2) {
      if (arr1.length != arr2.length) return false;
      for (var i = 0; i < arr1.length; i++) {
        if (arr1[i].compare) {
          if (!arr1[i].compare(arr2[i])) return false;
        }
        if (arr1[i] !== arr2[i]) return false;
      }
      return true;
    };

    QtiUtils.compareArraysIgnoringOrder = function(arr1, arr2) {
      return $(arr1).not(arr2).length == 0 && $(arr2).not(arr1).length == 0

    };

    QtiUtils.getResponseById = function (id, responses) {

      if (!id || !responses) {
        return null;
      }

      for (var i = 0; i < responses.length; i++) {
        if (responses[i].id == id) {
          return responses[i];
        }
      }

      return null;
    };


    QtiUtils.isResponseCorrect = function (response) {
      if (!response || !response.outcome) {
        return false;
      }
      return response.outcome.score == 1;
    };

    /**
     * Get the value from the response object
     * @param id
     * @param responses
     * @param defaultValue
     * @return {*}
     */
    QtiUtils.getResponseValue = function (id, responses, defaultValue) {
      defaultValue = (defaultValue || "");

      try {
        var response = QtiUtils.getResponseById(id, responses);
        if (response)  return response.value;
      } catch (e) {
        // just means it isn't set, leave it as ""
      }
      return defaultValue;
    };

    QtiUtils.getPromptSpan = function (xmlString) {
      var promptMatch = xmlString.match(/<:*prompt>([\s\S]*?)<\/:*prompt>/);
      var prompt = "<span class=\"prompt\">" + ((promptMatch && promptMatch.length > 0) ? promptMatch[1] : "") + "</span>";
      return prompt;
    };

    return QtiUtils;
  }
);
