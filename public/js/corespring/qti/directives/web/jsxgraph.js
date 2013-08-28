'use strict';
angular.module('qti.directives').directive('jsxGraph', function(Canvas,QtiUtils) {
return {
  template: "<div class='jxgbox' ng-style='boxStyle' style='width: 100%; height: 100%'></div>",
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
    //      y:[Number],
    //      color: [String]
    //    }
    //    ...
    //  },
    //  drawShape:{
    //    line: [pt1,pt2],
    //    showPoints: Boolean
    //  },
    //  pointsStyle: [String], //set the color of all the points and labels of points
    //  graphStyle: {}, //set the css
    //  shapesStyle: [String], //set the color of all shapes (e.g. lines) on the graph
    //  lockGraph: Boolean
    //}
    graphCallback: '='
  },
  link: function(scope, elem, attr) {
    //global vars
    var canvasAttrs = {
        domain: parseInt(attr.domain?attr.domain:10),
        range: parseInt(attr.range?attr.range:10),
        scale: parseFloat(attr.scale?attr.scale:1),
        maxPoints: parseInt(attr.maxpoints?attr.maxpoints:null),
        domainLabel: attr.domainlabel,
        rangeLabel: attr.rangelabel,
        tickLabelFrequency: parseInt(attr.ticklabelfrequency?attr.ticklabelfrequency:5),
        pointLabels: attr.pointlabels,
        width: elem.width(),
        height: elem.height()
    };
    function generateCanvasId(){
        var alphabet = "abcdefghijklmnopqrstuvwxyz".split('')
        var canvasId = ""
        for(var i = 0; i < 6; i++){
            canvasId = canvasId + alphabet[Math.floor(Math.random()*alphabet.length)]
        }
        elem.find(".jxgbox").attr("id",canvasId)
        return canvasId
    }
    var canvas = new Canvas(generateCanvasId(), canvasAttrs);
    //define callbacks
    canvas.on('up', function(e) {
      var coords = canvas.getMouseCoords(e);
      if ((!canvasAttrs.maxPoints || canvas.points.length < canvasAttrs.maxPoints) && !canvas.pointCollision(coords)) {
        addPoint(coords);
      }
    });
    var lockGraph = false;
    var points = {}
    var onPointMove = function(point, coords) {
      if(!lockGraph){
          if(coords != null) point.moveTo([coords.x, coords.y]);
          points[point.name] = {x: point.X(), y: point.Y()};
          scope.interactionCallback({points: points});
      }
    };
    var addPoint = function(coords, ptName) {
      if(!lockGraph){
        var point = canvas.addPoint(coords, ptName);
        point.on('up',function(e){
            onPointMove(point);
        })
        onPointMove(point);
        return point;
      }
    };
    var clearBoard = function(){
        while(canvas.points.length > 0){
            canvas.popPoint();
        }
        while(canvas.shapes.length > 0){
            canvas.popShape();
        }
        points = {}
    }
    scope.graphCallback = function(params){
        if(params.points && canvas){
            clearBoard();
            if(QtiUtils.isObject(params.points)){
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
                      canvasPoint = addPoint(coords);
                  }
                  if(point.color) canvas.changePointColor(canvasPoint, point.color)
                }
              }
            }else if(QtiUtils.isArray(params.points)){
                for(var i = 0; i < params.points.length; i++){
                    var coordx = parseFloat(params.points[i].x);
                    var coordy = parseFloat(params.points[i].y);
                    if (!isNaN(coordx) && !isNaN(coordy)) {
                        var coords = {
                            x: coordx,
                            y: coordy
                        };
                        var canvasPoint = canvas.pointCollision(coords)
                        if(canvasPoint == null){
                            canvasPoint = addPoint(coords)
                        }
                    }
                    if(params.points[i].color) canvas.changePointColor(canvasPoint, params.points[i].color)
                }
            }
        }
        if(params.drawShape && canvas){
             if(params.drawShape.line && !lockGraph){
                 var pt1 = canvas.getPoint(params.drawShape.line[0]);
                 var pt2 = canvas.getPoint(params.drawShape.line[1]);
                 if(pt1 && pt2){
                     canvas.makeLine([pt1,pt2]);
                 }
             }else if(params.drawShape.curve && !lockGraph){
                canvas.makeCurve(params.drawShape.curve)
             }
        }
        if(params.pointsStyle){
            _.each(canvas.points, function(p){
                canvas.changePointColor(p, params.pointsStyle)
            })
        }
        if(params.graphStyle){
            scope.boxStyle = _.extend({width: "100%", height: "100%"}, params.graphStyle)
        }
        if(params.shapesStyle){
            _.each(canvas.shapes, function(shape){
                canvas.changeShapeColor(shape, params.shapesStyle)
            })
        }
        if(params.lockGraph){
             _.each(canvas.points,function(p){
                 p.setAttribute({fixed: true});
             });
             lockGraph = true;
        }
    };
  }
};
});