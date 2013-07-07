angular.module("qti.directives").directive("graphinteraction", function(){
    template: "<div jsx-graph board-params='boardParams' points='points' max-points='2' scale='1'></div>"
    replace:false,
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
            }else $scope.equation = "y = mx + b"
        }, true)
        //refresh periodically
        setInterval(function(){
            $scope.$digest()
        }, 500)
    }],
    link: function(scope, element, attrs, AssessmentItemController){
        scope.controller = AssessmentItemController

    }
})