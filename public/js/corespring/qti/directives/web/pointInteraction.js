angular.module("qti.directives").directive("pointinteraction", function(){
    return {
        template: [
        "<div class='graph-interaction'>",
        "   <div class='points-display'>",
        "       <div class='point-display'>",
        "           <p>y-intercept:</p>",
        "           <p>x: </p>",
        "           <input type='text' style='width: 80px;', ng-model='yintercept.x' ng-disabled='outcomeReturned'>",
        "           <p>y: </p>",
        "           <input type='text' style='width: 80px;' ng-model='yintercept.y'  ng-disabled='outcomeReturned'>",
        "       </div>",
        "   </div>",
        "   <div jsx-graph graph-callback='graphCallback' interaction-callback='interactionCallback' max-points='1'></div>",
        "</div>"].join("\n"),
        restrict: 'E',
        scope: true,
        require: '^assessmentitem',
        controller: ['$scope', function($scope){
            $scope.yintercept = {x: undefined, y: undefined}
            $scope.$watch('showNoResponseFeedback', function(){
                 if($scope.isEmptyItem($scope.pointResponse) && $scope.showNoResponseFeedback){
                    $scope.graphCallback({submission: {isIncomplete:true}})
                 }
            });
            $scope.interactionCallback = function(params){
                if(params.points.A){
                    $scope.yintercept = params.points.A
                    $scope.graphCallback({submission:{clearBorder: true}})
                    $scope.pointResponse = $scope.yintercept.x+","+$scope.yintercept.y
                    $scope.controller.setResponse($scope.responseIdentifier, $scope.pointResponse)
                } else $scope.pointResponse = null
            }
            $scope.$watch('yintercept', function(yintercept){
              function checkCoords(coords){
                  return coords && !isNaN(coords.x) && !isNaN(coords.y)
              }
              if($scope.graphCallback && checkCoords(yintercept)) $scope.graphCallback({points: {A: yintercept}})
            }, true)
            $scope.$on("formSubmitted",function(){
                if(!$scope.locked){
                   $scope.outcomeReturned = true
                   var response = _.find($scope.itemSession.responses,function(r){
                       return r.id === $scope.responseIdentifier
                   })
                   $scope.graphCallback({submission: {isCorrect: response && response.outcome.isCorrect, lockGraph: true}});
                }
            });

            //refresh periodically
            setInterval(function(){
                $scope.$digest()
            }, 500)
        }],
        compile: function(element, attrs, transclude){
            var width = attrs.graphWidth?attrs.graphWidth:"300px"
            var height = attrs.graphHeight?attrs.graphHeight:"300px"
            var graphAttrs = {
                                 domain: parseInt(attrs.domain?attrs.domain:10),
                                 range: parseInt(attrs.range?attrs.range:10),
                                 scale: parseFloat(attrs.scale?attrs.scale:1),
                                 domainLabel: attrs.domainLabel,
                                 rangeLabel: attrs.rangeLabel,
                                 tickLabelFrequency: attrs.tickLabelFrequency,
                                 pointLabels: attrs.pointLabels
                             }
            element.find('[jsx-graph]').css({width: width, height: height})
            element.find('[jsx-graph]').attr(graphAttrs)
            //element.find("#initialParams").remove()
            return function(scope, element, attrs, AssessmentItemController){
                scope.scale = graphAttrs.scale;
                scope.responseIdentifier = attrs.responseidentifier;
                scope.controller = AssessmentItemController
                scope.controller.registerInteraction(element.attr('responseIdentifier'), "line graph", "graph")
                scope.controller.setResponse(scope.responseIdentifier,null)
                scope.outcomeReturned = scope.locked = attrs.hasOwnProperty('locked')?true:false
            }
        }
    }
})