function CreateCollection($scope, $rootScope, CollectionManager, UserInfo) {

  $scope.orgName = UserInfo.org.name;

  $scope.newAlert = function (cssclass, message) {
    $scope.alertClass = cssclass;
    $scope.alertMessage = message;
  };

  $scope.newAlert("alert", "");
  //this looks kind of weird. this is all for opening and closing the collections modal correctly between MainNavController and this controller

  $rootScope.$watch('collectionsWindowRoot', function () {
    $scope.collectionsWindow = $rootScope.collectionsWindowRoot;
  });

  $scope.closeCollectionWindow = function () {
    $scope.collectionsWindow = false;
  };

  $scope.$watch('collectionsWindow', function () {
    if (!$scope.collectionsWindow && $rootScope.collectionsWindowRoot) $rootScope.collectionsWindowRoot = false;
  });

  $scope.createCollection = function (collectionName) {
    if (collectionName) {

      var onSuccess = function (data) {
        $('#newcollection').val('');
        $scope.newAlert('alert alert-success', "Successfully created collection");
      };

      var onError = function (err) {
        $scope.newAlert('alert alert-error', "Error occurred when creating a collection");
        console.log("create collection: error: " + err);
      };

      CollectionManager.addCollection(collectionName, onSuccess, onError);

    }
  };

  $scope.deleteCollection = function (id) {

    var onSuccess = function () {
      $scope.newAlert('alert alert-success', "Successfully deleted collection");
    };

    var onError = function (err) {
      $scope.newAlert('alert alert-error', "Error deleting collection");
      console.log("error deleting collection: " + err);
    };

    CollectionManager.removeCollection(id, onSuccess, onError);
  };

  $scope.$watch(
    function () {
      return CollectionManager.sortedCollections;
    },
    function (newValue) {
      if (newValue) {
        $scope.collections = newValue[0].collections;
      }
    }, true);
}
CreateCollection.$inject = ['$scope', '$rootScope', 'CollectionManager', 'UserInfo'];
