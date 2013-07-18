angular.module('qti.directives').directive('jsxGraph', function(Canvas) {
return {
  template: "<div id='box' class='jxgbox' ng-style='boxStyle' style='width: 100%; height: 100%;'></div>",
  restrict: 'A',
  scope: {
    //{
    //  points:{...}
    //}
    interactionCallback: '=',
    //{
    //  points:{
    //    [ptName]:{
    //      x:[Number],
    //      y:[Number]
    //    }
    //    ...
    //  },
    //  drawShape:{
    //    line: [pt1,pt2]
    //  },
    //  submission: {
    //    isIncomplete:[Boolean],
    //    clearBorder:[Boolean],
    //    lockGraph:[Boolean],
    //    isCorrect:[Boolean]
    //  }
    //}
    graphCallback: '=',
  },
  link: function(scope, elem, attr) {
    //global vars
    var canvasAttrs = {
        domain: parseInt(attr.domain?attr.domain:10),
        range: parseInt(attr.range?attr.range:10),
        scale: parseFloat(attr.scale?attr.scale:1),
        maxPoints: parseInt(attr.maxPoints?attr.maxPoints:null),
        domainLabel: attr.domainlabel,
        rangeLabel: attr.rangelabel
    }
    var canvas = new Canvas("box", canvasAttrs);
    var lockGraph = false;
    var points = {}
    var onPointMove = function(point, coords) {
      if(!lockGraph){
          if(coords != null) point.moveTo([coords.x, coords.y]);
          points[point.name] = {x: point.X(), y: point.Y()};
          scope.interactionCallback({points: points})
      }
    };
    var addPoint = function(coords, ptName) {
      if(!lockGraph){
        var point = canvas.addPoint(coords, ptName);
        point.on('up',function(e){
            onPointMove(point)
        })
        onPointMove(point)
        return point;
      }
    };
    //define callbacks
    canvas.on('up', function(e) {
      var coords = canvas.getMouseCoords(e);
      if ((!canvasAttrs.maxPoints || canvas.points.length < canvasAttrs.maxPoints) && !canvas.pointCollision(coords)) {
        addPoint(coords);
      }
    });
    scope.graphCallback = function(params){
        if(params.points){
            for (var ptName in params.points) {
              var point = params.points[ptName];
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
                } else if (!canvasAttrs.maxPoints || canvas.points.length < canvasAttrs.maxPoints) {
                  addPoint(coords);
                }
              }
            }
        }
        if(params.drawShape){
             if(params.drawShape.line && !lockGraph){
                 var pt1 = canvas.getPoint(params.drawShape.line[0])
                 var pt2 = canvas.getPoint(params.drawShape.line[1])
                 if(pt1 && pt2){
                     canvas.makeLine([pt1,pt2])
                 }
             }
        }
        if(params.submission){
             if(params.submission.isIncomplete){
               scope.boxStyle = {width: "100%", height: "100%", borderColor: "yellow", borderWidth: "2px"};
             }else if(params.submission.clearBorder){
               scope.boxStyle = {width: "100%", height: "100%"};
             }else{
                 if(params.submission.lockGraph){
                     _.each(canvas.points,function(p){
                         p.setAttribute({fixed: true})
                     })
                     lockGraph = true;
                 }
                 if(params.submission.isCorrect){
                   scope.boxStyle = {width: "100%", height: "100%", borderColor: "green", borderWidth: "2px"};
                 }else if(params.submission.hasOwnProperty('isCorrect')){
                    scope.boxStyle = {width: "100%", height: "100%", borderColor: "red", borderWidth: "2px"};
                 }
             }
        }
    }
  }
};
});