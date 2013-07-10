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

    QtiUtils.getOutcomeValue = function(id, responses) {
        var response = QtiUtils.getResponseById(id, responses);
        if(response) return response.outcome
    }
    return QtiUtils;
  }
);

angular.module('qti.services').factory('Canvas', function() {
  function Canvas(id, domain, range, scale) {
    this.board = JXG.JSXGraph.initBoard(id, {
                         boundingbox: [0 - domain, range, domain, 0 - range],
                         grid: {
                             hasGrid: true,
                             gridX: scale,
                             gridY: scale
                         },
                         axis: true,
                         showNavigation: false,
                         showCopyright: false,
                         zoom: false,
                         keepaspectratio: true
                       });
    this.points = [];
    this.scale = scale
  }
  Canvas.prototype.getMouseCoords = function(e) {
    var coords = new JXG.Coords(JXG.COORDS_BY_SCREEN, [e.offsetX, e.offsetY], this.board);
    var simpleCoords = {
      x: coords.usrCoords[1],
      y: coords.usrCoords[2]
    };
    return simpleCoords
  };

  Canvas.prototype.pointCollision = function(coords) {
    var el, _i, _len, _ref;
    _ref = this.board.objects;
    for (_i = 0, _len = _ref.length; _i < _len; _i++) {
      el = _ref[_i];
      if (JXG.isPoint(this.board.objects[el]) && this.board.objects[el].hasPoint(coords.x, coords.y)) {
        return el;
      } else {
        return null;
      }
    }
  };

  Canvas.prototype.addPoint = function(coords) {
    var point = this.board.create('point', [coords.x, coords.y], {snapToGrid: true, snapSizeX: this.scale, snapSizeY: this.scale});
    this.points.push(point);
    return point;
  };

  Canvas.prototype.popPoint = function() {
    return this.board.removeObject(this.points.pop);
  };

  Canvas.prototype.removePoint = function(pointId) {
    var p, _i, _len, _ref, _results;
    this.board.removeObject(pointId);
    _ref = this.points;
    _results = [];
    for (_i = 0, _len = _ref.length; _i < _len; _i++) {
      p = _ref[_i];
      if (p.id !== pointId) {
        _results.push(this.points = p);
      }
    }
    return _results;
  };

  Canvas.prototype.on = function(event, handler) {
    return this.board.on(event, handler);
  };

  Canvas.prototype.makeLine = function() {
    if (this.points.length === 2) {
      return this.board.create('line', [this.points[0], this.points[1]], {
        strokeColor: '#00ff00',
        strokeWidth: 2,
        fixed: true
      });
    }
  };
  return Canvas;
});
