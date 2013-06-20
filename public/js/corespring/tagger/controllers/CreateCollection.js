function CreateCollection($scope, $rootScope, CollectionManager, UserInfo) {

  $scope.orgName = UserInfo.org.name;

  $scope.hideAlertMessage = function () {
    $scope.alertMessage = null;
    $scope.alertClass = null;
  };

  $scope.setAlertClassAndMessage = function (cssclass, message) {
    $scope.alertClass = 'alert-' + cssclass;
    $scope.alertMessage = message;
  };

  //$scope.setAlertClassAndMessage("alert", "");
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
        $scope.setAlertClassAndMessage('success', "Successfully created collection");
      };

      var onError = function (err) {
        $scope.setAlertClassAndMessage('error', "Error occurred when creating a collection");
      };

      CollectionManager.addCollection(collectionName, onSuccess, onError);

    }
  };

  $scope.paneClicked = function () {
    $scope.hideAlertMessage();
  };

  $scope.deleteCollection = function (id) {

    var onSuccess = function () {
      $scope.setAlertClassAndMessage('success', "Successfully deleted collection");
    };

    var onError = function (err) {
      $scope.setAlertClassAndMessage('error', "Error deleting collection");
      console.log("error deleting collection: " + err);
    };

    CollectionManager.removeCollection(id, onSuccess, onError);
  };

  /** Callback hook for the content-editable directive */
  $scope.onRenameCollection = function (change, collectionId, callback) {

    var onError = function () {
      $scope.setAlertClassAndMessage("error", "Error editing collection name");
      callback(false);
    };

    var onSuccess = function () {
      $scope.setAlertClassAndMessage("success", "Renamed collection to " + change);
      callback(true);
    };

    var onNoChange = function () {
      callback(true);
    };

    CollectionManager.renameCollection(collectionId, change, onSuccess, onError, onNoChange);
  }

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
