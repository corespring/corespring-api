angular.module('qti.directives').factory('Canvas', function() {
var Canvas;
Canvas = (function() {
  function Canvas(board) {
    this.board = board;
    this.points = [];
  }

  Canvas.prototype.getMouseCoords = function(e, scale) {
    var coords;
    coords = new JXG.Coords(JXG.COORDS_BY_SCREEN, [e.offsetX, e.offsetY], this.board);
    coords = {
      x: coords.usrCoords[1],
      y: coords.usrCoords[2]
    };
    if (scale != null) {
      return this.interpolateCoords(coords, scale);
    } else {
      return coords;
    }
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
    var point;
    point = this.board.create('point', [coords.x, coords.y]);
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

  Canvas.prototype.prettifyPoints = function() {
    var newPoints, p, _i, _len, _ref;
    newPoints = {};
    _ref = this.points;
    for (_i = 0, _len = _ref.length; _i < _len; _i++) {
      p = _ref[_i];
      newPoints[p.name] = {
        x: p.coords.usrCoords[1],
        y: p.coords.usrCoords[2]
      };
    }
    return newPoints;
  };

  Canvas.prototype.interpolateCoords = function(coords, scale) {
    var interpolate;
    interpolate = function(num) {
      return Math.round(num / scale) * scale;
    };
    return {
      x: interpolate(coords.x),
      y: interpolate(coords.y)
    };
  };

  return Canvas;

})();
return Canvas;
});

angular.module('qti.directives').directive('jsxGraph', function(Canvas) {
return {
  template: "<div id='box' class='jxgbox' style='width:100%; height:100%;'></div>",
  restrict: 'A',
  scope: {
    boardParams: '=',
    points: '=',
    setPoints: '=',
    lockPoints: '=',
    maxPoints: '@',
    scale: '@'
  },
  link: function(scope, elem, attr) {
    var addPoint, canvas, domain, onPointMove, range;
    domain = scope.boardParams.domain;
    range = scope.boardParams.range;
    if (domain && range) {
      canvas = new Canvas(JXG.JSXGraph.initBoard('box', {
        boundingbox: [0 - domain, range, domain, 0 - range],
        grid: true,
        axis: true,
        showNavigation: false,
        showCopyright: false
      }));
      onPointMove = function(point, coords) {
        var newCoords;
        newCoords = coords != null ? canvas.interpolateCoords({
          x: coords.x,
          y: coords.y
        }, scope.scale) : canvas.interpolateCoords({
          x: point.X(),
          y: point.Y()
        }, scope.scale);
        point.moveTo([newCoords.x, newCoords.y]);
        scope.points[point.name] = newCoords;
      };
      addPoint = function(coords) {
        var line, point;
        point = canvas.addPoint(coords);
        point.on("up", function() {
          onPointMove(point);
        });
        onPointMove(point);
        if (canvas.points.length === 2) {
          line = canvas.makeLine();
        }
        return point;
      };
      canvas.on('up', function(e) {
        var coords;
        coords = canvas.getMouseCoords(e, scope.scale);
        if (canvas.points.length < scope.maxPoints) {
          addPoint(coords);
        }
      });
      scope.lockPoints = function(lockit){
        if(lockit){
            _.each(canvas.points,function(p){
                p.setAttribute({fixed: true})
            })
        }
      }
      scope.$watch('points', function(newValue, oldValue) {
        if (newValue !== oldValue) {
          var _ref = scope.points;
          for (var ptName in _ref) {
            var pts = _ref[ptName];
            var coordx = parseFloat(pts.x);
            var coordy = parseFloat(pts.y);
            if (!isNaN(coordx) && !isNaN(coordy)) {
              var coords = {
                x: coordx,
                y: coordy
              };
              var canvasPointRef = null;
              var _ref1 = canvas.points;
              for (var _i = 0, _len = _ref1.length; _i < _len; _i++) {
                var canvasPoint = _ref1[_i];
                if (ptName === canvasPoint.name) {
                  canvasPointRef = canvasPoint;
                }
              }
              if (canvasPointRef != null) {
                if (canvasPointRef.X() !== coords.x || canvasPointRef.Y() !== coords.y) {
                  onPointMove(canvasPointRef, coords);
                }
              } else if (canvas.points.length < scope.maxPoints) {
                addPoint(coords);
              }
            }
          }
        }
      }, true);
    } else {
      console.error("domain and/or range unspecified");
    }
  }
};
});