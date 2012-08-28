
function HomeController($scope, $timeout, $http, AccessToken, ItemService ) {
    $http.defaults.headers.get = ($http.defaults.headers.get || {});
    $http.defaults.headers.get['Content-Type'] = 'application/json';

    $scope.$root.mode = "home";

    $scope.accessToken = AccessToken;

    $scope.loadItems = function(){
       $scope.items = ItemService.query({ access_token: $scope.accessToken.token});
    };

    $scope.$watch('accessToken.token', function(newValue, oldValue){
       console.log("new token:" + newValue + ", " + oldValue );

       if(newValue){
           $timeout(function(){
               $scope.loadItems();
           });
       }
    });
}

HomeController.$inject = ['$scope', '$timeout', '$http', 'AccessToken', 'ItemService'];

