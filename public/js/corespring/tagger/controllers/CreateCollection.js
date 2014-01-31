function CreateCollection($scope, CollectionManager, UserInfo, Logger) {

  $scope.orgName = UserInfo.org.name;
  $scope.viewState = "shared";

  $scope.disableCollection = function(collectionId) {
    CollectionManager.disableCollection(collectionId, function onSuccess(success){}, function onError(error){});
  };

  $scope.enableCollection = function(collectionId) {
    CollectionManager.enableCollection(collectionId, function onSuccess(success){
      var test = success;
    },
    function onError(error){
      var test = error;
    });
  };

  $scope.hideAlertMessage = function () {
    $scope.alertMessage = null;
    $scope.alertClass = null;
  };

  $scope.setAlertClassAndMessage = function (cssclass, message) {
    $scope.alertClass = 'alert-' + cssclass;
    $scope.alertMessage = message;
    if(cssclass == 'error') Logger.error(message)
  };

  $scope.paneClicked = function () {
    $scope.hideAlertMessage();
  };

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


  $scope.deleteCollection = function (id) {
    var onSuccess = function () {
      $scope.setAlertClassAndMessage('success', "Successfully deleted collection");
    };

    var onError = function (err) {
      $scope.setAlertClassAndMessage('error', "Error deleting collection");
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
        $scope.sharedCollections = newValue[1].collections;
      }
    }, true);
}
CreateCollection.$inject = ['$scope', 'CollectionManager', 'UserInfo', 'Logger'];
