//app.js
var app = angular.module('corespring-dev', ['cs','ngResource']);

app.controller('MainController', function($scope) {
  $scope.name = 'World';
});

angular.module('corespring-dev').factory("Developer", ['$resource',function($resource){
    return $resource(
        '/developer/:func',
        {},
        {
            isLoggedIn : {method : 'GET', params : {func : 'isauth'}, isArray : false},
            getOrg : {method : 'GET', params : {func : 'org'}, isArray : false}
        }
    );
}]);
function DeveloperCtrl($scope,Developer) {
    //noauth = not logged in
    //noorg = logged in but not registered with an organization
    //registered= logged in and registered with an organization

    $scope.authState = "noauth";
    $scope.org = {name : ""};
    (function(){
        Developer.isLoggedIn({},function(data) {
            console.log(JSON.stringify(data))
            if(data.isLoggedIn == true){
                $scope.authState = "noorg";
                Developer.getOrg({},function(data){
                    console.log(data)
                    $scope.org = data;
                    $scope.authState = "registered"
                }) ;
            } else $scope.authState = "noauth";
        });
    })();
}
DeveloperCtrl.$inject = ['$scope','Developer'];