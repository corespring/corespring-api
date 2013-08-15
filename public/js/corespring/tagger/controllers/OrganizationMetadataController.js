function OrganizationMetadataController($scope, MessageBridge) {

  var getMetadataSet = function(key,sets){
     return _.find(sets, function(s){return s.metadataKey == key});
  };

  var getEditorUrl = function(key, sets){
    var metadataSet = getMetadataSet(key,sets);
    if(metadataSet && metadataSet.editorUrl){
      return metadataSet.editorUrl;
    } else {
      return "/metadata/unkown-editor-url?" + key
    }
  };

  $scope.$watch("itemData", function() {
  });

  $scope.$watch("selectedMetadataSet", function(k) {
    if (!k) return;

    if ($scope.metadataSets)
      $scope.editUrl = getEditorUrl(k, $scope.metadataSets);
  });

  MessageBridge.addMessageListener(function (message) {
    if (message && message.data && message.data.type == 'updateMetadata') {
      console.log("updating metadata: ", message.data.message);
      // TODO: Update metadata in corespring
    }

    if (message && message.data && message.data.type == 'requestMetadata') {
      console.log("Metadata Requested");

      // TODO: Send current metadata
      var current = getMetadataSet($scope.selectedMetadataSet, $scope.metadataSets);
      MessageBridge.sendMessage("externalMetadataEditor", {type: "currentMetadata", message: current}, true);
    }
  });

}
