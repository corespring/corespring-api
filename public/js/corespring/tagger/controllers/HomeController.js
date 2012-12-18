function HomeController(
  $scope,
  $rootScope,
  $http,
  $location,
  ItemService,
  SearchService,
  Collection,
  ItemFormattingUtils) {

    //Mixin ItemFormattingUtils
    angular.extend($scope, ItemFormattingUtils);

    $http.defaults.headers.get = ($http.defaults.headers.get || {});
    $http.defaults.headers.get['Content-Type'] = 'application/json';

    $scope.$root.mode = "home";

    $scope.searchParams = $rootScope.searchParams ? $rootScope.searchParams : ItemService.createWorkflowObject();
    $rootScope.$broadcast('onListViewOpened');


    var init = function(){
        $scope.search();
        loadCollections();
    };

    $scope.search = function() {
        SearchService.search($scope.searchParams, function(res){
            $rootScope.items = res;
        });
    };

    $scope.loadMore = function () {
        SearchService.loadMore(function () {
                // re-bind the scope collection to the services model after result comes back
                $rootScope.items = SearchService.itemDataCollection;
                //Trigger MathJax
                setTimeout(function(){
                    MathJax.Hub.Queue(["Typeset",MathJax.Hub]);
                }, 200);

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


    $scope.showGradeLevel = function () {
        return $scope.createGradeLevelString(this.item.gradeLevel);
    };



    $scope.deleteItem = function(item) {
        $scope.itemToDelete = item;
        $scope.showConfirmDestroyModal = true;
    }

    $scope.deleteConfirmed = function(){
        var deletingId = $scope.itemToDelete.id;
        ItemService.remove({id: $scope.itemToDelete.id},
            function(result) {
                $scope.itemToDelete = null;
                $scope.search();
            }
        );
        $scope.itemToDelete = null;
        $scope.showConfirmDestroyModal = false;
    };

    $scope.deleteCancelled = function(){
        console.log("Item Delete Cancelled");
       $scope.itemToDelete = null;
       $scope.showConfirmDestroyModal = false;
    };


    /*
     * called from the repeater. scope (this) is the current item
     */
    $scope.openEditView = function () {
        SearchService.currentItem = this.item;
        $location.url('/edit/' + this.item.id + "?panel=metadata");
    };

    init();
}

HomeController.$inject = ['$scope',
    '$rootScope',
    '$http',
    '$location',
    'ItemService',
    'SearchService',
    'Collection',
    'ItemFormattingUtils'];

