angular.module("qti.directives").directive("lineinteraction", function(){
    return {
        template: [
        "<div class='graph-interaction'>",
        "   <div jsx-graph points='points' submission-callback='graphCallback'></div>",
        "   <div class='points-display'>",
        "       <div class='point-display'>",
        "           <p>Point A:</p>",
        "           <p>x: </p>",
        "           <input type='text' style='width: 80px;', ng-model='points.A.x' ng-disabled='outcomeReturned'>",
        "           <p>y: </p>",
        "           <input type='text' style='width: 80px;' ng-model='points.A.y'  ng-disabled='outcomeReturned'>",
        "       </div>",
        "       <div class='point-display'>",
        "           <p>Point B:</p>",
        "           <p>x: </p>",
        "           <input type='text' style='width: 80px;', ng-model='points.B.x' ng-disabled='outcomeReturned'>",
        "           <p>y: </p>",
        "           <input type='text' style='width: 80px;' ng-model='points.B.y' ng-disabled='outcomeReturned'>",
        "       </div>",
        "   </div>",
        "</div>"].join("\n"),
        restrict: 'E',
        scope: true,
        require: '^assessmentitem',
        controller: ['$scope', function($scope){
            $scope.points = {A: {x: undefined, y: undefined}, B: {x: undefined, y: undefined}}
            $scope.$watch('showNoResponseFeedback', function(){
                 if($scope.isEmptyItem($scope.equation) && $scope.showNoResponseFeedback){
                    $scope.graphCallback({isIncomplete:true})
                 }
            });
            $scope.$watch('points', function(points){
                function checkCoords(coords){
                    return coords && !isNaN(coords.x) && !isNaN(coords.y)
                }
                if(checkCoords($scope.points.A) && checkCoords($scope.points.B)){
                    var slope = ($scope.points.A.y - $scope.points.B.y) / ($scope.points.A.x - $scope.points.B.x)
                    var yintercept = $scope.points.A.y - ($scope.points.A.x * slope)
                    $scope.equation = "y="+slope+"x+"+yintercept
                    $scope.graphCallback({clearBorder: true})
                }else $scope.equation = null
                $scope.controller.setResponse($scope.responseIdentifier, $scope.equation);
            }, true)
            $scope.$on("formSubmitted",function(){
                $scope.outcomeReturned = true
                var response = _.find($scope.itemSession.responses,function(r){
                    return r.id === $scope.responseIdentifier
                })
                $scope.graphCallback({isCorrect: response && response.outcome.isCorrect, lockGraph: true});
            });

            //refresh periodically
            setInterval(function(){
                $scope.$digest()
            }, 500)
        }],
        compile: function(element, attrs, transclude){
            var domain = parseInt(attrs.domain?attrs.domain:10);
            var range = parseInt(attrs.range?attrs.range:10);
            var scale = parseFloat(attrs.scale?attrs.scale:1)
            var width = attrs.graphWidth?attrs.graphWidth:"300px"
            var height = attrs.graphHeight?attrs.graphHeight:"300px"
            element.find('[jsx-graph]').css({width: width, height: height})
            element.find('[jsx-graph]').attr({domain: domain, range: range, scale: scale})
            return function(scope, element, attrs, AssessmentItemController){
                scope.scale = scale;
                scope.responseIdentifier = attrs.responseidentifier;
                scope.controller = AssessmentItemController
                scope.controller.registerInteraction(element.attr('responseIdentifier'), "line graph", "graph")
            }
        }
    }
})