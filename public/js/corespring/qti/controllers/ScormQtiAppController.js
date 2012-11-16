function QtiAppController($scope, $timeout, AssessmentSessionService, ScormBridge, Config) {

    $timeout(function () {
        if (typeof(MathJax) != "undefined") {
            MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
        }
    }, 200);


    var registeredInteractions = [];


    $scope.registerInteraction = function(id,prompt, type){

        type = (type || "other");
        
        var interaction = {
            index: registeredInteractions.length,
            id:id,
            prompt: prompt,
            type: type};

        registeredInteractions.push( interaction );
        return interaction;
    };

    var getIndexById = function(id){
        for(var i = 0 ; i < registeredInteractions.length ; i++){
            if(registeredInteractions[i].id == id){
                return i;
            }
        }
        return -1;
    };

    var getResponseById = function(arr, id){
        for(var i = 0; i < arr.length; i++){
            if(arr[i].id == id){
                return arr[i];
            }
        }
        return null;
    };

    $scope.init = function () {

        $scope.$watch('itemSession', function(newItemSession){

          if(newItemSession){
            ScormBridge.sendMessage({
              action: "setItemSession",
              value: JSON.stringify(newItemSession)
            });
          }
        });

        if(Config.sessionId === ""){
          AssessmentSessionService.create({itemId: Config.itemId}, {}, function (data) {
            $scope.itemSession = data;


          });
        } else {
          AssessmentSessionService.get({itemId: Config.itemId, sessionId: Config.sessionId}, {}, function (data) {
            $scope.itemSession = data;
          });
        }

        /**
         * The form has been submitted - process the responses and notify Scorm
         * of correct/incorrect results.
         */
        $scope.$on('formSubmitted', function(event, itemSession){

          var responses = itemSession.responses;
          var correctResponses = itemSession.sessionData.correctResponses;

           for( var i = 0 ; i < responses.length ; i++){
            var serverResponse = responses[i];
            var result = (serverResponse.outcome && serverResponse.outcome.score == 1) ? "correct" : "incorrect";
            var index = getIndexById(serverResponse.id);
            var response = serverResponse.value;
            var correctResponse = getResponseById(correctResponses, serverResponse.id);
            
            if(index !== -1){
                ScormBridge.sendMessage({
                    action: "setValue",
                    key: "result",
                    value: result,
                    index: index
                },
                {
                    action: "setValue",
                    key: "learner_response",
                    value: response,
                    index: index
                },
                {
                    action: "setValue",
                    key: "correct_responses.0.pattern",
                    value: correctResponse ? correctResponse.value : "?",
                    index: index
                });
            }
           }

            ScormBridge.sendMessage(
            {
                action: "setValue",
                key: "completion_status",
                value: "completed"
            },
            {
                action: "setValue",
                key: "success_status",
                value: "passed"
            },
            { action: "terminate" }
            );
        });

        $scope.$on('registerInteraction', function(event, id, prompt, type){
            console.log('registerInteraction: ' + id + ", " + prompt + ", " + type);

            var interaction = $scope.registerInteraction(id,prompt, type);
            //first send the id, then send the description
            ScormBridge.sendMessage(
                {
                    action: "setValue",
                    key: "id",
                    value: interaction.id,
                    index: interaction.index
                },
                {
                    action: "setValue",
                    key: "description",
                    value: interaction.prompt,
                    index: interaction.index
                },
                {
                    action: "setValue",
                    key: "type",
                    value: interaction.type,
                    index: interaction.index
                });
        });
    };

    $scope.init();
}

QtiAppController.$inject = ['$scope', '$timeout', 'AssessmentSessionService', 'ScormBridge', 'Config'];

