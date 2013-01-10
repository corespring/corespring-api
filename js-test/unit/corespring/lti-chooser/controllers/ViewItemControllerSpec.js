'use strict';

describe('lti-chooser.ViewItemController', function () {

    var scope, ctrl, $httpBackend;


    var MockMessageBridge = function () {
        console.log("MockMessageBridge");
        var targeted = {};
        this.addMessageListener = function(fn){
           this.addMessageListenerWithTarget("default", fn);
        };
        
        this.addMessageListenerWithTarget = function(target,fn){
            targeted[target] = (targeted[target] || []);
            targeted[target].push(fn);
        };

        this.sendMessage = function(target, msg){

            if(targeted[target]){

               for(var i = 0 ; i < targeted[target].length; i++){
                   targeted[target][i]({ data: JSON.stringify(msg)});
               }
            }
        };
    };

    var bridge = new MockMessageBridge();

    beforeEach(module('lti-services'));
    beforeEach(module('corespring-services'));
    beforeEach(module('tagger.services'));
    beforeEach(module('corespring-utils'));

    beforeEach(function () {
    module(function ($provide) {
      $provide.value('MessageBridge', bridge);
    });
  });



    var prepareBackend = function ($backend) {

        var urls = [
            {method:'GET', url:/\/api\/v1\/items.*/, response:{ ok:true }},
        ];

        for (var i = 0; i < urls.length; i++) {
            var definition = urls[i];
            $backend.when(definition.method, definition.url).respond(200, definition.response);
        }
    };


    beforeEach(inject(function (_$httpBackend_, $rootScope, $controller) {
        $httpBackend = _$httpBackend_;
        prepareBackend($httpBackend);
        scope = $rootScope.$new();

        try {
            ctrl = $controller(ltiChooser.ViewItemController, {$scope:scope});
        } catch (e) {
            throw("Error with the controller: " + e);
        }
    }));

    describe("inits", function(){

        it("is inited correctly", function(){

            var sessionSettingsSent = false;
            var onCalled = function(e){
               var d = JSON.parse(e.data);
               sessionSettingsSent = d.message == "update";
            };

            bridge.addMessageListenerWithTarget("previewIframe", onCalled);
            scope.config = { sessionSettings: { maxNoOfAttempts: 1} };
            expect(scope.previewPageIsReady).toBe(false);
            bridge.sendMessage( "default", {message: "ready"});
            expect(scope.previewPageIsReady).toBe(true);
            expect(sessionSettingsSent).toBe(true);

        });

        it("responds to update messages from bridge", function(){
            scope.config = { sessionSettings: { maxNoOfAttempts: 1} };
            bridge.sendMessage( "default", {message: "ready"});
            bridge.sendMessage( "default", {message: "update", settings: {maxNoOfAttempts: 100}});

            expect(scope.config.sessionSettings.maxNoOfAttempts).toBe(100);

        });
    });

});
