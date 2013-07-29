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

    QtiUtils.deepCopy = function(obj) {
      if (_.isArray(obj)) {
        var res = [];
        for (var i = 0; i < obj.length; i++)
          res.push(QtiUtils.deepCopy(obj[i]));
        return res;
      } else if (_.isObject(obj)) {
        var res = {};
        for (var k in obj) {
          res[k] = QtiUtils.deepCopy(obj[k]);
        }
        return res;
      }
      else {
        return obj;
      }
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

    QtiUtils.getOutcomeValue = function(id, responses) {
        var response = QtiUtils.getResponseById(id, responses);
        if(response) return response.outcome
    }

    QtiUtils.getPromptSpan = function (xmlString) {
      var promptMatch = xmlString.match(/<:*prompt>([\s\S]*?)<\/:*prompt>/);
      var prompt = (promptMatch && promptMatch.length > 0) ? ("<span class=\"prompt\">" + promptMatch[1] + "</span>") : "";
      return prompt;
    };

    return QtiUtils;
  }
);

angular.module('qti.services').factory('Canvas', function () {
  function Canvas(id, attrs) {
    this.board = JXG.JSXGraph.initBoard(id, {
                         boundingbox: [0 - attrs.domain, attrs.range, attrs.domain, 0 - attrs.range],
                         grid: {
                             hasGrid: true,
                             gridX: attrs.scale,
                             gridY: attrs.scale
                         },
                         showNavigation: false,
                         showCopyright: false,
                         zoom: false,
                         keepaspectratio: true
                       });
    var axisAttrs = {ticks: {minorTicks: attrs.tickLabelFrequency-1, drawLabels: true}};
    this.board.create('axis', [[0, 0], [1, 0]], axisAttrs);
    this.board.create('axis', [[0, 0], [0, 1]], axisAttrs);
    if(attrs.domainLabel){
        var xcoords = new JXG.Coords(JXG.COORDS_BY_USER, [attrs.domain, 0], this.board);
        var xoffset = new JXG.Coords(JXG.COORDS_BY_SCREEN, [xcoords.scrCoords[1]-((attrs.domainLabel.length*4)+10), xcoords.scrCoords[2]+10], this.board);
        this.board.create('text', [xoffset.usrCoords[1], xoffset.usrCoords[2], attrs.domainLabel], {fixed: true});
    }
    if(attrs.rangeLabel){
        var ycoords = new JXG.Coords(JXG.COORDS_BY_USER, [0, attrs.range], this.board);
        var yoffset = new JXG.Coords(JXG.COORDS_BY_SCREEN, [ycoords.scrCoords[1]-((attrs.rangeLabel.length*4)+15), ycoords.scrCoords[2]+10], this.board);
        this.board.create('text', [yoffset.usrCoords[1], yoffset.usrCoords[2], attrs.rangeLabel], {fixed: true});
    }
    this.points = [];
    this.scale = attrs.scale;
    if(attrs.pointLabels){
        this.pointLabels = attrs.pointLabels;
    } else {
        this.pointLabels = 'letters';
    }
  }
  Canvas.prototype.getMouseCoords = function(e) {
    var cPos = this.board.getCoordsTopLeftCorner(e),
        absPos = JXG.getPosition(e),
        dx = absPos[0]-cPos[0],
        dy = absPos[1]-cPos[1];
    var coords = new JXG.Coords(JXG.COORDS_BY_SCREEN, [dx, dy], this.board);
    var simpleCoords = {
      x: coords.usrCoords[1],
      y: coords.usrCoords[2]
    };
    return simpleCoords;
  };
  Canvas.prototype.getPoint = function(ptName){
    return _.find(this.points, function(p){
        return p.name == ptName;
    });
  };
  Canvas.prototype.pointCollision = function(coords) {
    var points = this.points, scale = this.scale;
    function min(coord){
        return coord - scale;
    }
    function max(coord){
        return coord + scale;
    }
    for (var i = 0; i < points.length; i++) {
      var point = points[i];
      //find area where coords might land that would constitute collision with point
      if(point.X() >= min(coords.x) && point.X() <= max(coords.x) && point.Y() >= min(coords.y) && point.Y() <= max(coords.y)){
        return point;
      }
    }
    return null;
  };

  Canvas.prototype.addPoint = function(coords, ptName) {
    var pointAttrs = {snapToGrid: true, snapSizeX: this.scale, snapSizeY: this.scale, showInfobox: false, withLabel:false};
    var point = this.board.create('point', [coords.x, coords.y], pointAttrs);
    this.points.push(point);
    var name = (function(labels,points){
        if(ptName){
            return ptName;
        } else if(typeof labels === "string"){
            if(labels === "numbers"){
                return points.length+".";
            } else if(labels === "letters"){
                return point.name;
            } else{
                return labels.split(",")[points.length-1];
            }
        }
    })(this.pointLabels, this.points);
    //in order to get correct offset for text, we must find origin point and offset by screen coordinates,
    //then apply the offset to the point coordinates to get the correct position of text
    var origin = new JXG.Coords(JXG.COORDS_BY_USER, [0, 0], this.board);
    var offset = new JXG.Coords(JXG.COORDS_BY_SCREEN, [origin.scrCoords[1]-22, origin.scrCoords[2]-15], this.board);
    this.board.create('text', [function(){return point.X()+offset.usrCoords[1];}, function(){return point.Y()+offset.usrCoords[2];},
        function () { return name+' ('+point.X()+','+point.Y()+')'; }], {fixed: true});
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

  Canvas.prototype.makeLine = function(pts) {
      return this.board.create('line', pts, {
        strokeColor: '#00ff00',
        strokeWidth: 2,
        fixed: true
      });
  };
  Canvas.prototype.makeCurve = function(fn){
    return this.board.create('functiongraph', [fn], {
        strokeColor: '#00ff00',
        strokeWidth: 2,
        fixed: true
      })
  }
  return Canvas;
});
