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
            isLoggedIn : {method : 'GET', params : {func : 'isloggedin'}, isArray : false},
            getOrg : {method : 'GET', params : {func : 'org'}, isArray : false}
        }
    );
}]);
function DeveloperCtrl($scope,$http,$rootScope,Developer) {
    //noauth = not logged in
    //noorg = logged in but not registered with an organization
    //registered= logged in and registered with an organization

    $scope.authState = "noauth";
    $scope.org = {name : ""};
    $scope.username = "";
    (function(){
        Developer.isLoggedIn({},function(data) {
            if(data.isLoggedIn == true){
                $scope.authState = "noorg";
                $scope.username = data.username
                Developer.getOrg({},function(org){
                    $scope.org = org;
                    $scope.authState = "registered"
                    $http.post('/auth/register',{organization: org.id}).success(function(data,status,headers,config){
                        if(data.client_id && data.client_secret){
                            $rootScope.apiClient = {clientId: data.client_id, clientSecret: data.client_secret}
                            $rootScope.$broadcast('setApiClient')
                        }
                    });
                }) ;
            } else $scope.authState = "noauth";
        }, function(data) {
          $scope.authState = "noauth";
        });
    })();
}
DeveloperCtrl.$inject = ['$scope','$http','$rootScope','Developer'];