function DevController($scope){
    $scope.saveResponses = function(){
        console.log("!!! saveResponses!!");

        $scope.$emit("submitItem", {isAttempt:false});
    }

    $scope.$watch('itemSession', function(newSession){

        console.log("!! new session....", newSession);
    });
}

DevController.$inject = [ '$scope' ];