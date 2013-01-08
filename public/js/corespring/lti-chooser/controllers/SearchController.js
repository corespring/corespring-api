function SearchController($scope, $rootScope, $http, ItemService, SearchService, Collection) {

  $rootScope.searchParams = ($rootScope.searchParams || ItemService.createWorkflowObject() );
  $scope.unpreppedGradeLevel = [];

  var init = function(){
    var defaultsFactory = new com.corespring.model.Defaults();

    /**
     * Only allow KG->8 and add HS for 09->13
     */
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
    return $scope.processItems("key", "HS", gradeLevels);
  };

  $scope.processCollections = function(collection){
    if(!collection || collection.length == 0){
      collection = _.filter($scope.collections, function(c){ return c.name == "All"});
    }
    return $scope.processItems("name", "All", collection);
  };

  $scope.processItems = function( prop, value, items, returnProp){
    function mapFn(i){
      if(i[prop] == value){
        return i.value;
      }
      return i;
    }
    return _.flatten(_.map(items, mapFn));
  };

  $scope.$on('search', function(){ $scope.search(); });

  $scope.search = function() {
    var params = angular.copy($scope.searchParams);
    params.gradeLevel = $scope.processGradeLevel(params.gradeLevel);
    params.collection = $scope.processCollections(params.collection);

    if(params.collection.length == 0){
      return;
    }

    function arrayIsEmpty(arr){ return (!arr || arr.length == 0); }
    function isEmpty(p){ return arrayIsEmpty(p.gradeLevel) && !p.searchText && arrayIsEmpty(p.collection); }
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

  $scope.clear = function(){
    $rootScope.searchParams = {};
    $rootScope.items = null;
    $rootScope.itemCount = null;
    SearchService.resetDataCollection();
    $rootScope.$broadcast("beginSearch");
  };

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
    if(!count && count !== 0){
      return "";
    }

    var resultString = (count == 1) ? "result" : "results";
    return count + " " + resultString;
  };


  function loadCollections() {
    Collection.get({}, function (data) {

        /**
         * For now restrict the filters to corespring Mathematics + Corespring ELA
         */
        function preProcess(c){
          return _.filter(c, function(i){ 
            return i.name == "CoreSpring Mathematics"
             || i.name == "CoreSpring ELA"
          })
        }
        var filtered = preProcess(data);

        $scope.collections = [{ name: "All", value: filtered } ].concat(filtered);

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
