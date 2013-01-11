angular.module("services", ["ngResource"]);
angular.module("dev-orgs-app", ['services']);

angular.module("services")
  .factory("Organizations", [ '$resource', function($resource){

    return $resource(
     '/developer/org',
      {},
      { get: {method: 'GET', isArray: true}}
    );

}]);


//TODO: use $location.path('/developer/home') instead of $window.location = '/developer/home'
function DeveloperOrgsController($scope, $window, Organizations){
  $scope.addOrg = function(orgName) {
    var org = {name : orgName};
    Organizations.save({},org, function(data){
        $window.location = "/developer/home";
    });
  }
}

DeveloperOrgsController.$inject = [
  '$scope', '$window', 'Organizations'
];
