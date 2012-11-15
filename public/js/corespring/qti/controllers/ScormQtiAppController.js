function QtiAppController($scope, $timeout, $location, AssessmentSessionService, ScormBridge) {

    $timeout(function () {
        if (typeof(MathJax) != "undefined") {
            MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
        }
    }, 200);


    var registeredInteractions = [];


    $scope.registerInteraction = function(id,prompt){
        var interaction = {index: registeredInteractions.length, id:id, prompt: prompt};
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

    $scope.init = function () {
        var url = $location.absUrl();
        var matches = url.match(/.*\/scorm-player\/(.*?)\/.*/);
        var params = { itemId:matches[1] };
        AssessmentSessionService.create(params, {}, function (data) {
            $scope.itemSession = data;
        });

        /**
         * The form has been submitted - process the responses and notify Scorm
         * of correct/incorrect results.
         */
        $scope.$on('formSubmitted', function(event, responses){
           for( var i = 0 ; i < responses.length ; i++){
            var serverResponse = responses[i];
            var result = (serverResponse.outcome && serverResponse.outcome.score == 1) ? "correct" : "incorrect";
            var index = getIndexById(serverResponse.id);

            ScormBridge.sendMessage({
                action: "setValue",
                key: "result",
                value: result,
                index: index
            });
           }

            ScormBridge.sendMessage({
                action: "setValue",
                key: "completion_status",
                value: "completed"
            });
        });

        $scope.$on('registerInteraction', function(event, id, prompt){
            console.log('registerInteraction: ' + id + ", " + prompt);

            var interaction = $scope.registerInteraction(id,prompt);
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
                });
        });
    };

    $scope.init();
}

QtiAppController.$inject = ['$scope', '$timeout', '$location', 'AssessmentSessionService', 'ScormBridge'];

