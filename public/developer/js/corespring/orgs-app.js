angular.module("services", ["ngResource"]);
angular.module("dev-orgs-app", ['services']);

angular.module("services")
  .factory("Organizations", [ '$resource', function($resource){

    return $resource(
     '/developer/orgs',
      {},
      { get: {method: 'GET', isArray: true}}
    );

}]);



function DeveloperOrgsController($scope, Organizations){
  var populate = function() {
      Organizations.get(function(data){
        console.log("Orgs: " + JSON.stringify(data));
        $scope.orgs = data;
      })
  };
  populate();

  $scope.addOrg = function(orgName) {
    console.log("adding organization "+orgName);
    var org = {name : orgName};
    Organizations.save({},org, function(data){
        console.log(JSON.stringify(data));
        populate();
    });
  }
}

DeveloperOrgsController.$inject = [
  '$scope', 'Organizations'
];
