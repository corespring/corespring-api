angular.module('qti.directives').directive('jsxGraph', function(Canvas) {
return {
  template: "<div id='box' class='jxgbox' style='width:100%; height:100%;'></div>",
  restrict: 'A',
  scope: {
    points: '=',
    lockPoints: '='
  },
  link: function(scope, elem, attr) {
    var domain = parseInt(attr.domain?attr.domain:10);
    var range = parseInt(attr.range?attr.range:10);
    var scale = parseFloat(attr.scale?attr.scale:1)
    var canvas = new Canvas(domain,range,scale);
    var onPointMove = function(point, coords) {
      if(coords != null) point.moveTo([coords.x, coords.y]);
      scope.points[point.name] = {x: point.X(), y: point.Y()};
    };
    var addPoint = function(coords) {
      var point = canvas.addPoint(coords);
      point.on('up',function(e){
          onPointMove(point)
      })
      onPointMove(point)
      if (canvas.points.length === 2) {
        var line = canvas.makeLine();
      }
      return point;
    };
    canvas.on('up', function(e) {
      var coords = canvas.getMouseCoords(e);
      if (canvas.points.length < 2) {
        addPoint(coords);
      }
    });
    scope.lockPoints = function(lockit){
      if(lockit){
          _.each(canvas.points,function(p){
              p.setAttribute({fixed: true})
          })
      }else{
          _.each(canvas.points,function(p){
              p.setAttribute({fixed: false})
          })
      }
    }
    scope.$watch('points', function(newValue, oldValue) {
      if (newValue !== oldValue) {
        for (var ptName in scope.points) {
          var point = scope.points[ptName];
          var coordx = parseFloat(point.x);
          var coordy = parseFloat(point.y);
          if (!isNaN(coordx) && !isNaN(coordy)) {
            var coords = {
              x: coordx,
              y: coordy
            };
            var canvasPoint = null;
            for (var i = 0; i < canvas.points.length; i++) {
              if (ptName === canvas.points[i].name) {
                canvasPoint = canvas.points[i];
              }
            }
            //if the coordinates for a point that exists has changed, then update that point
            //otherwise, a new point will be created
            if (canvasPoint != null) {
              if (canvasPoint.X() !== coords.x || canvasPoint.Y() !== coords.y) {
                onPointMove(canvasPoint, coords);
              }
            } else if (canvas.points.length < 2) {
              addPoint(coords);
            }
          }
        }
      }
    }, true);
  }
};
});