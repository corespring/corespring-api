function SearchController($scope, $rootScope, $http, ItemService, SearchService, Collection) {
  //$http.defaults.headers.get = ($http.defaults.headers.get || {});
  //$http.defaults.headers.get['Content-Type'] = 'application/json';

  $scope.searchParams = $rootScope.searchParams ? $rootScope.searchParams : ItemService.createWorkflowObject();

  var init = function(){
    $scope.search();
    loadCollections();
  };

  $scope.search = function() {
    $rootScope.$broadcast("beginSearch");
    SearchService.search($scope.searchParams, function(res){
      $rootScope.items = res;
    });

    SearchService.count($scope.searchParams, function(count){

      $rootScope.itemCount = count;
    });
  };

  $scope.loadMore = function () {
    SearchService.loadMore(function () {
        // re-bind the scope collection to the services model after result comes back
        $rootScope.items = SearchService.itemDataCollection;
      }
    );
  };

  function loadCollections() {
    Collection.get({}, function (data) {
        $scope.collections = data;
      },
      function () {
        console.log("load collections: error: " + arguments);
      });
  }
  init();
}

SearchController.$inject = ['$scope',
  '$rootScope',
  '$http',
  'ItemService',
  'SearchService',
  'Collection'];
