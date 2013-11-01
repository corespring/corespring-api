'use strict';
angular.module("qti.directives").directive("graphline", function(){
  return {
    restrict: "E",
    require: '^lineinteraction',
    scope: 'true',
    compile: function(element,attrs,transclude){
      element.attr('hidden','');
      var locked = element.parent()[0].attributes.getNamedItem('locked')?true:false;
      return function(scope,element,attrs,LineCtrl){
        var points = _.map(element.find('point'),function(p){
          var coords = p.innerHTML.split(",");
          if(coords.length == 2){
            return {x: coords[0], y: coords[1]};
          }else {
            throw "each point must contain x and y coordinate separated by a comma";
          }
        })
        if(points.length == 2){
          LineCtrl.setInitialParams({
            points: {
              A: points[0],
              B: points[1]
            },
            drawShape:{
              line: ["A","B"]
            },
            submission:{lockGraph: locked}
          });
        } else{
          throw "line must contain 2 points";
        }
      };
    }
  }
});
angular.module("qti.directives").directive("graphcurve", function(){
  return {
    restrict: "E",
    require: '^lineinteraction',
    scope: 'true',
    compile: function(element,attrs,transclude){
      element.attr('hidden','');
      var locked = element.parent()[0].attributes.getNamedItem('locked')?true:false;
      return function(scope,element,attrs,LineCtrl){
        var innertext = element[0].innerHTML;
        if(!innertext) throw "graphcurve must contain text";
        var equation = innertext.split('=')[1];
        if(equation){
          equation = equation.replace("x","*x")
          LineCtrl.setInitialParams({
            drawShape:{
              curve: function(x){return eval(equation)}
            },
            submission:{lockGraph: locked}
          })
        }
      }
    }
  }
})
angular.module("qti.directives").directive("lineinteraction", ['$compile', function($compile){
  function setDefaultInputStyles(scope){
    scope.inputStyle = {width: "40px"}
  }
  return {
    template: [
      "<div class='container-fluid graph-interaction' >",
      "   <div id='additional-text' class='row-fluid additional-text' ng-show='additionalText'>",
      "       <p>{{additionalText}}</p>",
      "   </div>",
      "   <div class='row-fluid'>",
      "       <div id='inputs' class='span5' ng-show='showInputs' style='margin-right: 17px;'>",
      "           <div class='point-display' style='padding-bottom: 10px;'>",
      "              <p>Point A:</p>",
      "              <p>x: </p>",
      "              <input type='text' ng-style='inputStyle', ng-model='points.A.x' ng-disabled='locked'>",
      "              <p>y: </p>",
      "              <input type='text' ng-style='inputStyle' ng-model='points.A.y'  ng-disabled='locked'>",
      "          </div>",
      "          <hr class='point-display-break'>",
      "          <div class='point-display' style='padding-top: 10px;'>",
      "             <p>Point B:</p>",
      "             <p>x: </p>",
      "             <input type='text' ng-style='inputStyle' ng-model='points.B.x' ng-disabled='locked'>",
      "             <p>y: </p>",
      "             <input type='text' ng-style='inputStyle' ng-model='points.B.y' ng-disabled='locked'>",
      "          </div>",
      "      </div>",
      "      <div id='scale-display' class='span4 scale-display' ng-show='showInputs'>",
      "          <p>scale={{scale}}</p>",
      "          <button type='button' class='btn btn-default btn-undo' ng-click='undo()'>Undo</button>",
      "          <button type='button' class='btn btn-default btn-start-over' ng-click='startOver()'>Start Over</button>",
      "      </div>",
      "   </div>",
      "   <div id='graph-container' class='row-fluid graph-container'></div>",
      "   <div id='initialParams' ng-transclude></div>",
      "</div>"
    ].join("\n"),
    transclude: true,
    restrict: 'E',
    scope: true,
    require: '?^assessmentitem',
    controller: ['$scope', function($scope){
      $scope.submissions = 0
      this.setInitialParams = function(initialParams){
        $scope.initialParams = initialParams;
      };
      $scope.$watch('graphCallback',function(){
        if($scope.graphCallback){
          if($scope.initialParams){
            $scope.graphCallback($scope.initialParams);
          }
          if($scope.locked){
            $scope.graphCallback({lockGraph: true})
          }
        }
      })
      $scope.points = {A: {x: undefined, y: undefined, isSet:false}, B: {x: undefined, y: undefined, isSet:false}};
      $scope.$watch('showNoResponseFeedback', function(){
        if(!$scope.locked && $scope.isEmptyItem($scope.graphCoords) && $scope.showNoResponseFeedback){
          $scope.graphCallback({graphStyle: {borderColor: "yellow", borderWidth: "2px"}});
        }
      });
      $scope.interactionCallback = function(params){
        //set scope point to params point, rounding if necessary
        function setPoint(name){
          if(params.points[name]){
            var px = params.points[name].x;
            var py = params.points[name].y;
            if(px > $scope.domain){px = $scope.domain; }
            else if(px < (0 - $scope.domain)){px = 0 - $scope.domain; }
            if(py > $scope.range) {py = $scope.range; }
            else if(py < (0 - $scope.range)) {py = 0 - $scope.range; }
            if($scope.sigfigs > -1) {
              var multiplier = Math.pow(10,$scope.sigfigs);
              px = Math.round(px*multiplier) / multiplier;
              py = Math.round(py*multiplier) / multiplier;
            }
            $scope.points[name] = {x: px, y: py, isSet:true};
          }
        }
        if(params.points){

          setPoint('A');
          setPoint('B');

          //if both points are created, draw line and set response
          if(params.points.A && params.points.B){
            $scope.graphCoords = [params.points.A.x+","+params.points.A.y, params.points.B.x+","+params.points.B.y];
            var slope = (params.points.A.y - params.points.B.y) / (params.points.A.x - params.points.B.x);
            var yintercept = params.points.A.y - (params.points.A.x * slope);
            $scope.equation = "y="+slope+"x+"+yintercept;
            $scope.graphCallback({graphStyle:{},drawShape:{line: ["A","B"]}});
          }else{
            $scope.graphCoords = null;
          }
          if(!$scope.locked && $scope.controller){
            $scope.controller.setResponse($scope.responseIdentifier, $scope.graphCoords);
          }
        }
      }
      $scope.$watch('points', function(points){
        function checkCoords(coords){
          return coords && !isNaN(coords.x) && !isNaN(coords.y);
        }
        var graphPoints = {};
        _.each(points,function(coords,ptName){
          if(checkCoords(coords)) graphPoints[ptName] = coords;
        });
        if($scope.graphCallback){
          $scope.graphCallback({points: graphPoints});
        }
      }, true)
      $scope.undo = function(){
        if(!$scope.locked && $scope.points.B && $scope.points.B.isSet){
          $scope.points.B = {}
        } else if(!$scope.locked && $scope.points.A && $scope.points.A.isSet){
          $scope.points.A = {}
        }
      }
      $scope.startOver = function(){
        if(!$scope.locked){
          $scope.points.B = {}
          $scope.points.A = {}
        }
      }
      function lockGraph(){
        $scope.locked = true;
        $scope.graphCallback({lockGraph: true});  
      }
      $scope.$on('controlBarChanged', function(){
        if($scope.settingsHaveChanged){
          $scope.graphCallback({clearBoard: true});
          setDefaultInputStyles($scope);
          $scope.correctAnswerBody = "clear";
          $scope.points = {A: {x: undefined, y: undefined, isSet:false}, B: {x: undefined, y: undefined, isSet:false}};
          $scope.locked = false;
        }
      })
      $scope.$on("highlightUserResponses", function(){
        if(!$scope.itemSession.isFinished && $scope.itemSession.responses){
           var response = _.find($scope.itemSession.responses,function(r){
                return r.id === $scope.responseIdentifier;
           });
           if(response){
               var A = response.value[0].split(",")
               var B = response.value[1].split(",")
               $scope.points.A = {x: A[0], y: A[1]}
               $scope.points.B = {x: B[0], y: B[1]}
           }
        }
      })
      $scope.$on("formSubmitted",function(){
        if(!$scope.locked){
          $scope.submissions++;
          var response = _.find($scope.itemSession.responses,function(r){
            return r.id === $scope.responseIdentifier;
          });
          if($scope.itemSession.settings.highlightUserResponse){
            if(response && response.outcome.isCorrect){
              $scope.graphCallback({graphStyle: {borderColor: "green", borderWidth: "2px"}, pointsStyle: "green", shapesStyle: "green"})
              $scope.inputStyle = _.extend($scope.inputStyle, {border: 'thin solid green'})
            } else {
              $scope.graphCallback({graphStyle: {borderColor: "red", borderWidth: "2px"}, pointsStyle: "red", shapesStyle: "red"})
              $scope.inputStyle = _.extend($scope.inputStyle, {border: 'thin solid red'})
            }
          }
          var maxAttempts = $scope.itemSession.settings.maxNoOfAttempts?$scope.itemSession.settings.maxNoOfAttempts:1
          if($scope.submissions >= maxAttempts){
            lockGraph();
          }else if(maxAttempts == 0 && response && response.outcome.isCorrect){
            lockGraph();
          }
          if($scope.itemSession.settings.highlightCorrectResponse){
            var correctResponse = _.find($scope.itemSession.sessionData.correctResponses, function(cr){
              return cr.id == $scope.responseIdentifier;
            });
            if(correctResponse && correctResponse.value && !response.outcome.isCorrect){
              $scope.correctAnswerBody = [
                "<p>The equation is "+correctResponse.value+"</p>",
                "<lineInteraction domain='"+$scope.domain+"'",
                "range='"+$scope.range+"'",
                "scale='"+$scope.scale+"'",
                "domain-label='"+$scope.domainLabel+"'",
                "range-label='"+$scope.rangeLabel+"'",
                "tick-label-frequency='"+$scope.tickLabelFrequency+"'",
                "show-inputs=false",
                "locked=''",
                "graph-width=300",
                "graph-height=300",
                ">",
                "<graphcurve>"+correctResponse.value+"</graphcurve>",
                "</lineInteraction>"
              ].join("\n");
            }
          }
        }
      });
    }],
    compile: function(element, attrs, transclude){
      var graphAttrs = {
        "jsx-graph": "",
        "graph-callback": "graphCallback",
        "interaction-callback": "interactionCallback",
        domain: parseInt(attrs.domain?attrs.domain:10),
        range: parseInt(attrs.range?attrs.range:10),
        scale: parseFloat(attrs.scale?attrs.scale:1),
        domainLabel: attrs.domainLabel,
        rangeLabel: attrs.rangeLabel,
        tickLabelFrequency: attrs.tickLabelFrequency,
        showLabels: attrs.showLabels?attrs.showLabels:"true",
        maxPoints:2
      };
      return function(scope, element, attrs, AssessmentItemController){
        function setScopeFromAttrs(){
          scope.additionalText = attrs.additionalText;
          scope.scale = graphAttrs.scale;
          scope.domain = graphAttrs.domain;
          scope.range = graphAttrs.range;
          scope.sigfigs = parseInt(attrs.sigfigs?attrs.sigfigs:-1);
          scope.showInputs = !attrs.showInputs || attrs.showInputs === "true";
          scope.responseIdentifier = attrs.responseidentifier;
          scope.locked = attrs.hasOwnProperty('locked')?true:false;
          scope.domainLabel = graphAttrs.domainLabel
          scope.rangeLabel = graphAttrs.rangeLabel
          scope.tickLabelFrequency = attrs.tickLabelFrequency
          setDefaultInputStyles(scope);
        }
        setScopeFromAttrs();
        var containerWidth, containerHeight;
        var graphContainer = element.find('.graph-container')
        if(attrs.graphWidth && attrs.graphHeight){
          containerWidth = parseInt(attrs.graphWidth)
          containerHeight = parseInt(attrs.graphHeight)
        } else {
          containerHeight = containerWidth = graphContainer.width()
        }
        graphContainer.attr(graphAttrs);
        graphContainer.css({width: containerWidth, height: containerHeight});
        $compile(graphContainer)(scope);
        if(scope.showInputs){
          //refresh periodically
          setInterval(function(){
            scope.$digest();
          }, 500)
        }
        scope.controller = AssessmentItemController;
        if(scope.controller) scope.controller.registerInteraction(element.attr('responseIdentifier'), "line graph", "graph");
        if(!scope.locked && scope.controller){
          scope.controller.setResponse(scope.responseIdentifier,null);
          element.find(".graph-interaction").append("<correctanswer class='correct-answer' correct-answer-body='correctAnswerBody' responseIdentifier={{responseIdentifier}}>See the correct answer</correctanswer>")
          $compile(element.find("correctanswer"))(scope)
        }
      }
    }
  }
}]);