var app = angular.module('app', ['ngResource']);

app.controller('TestApi', function($scope, $resource, $http) {
    // this access_token is part of the fixture data
    var access_token = '34dj45a769j4e1c0h4wb';

    var baseApiUrl = 'http://localhost:9000/api/v1';
    if ($scope.baseApiUrl) {
        baseApiUrl = $scope.baseApiUrl;
    }

    // for testing
    $scope.url = '/items/5001b7ade4b0d7c9ec321070/sessions?access_token=34dj45a769j4e1c0h4wb';

    $scope.method = 'GET';
    $scope.methods = ['GET', 'POST', 'PUT', 'DELETE'];

    $scope.submit = function() {
        // submit the api call and populate the response
        if ($scope.method == 'GET') {
            // handle get call
            $http({
                method: 'GET',
                url: baseApiUrl + $scope.url
            }).
                success(function(data, status, headers, config) {
                    $scope.responseBody = data.toString();
                    $scope.responseStatus = status;
                }).
                error(function(data, status, headers, config) {
                    $scope.responseBody = data.toString();
                    $scope.responseStatus = status;
                });

        }
        if ($scope.method == 'POST' || $scope.method == 'PUT') {
            $http({
                method: 'POST',
                url: baseApiUrl + $scope.url,
                data: $scope.body
            }).
                success(function(data, status, headers, config) {
                    $scope.responseBody = data;
                    $scope.responseStatus = status;
                }).
                error(function(data, status, headers, config) {
                    $scope.responseBody = data;
                    $scope.responseStatus = status;
                });
        }



    }

});
