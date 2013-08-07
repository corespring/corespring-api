function OrganizationMetadataController($scope, MessageBridge) {
  MessageBridge.addMessageListener(function (message) {
    if (message && message.data && message.data.type == 'updateMetadata') {
      console.log("updating metadata: ", message.data.message);
    }

    if (message && message.data && message.data.type == 'requestMetadata') {
      console.log("Metadata Requested");

      // TODO: Send current metadata

      var current = {
        "rabbit": "fast",
        "snail": "slow"
      };

      MessageBridge.sendMessage("externalMetadataEditor", {type: "currentMetadata", message: current}, true);
    }


  });



}
