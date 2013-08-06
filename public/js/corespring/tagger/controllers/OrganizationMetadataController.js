function OrganizationMetadataController($scope, MessageBridge) {
  MessageBridge.addMessageListener(function (message) {
    if (message && message.data && message.data.type == 'setMetadata') {
      console.log("message received: ", message.data.message);
    }

    if (message && message.data && message.data.type == 'requestMetadata') {
      console.log("Metadata Requested");
    }


  });



}
