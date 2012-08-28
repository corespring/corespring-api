
function HomeController($scope, $timeout, $http, $location, AccessToken, ItemService ) {
    $http.defaults.headers.get = ($http.defaults.headers.get || {});
    $http.defaults.headers.get['Content-Type'] = 'application/json';

    $scope.$root.mode = "home";

    $scope.accessToken = AccessToken;

    $scope.loadItems = function(){
       $scope.items = ItemService.query({ access_token: $scope.accessToken.token});
    };

    /*
     * called from the repeater. scope (this) is the current item
     */
    $scope.openEditView = function () {
        $location.url('/view/' + this.item.id);
    };

    $scope.$watch('accessToken.token', function(newValue, oldValue){
       if(newValue){
           $timeout(function(){
               $scope.loadItems();
           });
       }
    });
}

HomeController.$inject = ['$scope', '$timeout', '$http', '$location', 'AccessToken', 'ItemService'];

