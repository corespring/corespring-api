'use strict';

angular.module("qti.directives").directive("pointinteraction", function(){
    return {
        template: [
        "<div class='graph-interaction'>",
        "   <div class='additional-text' ng-show='additionalText'>",
        "       <p>{{additionalText}}</p>",
        "   </div>",
        "   <div class='graph-container' jsx-graph graph-callback='graphCallback' interaction-callback='interactionCallback'></div>",
        "</div>"].join("\n"),
        restrict: 'E',
        scope: true,
        require: '^assessmentitem',
        controller: ['$scope', '$document', function($scope, $document){
            $scope.yintercept = {x: undefined, y: undefined};
            $scope.$watch('showNoResponseFeedback', function(){
                 if($scope.isEmptyItem($scope.pointResponse) && $scope.showNoResponseFeedback){
                    $scope.graphCallback({submission: {isIncomplete:true}});
                 }
            });
            $scope.$watch('graphCallback',function(){
                if($scope.graphCallback){
                    $scope.graphCallback({initBoard: true})
                }
            })
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
                    $scope.graphCallback({submission:{clearBorder: true}});
                    $scope.controller.setResponse($scope.responseIdentifier, $scope.pointResponse);
                } else{
                    $scope.pointResponse = null;
                }
            }
            $scope.$on("formSubmitted",function(){
                if(!$scope.locked){
                   $scope.outcomeReturned = true;
                   var response = _.find($scope.itemSession.responses,function(r){
                       return r.id === $scope.responseIdentifier;
                   });
                   $scope.graphCallback({submission: {isCorrect: response && response.outcome.isCorrect, lockGraph: true}});
                }
            });

            //refresh periodically
            setInterval(function(){
                $scope.$digest();
            }, 500)
        }],
        compile: function(element, attrs, transclude){
            var domain = parseInt(attrs.domain?attrs.domain:10);
            var range = parseInt(attrs.range?attrs.range:10);
            var graphAttrs = {
                                 domain: domain,
                                 range: range,
                                 scale: parseFloat(attrs.scale?attrs.scale:1),
                                 domainLabel: attrs.domainLabel,
                                 rangeLabel: attrs.rangeLabel,
                                 tickLabelFrequency: attrs.tickLabelFrequency,
                                 pointLabels: attrs.pointLabels,
                                 maxPoints: attrs.maxPoints
                             };
            element.find('[jsx-graph]').attr(graphAttrs);
            return function(scope, element, attrs, AssessmentItemController){
                var jsxgraphElem = element.find('[jsx-graph]');
                jsxgraphElem.css({height: jsxgraphElem.width()});
                scope.additionalText = attrs.additionalText
                scope.scale = graphAttrs.scale;
                scope.domain = domain;
                scope.range = range;
                scope.sigfigs = parseInt(attrs.sigfigs?attrs.sigfigs:-1);
                scope.responseIdentifier = attrs.responseidentifier;
                scope.controller = AssessmentItemController;
                scope.controller.registerInteraction(element.attr('responseIdentifier'), "line graph", "graph");
                scope.controller.setResponse(scope.responseIdentifier,null);
                scope.outcomeReturned = scope.locked = attrs.hasOwnProperty('locked')?true:false;
            }
        }
    }
});