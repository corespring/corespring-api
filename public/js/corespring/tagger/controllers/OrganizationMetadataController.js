function OrganizationMetadataController($scope, MessageBridge) {

  $scope.$watch("itemData", function() {
  });

  $scope.$watch("selectedMetadataSet", function(k) {
    if (!k) return;

    if ($scope.itemData)
      $scope.editUrl = $scope.itemData.metadataSets[k].editorUrl;

  });

  MessageBridge.addMessageListener(function (message) {
    if (message && message.data && message.data.type == 'updateMetadata') {
      console.log("updating metadata: ", message.data.message);
      // TODO: Update metadata in corespring
    }

    if (message && message.data && message.data.type == 'requestMetadata') {
      console.log("Metadata Requested");

      // TODO: Send current metadata
      var current = $scope.itemData.metadataSets[$scope.selectedMetadataSet];
      MessageBridge.sendMessage("externalMetadataEditor", {type: "currentMetadata", message: current}, true);
    }
  });

}
