angular.module("services", ["ngResource"]);
angular.module("dev-orgs-app", ['services']);

angular.module("services")
  .factory("Organizations", [ '$resource', function($resource){

    return $resource(
     '/api/v1/organizations',
      {},
      { get: {method: 'GET', isArray: true}}
    );
}]);

function DeveloperOrgsController($scope, Organizations){
  $scope.message = "hi from the controller";
  console.log("DeveloperOrgsController");

  Organizations.get(function(data){

    console.log("Orgs: " + data);
    $scope.orgs = data;
  });
}

DeveloperOrgsController.$inject = [
  '$scope', 'Organizations'
];
