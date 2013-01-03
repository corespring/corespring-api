function SearchController($scope, $rootScope, $http, ItemService, SearchService, Collection) {

  $rootScope.searchParams = ($rootScope.searchParams || ItemService.createWorkflowObject() );
  $scope.unpreppedGradeLevel = [];

  var init = function(){
    var defaultsFactory = new com.corespring.model.Defaults();
    function processDefaults(defaults) {
     var validKeys = "KG,01,02,03,04,05,06,07,08".split(",");
     var highschool = "09,10,11,12,13".split(",");

     var out = [];
     var hs = { key: "HS", value: ""};
     for( var i = 0 ; i < defaults.length ; i++ ){
       var d = defaults[i];

       if(highschool.indexOf(d.key) != -1){
          if(!hs.value){
            hs.value = [];
          }
          hs.value.push(d);
       }
       if(validKeys.indexOf(d.key) != -1){
         out.push(d);
       }
     }
     out.push(hs);
     return out;
    }

    var defaults = defaultsFactory.buildNgDataProvider("gradeLevels");
    var dataProvider = processDefaults(defaults);
    $scope.gradeLevelDataProvider = dataProvider;
    loadCollections();
  };

  $scope.processGradeLevel = function(gradeLevels){
    var out = [];

    if(!gradeLevels){
      return out;
    }

    for(var i = 0 ; i < gradeLevels.length; i++ ){
      var gl = gradeLevels[i];
      if(gl.key == "HS"){
        out = out.concat( gl.value );
      } else {
        out.push(gl);
      }
    }
    return out;
  };

  $scope.search = function() {
    var params = angular.copy($scope.searchParams);
    params.gradeLevel = $scope.processGradeLevel(params.gradeLevel);

    function isEmpty(p){ return !p.gradeLevel && !p.searchText && !p.collection; }
    function shorterThan(s, count){ return !s || s.length < count; }

    if(isEmpty(params)){
      $rootScope.items = undefined;
      $rootScope.itemCount = 0;
      return;
    }

    $rootScope.$broadcast("beginSearch");
    SearchService.search(params, function onSuccess(res){
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
