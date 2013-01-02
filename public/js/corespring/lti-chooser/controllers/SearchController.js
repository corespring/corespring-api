function SearchController($scope, $rootScope, $http, ItemService, SearchService, Collection) {
  //$http.defaults.headers.get = ($http.defaults.headers.get || {});
  //$http.defaults.headers.get['Content-Type'] = 'application/json';

  $rootScope.searchParams = ($rootScope.searchParams || ItemService.createWorkflowObject() );

  var init = function(){
    var defaults = new com.corespring.model.Defaults();
    $scope.gradeLevelDataProvider = defaults.buildNgDataProvider("gradeLevels");
    loadCollections();
  };

  $scope.search = function() {

    function isEmpty(p){ return !p.gradeLevel && !p.searchText && !p.collection; }
    function shorterThan(s, count){ return !s || s.length < count; }

    if(isEmpty($scope.searchParams)){
      $rootScope.items = undefined;
      $rootScope.itemCount = 0;
      return;
    }

    //if( shorterThan($scope.searchParams.searchText, 3) ){
    //  return;
    //}

    $rootScope.$broadcast("beginSearch");
    SearchService.search($scope.searchParams, function onSuccess(res){
      $rootScope.items = res;
    },
    function onError(data){
      $rootScope.$broadcast("searchFailed");
      alert("there was an error searching");
    });
  };

  $rootScope.$on('onNetworkLoading', function(event,count){
    $rootScope.isSearching = true;
  });

  $rootScope.$on('onSearchCountComplete', function(event,count){
    $rootScope.itemCount = count;
    $rootScope.isSearching = false;
  });

  $rootScope.$on('loadMoreSearchResults', function(event){
    console.log("SearchController - on - loadMore");
    $scope.loadMore();
  });

  $scope.$on('loadMore', function(event){
    $scope.loadMore();
  });

  $scope.loadMore = function () {
    SearchService.loadMore(function () {
        // re-bind the scope collection to the services model after result comes back
        $rootScope.items = SearchService.itemDataCollection;

        $rootScope.isSearching = false;
      }
    );
  };


  $scope.getItemCountLabel = function(count){
    if(!count){
      return "";
    }
    return count + " results";
  };


  function loadCollections() {
    Collection.get({}, function (data) {
        $scope.collections = data;

        $scope.search();
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
