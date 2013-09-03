function OrganizationMetadataController($scope, MessageBridge, ExtendedData) {

  var getMetadata = function(key,sets){
     return _.find(sets, function(s){return s.metadataKey == key});
  };

  var updateMetadata = function(key, holder, newData){
    var current = getMetadata(key, holder);
    current.data = newData;
  };

  var getEditorUrl = function(key, sets){
    var metadataSet = getMetadata(key,sets);
    if(metadataSet && metadataSet.editorUrl){
      return metadataSet.editorUrl;
    } else {
      return "/metadata/unkown-editor-url?" + key
    }
  };

  $scope.$watch("selectedMetadata", function(k) {
    if (!k) return;

    if ($scope.itemMetadata)
      $scope.editUrl = getEditorUrl(k, $scope.itemMetadata);
  });

  MessageBridge.addMessageListener(function (message) {

    if (message && message.data && message.data.type == 'updateMetadata') {
      var update = message.data.message;

      var property = $scope.selectedMetadata;
      ExtendedData.update({id: $scope.itemData.id, property : property }, update, function onSuccess(response){
        var data = response[$scope.selectedMetadata];
        updateMetadata($scope.selectedMetadata, $scope.itemMetadata, data );
        MessageBridge.sendMessage("externalMetadataEditor", {type: "currentMetadata", message: data}, true);
      });
      return;
    }

    if (message && message.data && message.data.type == 'requestMetadata') {

      var current = getMetadata($scope.selectedMetadata, $scope.itemMetadata);

      if(current){
        MessageBridge.sendMessage("externalMetadataEditor", {type: "currentMetadata", message: current.data}, true);
      }
    }
  });

}

OrganizationMetadataController.$inject = ['$scope', 'MessageBridge', 'ExtendedData'];
