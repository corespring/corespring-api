window.ltiChooser = (window.ltiChooser || {});

angular.module("lti-chooser", ['tagger.services','lti-services', 'ngResource', 'corespring-directives']);

angular.module("lti-chooser").config(['$routeProvider', function ($routeProvider) {
    $routeProvider.when('/main', {templateUrl:'/lti/chooser/partials/main', controller:MainController});
    $routeProvider.when('/browse', {templateUrl:'/lti/chooser/partials/browse', controller:BrowseController});
    $routeProvider.when('/view/:itemId', {templateUrl:'/lti/chooser/partials/view', controller: ltiChooser.ViewItemController});
    $routeProvider.otherwise({redirectTo:'/main'});
  }]);

angular.module("lti-services", ['ngResource'])
  .factory("LaunchConfigService", [ '$resource', function($resource){
    return $resource("/lti/launch-config/:id", {}, { save: { method: "PUT"}});
  }])

  .factory("LtiItemService", ['$resource', function($resource){
    return $resource("/api/v1/items/:id", {} );
  }]);


function LtiChooserController( $scope, $rootScope, $location ){

  $scope.returnToSearch = function(){
    $location.url("/browse");
  };

  $scope.showSearch = function(){
     return $location.url() == "/browse";
  };

  $scope.showPager = function(){
    return $location.url().indexOf("/view/") === 0;
  };

  $scope.loadItem = function(id){
    console.log("loadItem - id: " + id);
    $location.url("/view/" + id);
  };

  $scope.loadMore = function(){
    $rootScope.$broadcast('loadMoreSearchResults');
  };

  $scope.change = function(){
    $location.url("/browse");
  };

  $scope.getAssignRemoveLabel = function(){
    return $scope.isAssigned() ? "Remove" : "Assign";
  };

  $scope.assignOrRemove = function(){
    if($scope.isAssigned()){
      $scope.remove();
    }
    else{
      $scope.assign();
    }
  };

  $scope.isAssigned = function(){
    if(!$scope.config){
      return true;
    }

    if(!$scope.item){
      return true;
    }
    return $scope.config.itemId === $scope.item.id;
  };

  $scope.remove = function(){
    $scope.config.itemId = null;
    $rootScope.$broadcast('saveConfig', { redirect: false });
    $location.url("/browse");
  };

  $scope.assign = function(){
    $scope.config.itemId = $scope.item.id;
    $rootScope.$broadcast('saveConfig');
  };
}

LtiChooserController.$inject = ['$scope', '$rootScope', '$location'];


