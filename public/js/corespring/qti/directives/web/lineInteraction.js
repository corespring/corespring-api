angular.module("qti.directives").directive("lineinteraction", function(){
    return {
        template: [
        "<div class='graph-interaction'>",
        "   <div jsx-graph points='points' lock-points='lockGraphPoints'></div>",
        "   <div class='points-display'>",
        "       <div class='point-display'>",
        "           <p>Point A:</p>",
        "           <p>x: </p>",
        "           <input type='text' style='width: 80px;', ng-model='points.A.x' ng-required='pointsRequired' ng-disabled='inputDisabled'>",
        "           <p>y: </p>",
        "           <input type='text' style='width: 80px;' ng-model='points.A.y' ng-required='pointsRequired' ng-disabled='inputDisabled'>",
        "       </div>",
        "       <div class='point-display'>",
        "           <p>Point B:</p>",
        "           <p>x: </p>",
        "           <input type='text' style='width: 80px;', ng-model='points.B.x' ng-required='pointsRequired' ng-disabled='inputDisabled'>",
        "           <p>y: </p>",
        "           <input type='text' style='width: 80px;' ng-model='points.B.y' ng-required='pointsRequired' ng-disabled='inputDisabled'>",
        "       </div>",
        "   </div>",
        "</div>"].join("\n"),
        restrict: 'E',
        scope: true,
        require: '^assessmentitem',
        controller: ['$scope', function($scope){
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
            $scope.pointsRequired = function(){
                return false;
            }
            //refresh periodically
            setInterval(function(){
                $scope.$digest()
            }, 500)
        }],
        compile: function(element, attrs, transclude){
            var domain = parseInt(attrs.domain?attrs.domain:10);
            var range = parseInt(attrs.range?attrs.range:10);
            var scale = parseFloat(attrs.scale?attrs.scale:1)
            element.find('[jsx-graph]').css({width: attrs.graphWidth, height: attrs.graphHeight})
            element.find('[jsx-graph]').attr({domain: domain, range: range, scale: scale})
            return function(scope, element, attrs, AssessmentItemController){
                scope.scale = scale;
                scope.responseIdentifier = attrs.responseidentifier;
                scope.controller = AssessmentItemController
                //scope.controller.registerInteraction(element.attr('responseIdentifier'), "line graph", "graph")
            }
        }
    }
})