angular.module("qti.directives").directive("lineinteraction", function(){
    return {
        template: [
        "<div class='graph-interaction'>",
        "   <div jsx-graph board-params='boardParams' points='points' max-points='2' scale='.5' lock-points='lockGraphPoints'></div>",
        "   <div class='points-display'>",
        "       <div class='point-display'>",
        "           <p>Point A:</p>",
        "           <p>x: </p>",
        "           <input type='text' style='width: 80px;', ng-model='points.A.x' ng-disabled='inputDisabled'>",
        "           <p>y: </p>",
        "           <input type='text' style='width: 80px;' ng-model='points.A.y' ng-disabled='inputDisabled'>",
        "       </div>",
        "       <div class='point-display'>",
        "           <p>Point B:</p>",
        "           <p>x: </p>",
        "           <input type='text' style='width: 80px;', ng-model='points.B.x' ng-disabled='inputDisabled'>",
        "           <p>y: </p>",
        "           <input type='text' style='width: 80px;' ng-model='points.B.y' ng-disabled='inputDisabled'>",
        "       </div>",
        "   </div>",
        "</div>"].join("\n"),
        restrict: 'E',
        scope: true,
        require: '^assessmentitem',
        controller: ['$scope', function($scope){
            $scope.boardParams = {
                domain: 10,
                range: 10
            }
            $scope.equation = "y = mx + b";
            $scope.points = {A: {x: undefined, y: undefined}, B: {x: undefined, y: undefined}}
            $scope.$watch('points', function(points){
                function checkCoords(coords){
                    return coords && !isNaN(coords.x) && !isNaN(coords.y)
                }
                if(checkCoords($scope.points.A) && checkCoords($scope.points.B)){
                    var slope = ($scope.points.A.y - $scope.points.B.y) / ($scope.points.A.x - $scope.points.B.x)
                    var yintercept = $scope.points.A.y - ($scope.points.A.x * slope)
                    $scope.equation = "y = "+slope+"x + "+yintercept;
                    $scope.controller.setResponse($scope.responseIdentifier, "y="+slope+"x+"+yintercept);
                }else $scope.equation = "y = mx + b"
            }, true)
            $scope.$on("formSubmitted",function(){
                console.log("form submitted")
                $scope.lockGraphPoints(true);
                $scope.inputDisabled = true;
            });
            //refresh periodically
            setInterval(function(){
                $scope.$digest()
            }, 500)
        }],
        compile: function(element, attrs, transclude){
            element.find('[jsx-graph]').css({width: attrs.graphWidth, height: attrs.graphHeight})
            return function(scope, element, attrs, AssessmentItemController){
                scope.responseIdentifier = attrs.responseidentifier;
                scope.controller = AssessmentItemController
                //scope.controller.registerInteraction(element.attr('responseIdentifier'), "line graph", "graph")
            }
        }
    }
})