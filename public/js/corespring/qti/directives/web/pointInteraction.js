'use strict';

angular.module("qti.directives").directive("graphpoint",function(){
    return {
        restrict: "E",
        require: '^pointinteraction',
        scope: {},
        compile: function(element,attrs,transclude){
            element.attr('hidden','');
            var locked = element.parent()[0].attributes.getNamedItem('locked')?true:false;
            return function(scope,element,attrs,PointCtrl){
                var coords = element[0].innerHTML.split(",");
                if(coords.length == 2){
                    var point = {x: coords[0], y: coords[1]};
                    if(attrs.color) point = _.extend(point,{color: attrs.color})
                    var points = []
                    if(PointCtrl.getInitialParams() && PointCtrl.getInitialParams().points){
                        points = PointCtrl.getInitialParams().points
                    }
                    points.push(point)
                    PointCtrl.setInitialParams({ points: points })
                } else {
                    throw "each point must contain x and y coordinate separated by a comma";
                }
            };
        }
    }
})
angular.module("qti.directives").directive("pointinteraction", ['$compile', function($compile){
    return {
        template: [
        "<div class='graph-interaction'>",
        "   <div class='additional-text' ng-show='additionalText'>",
        "       <p>{{additionalText}}</p>",
        "   </div>",
        "   <div class='graph-container'></div>",
        "   <div id='initialParams' ng-transclude></div>",
        "</div>"].join("\n"),
        restrict: 'E',
        transclude: true,
        scope: true,
        require: '?^assessmentitem',
        controller: ['$scope', function($scope){
            $scope.submissions = 0
            this.setInitialParams = function(initialParams){
                $scope.initialParams = initialParams;
            };
            this.getInitialParams = function(){return $scope.initialParams;}
            $scope.$watch('graphCallback',function(){
                if($scope.graphCallback){
                    if($scope.initialParams){
                       $scope.graphCallback($scope.initialParams);
                    }
                }
            })
            $scope.$watch('showNoResponseFeedback', function(){
                 if(!$scope.locked && $scope.isEmptyItem($scope.graphCoords) && $scope.showNoResponseFeedback){
                    $scope.graphCallback({graphStyle: {borderColor: "yellow", borderWidth: "2px"}});
                 }
            });
            $scope.interactionCallback = function(params){
                function round(coord){
                     var px = coord.x;
                     var py = coord.y;
                     if(px > $scope.domain){px = $scope.domain; }
                     else if(px < (0 - $scope.domain)){px = 0 - $scope.domain; }
                     if(py > $scope.range) {py = $scope.range; }
                     else if(py < (0 - $scope.range)) {py = 0 - $scope.range; }
                     if($scope.sigfigs > -1) {
                         var multiplier = Math.pow(10,$scope.sigfigs);
                         px = Math.round(px*multiplier) / multiplier;
                         py = Math.round(py*multiplier) / multiplier;
                     }
                     return {x: px,y: py};
                }
                if(params.points){
                    $scope.pointResponse = _.map(params.points,function(coord){
                        var newCoord = round(coord);
                        return newCoord.x+","+newCoord.y;
                    });
                    $scope.graphCallback({graphStyle: {}});
                    $scope.controller.setResponse($scope.responseIdentifier, $scope.pointResponse);
                } else{
                    $scope.pointResponse = null;
                }
            }
            $scope.$on("formSubmitted",function(){
               if(!$scope.locked){
                   $scope.submissions++;
                   var response = _.find($scope.itemSession.responses,function(r){
                       return r.id === $scope.responseIdentifier;
                   });
                   if($scope.itemSession.settings.highlightUserResponse){
                        if(response && response.outcome.isCorrect){
                            $scope.graphCallback({graphStyle: {borderColor: "green", borderWidth: "2px"}, pointsStyle: "green"})
                        } else {
                            $scope.graphCallback({graphStyle: {borderColor: "red", borderWidth: "2px"}, pointsStyle: "red"})
                        }
                   }
                   var maxAttempts = $scope.itemSession.settings.maxNoOfAttempts?$scope.itemSession.settings.maxNoOfAttempts:1
                   if($scope.submissions >= maxAttempts){
                        $scope.locked = true;
                        $scope.graphCallback({lockGraph: true});
                   }
                   if($scope.itemSession.settings.highlightCorrectResponse){
                       var correctResponse = _.find($scope.itemSession.sessionData.correctResponses, function(cr){
                           return cr.id == $scope.responseIdentifier;
                       });
                       if(correctResponse && correctResponse.value && !response.outcome.isCorrect){
                           var startElem = [
                              "<pointInteraction responseIdentifier="+$scope.responseIdentifier,
                                "point-labels="+$scope.pointLabels,
                                "max-points="+$scope.maxPoints,
                                "locked=''",
                                "graph-width=300",
                                "graph-height=300",
                                ">"
                           ]
                           var body = _.map(correctResponse.value, function(value){
                                return "<graphpoint color='green'>"+value+"</graphpoint>"
                           })
                           var endElem = ["</pointInteraction>"]
                           $scope.correctAnswerBody = _.flatten([startElem, body, endElem]).join("\n");
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
                                 pointLabels: attrs.pointLabels,
                                 maxPoints: attrs.maxPoints
                             };
            return function(scope, element, attrs, AssessmentItemController){
                var containerWidth, containerHeight;
                var graphContainer = element.find('.graph-container')
                if(attrs.graphWidth && attrs.graphHeight){
                    containerWidth = parseInt(attrs.graphWidth)
                    containerHeight = parseInt(attrs.graphHeight)
                } else {
                    containerHeight = containerWidth = graphContainer.width()
                }
                graphContainer.attr(graphAttrs);
                graphContainer.css({width: Math.floor(containerWidth*.9), height: Math.floor(containerHeight*.9)});
                $compile(graphContainer)(scope);
                scope.additionalText = attrs.additionalText;
                scope.scale = graphAttrs.scale;
                scope.domain = graphAttrs.domain;
                scope.range = graphAttrs.range;
                scope.sigfigs = parseInt(attrs.sigfigs?attrs.sigfigs:-1);
                scope.responseIdentifier = attrs.responseidentifier;
                scope.controller = AssessmentItemController;
                if(scope.controller) scope.controller.registerInteraction(element.attr('responseIdentifier'), "line graph", "graph");
                scope.locked = attrs.hasOwnProperty('locked')?true:false;
                if(!scope.locked && scope.controller){
                    scope.controller.setResponse(scope.responseIdentifier,null);
                    element.find(".graph-interaction").append("<correctanswer class='correct-answer' correct-answer-body='correctAnswerBody' responseIdentifier={{responseIdentifier}}>See the correct answer</correctanswer>")
                    $compile(element.find("correctanswer"))(scope)
                }
                scope.domainLabel = graphAttrs.domainLabel
                scope.rangeLabel = graphAttrs.rangeLabel
                scope.tickLabelFrequency = attrs.tickLabelFrequency
                scope.pointLabels = graphAttrs.pointLabels
                scope.maxPoints = graphAttrs.maxPoints
            }
        }
    }
}]);