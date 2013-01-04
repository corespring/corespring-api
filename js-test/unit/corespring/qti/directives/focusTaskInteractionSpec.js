describe('qtiDirectives.choiceinteraction', function () {
    'use strict';

    var helper = new com.qti.helpers.QtiHelper();

    var basicNode = ['<focusTaskInteraction responseIdentifier="question">',
        '<focusChoice identifier="a">A</focusChoice>',
        '<focusChoice identifier="b">B</focusChoice>',
        '</focusTaskInteraction>'
    ].join("\n");

    var getInteraction = function (node) {
        node = (node || basicNode);
        return helper.compileAndGetScope(rootScope, compile, node);
    };

    afterEach(function() {
        rootScope = null;
    });

    beforeEach(module('qti'));

    var rootScope, compile;

    beforeEach(inject(function ($compile, $rootScope, _$httpBackend_) {
        helper.prepareBackend(_$httpBackend_);
        rootScope = $rootScope.$new();

        rootScope.highlightCorrectResponse = function(){ return true};
        compile = $compile;
    }));

    describe('focusTaskInteraction', function () {

        describe("compilation", function () {

            it('inits correctly', function () {
                var interaction = getInteraction();
                expect(interaction.scope).not.toBe(null);
                var element = interaction.element;
                expect(element.find('[focuschoice]').length).toBe(2);
            });

        });

        describe("behaviour", function () {

            it('sets chosen item', function () {
                var interaction = getInteraction();
                interaction.scope.setChosenItem("a", true);
                expect(interaction.scope.chosenItem).toEqual(["a"]);
                expect(interaction.scope.controller.findItemByKey("question").value).toEqual(["a"]);
                interaction.scope.setChosenItem("b", true);
                expect(interaction.scope.chosenItem).toEqual(["a", "b"]);
                expect(interaction.scope.controller.findItemByKey("question").value).toEqual(["a", "b"]);
                interaction.scope.setChosenItem("b", false);
                expect(interaction.scope.chosenItem).toEqual(["a"]);
                expect(interaction.scope.controller.findItemByKey("question").value).toEqual(["a"]);
            });


            it('watches showNoResponseFeedback', function () {
                var interaction = getInteraction();

                rootScope.$apply(function () {
                    rootScope.showNoResponseFeedback = true;
                });
                expect(interaction.scope.noResponse).toBe(true);

                rootScope.$apply(function () {
                    rootScope.showNoResponseFeedback = false;
                });
                expect(interaction.scope.noResponse).toBe(false);

                interaction.scope.setChosenItem("a", true);

                rootScope.$apply(function () {
                    rootScope.showNoResponseFeedback = true;
                });
                expect(interaction.scope.noResponse).toBe(false);
            });
        });
    });

    describe('focusChoice on checked interaction', function () {

        var getFocusChoiceInteraction = function () {
            var node = [
                    '<focusTaskInteraction responseIdentifier="question" checkIfCorrect="yes" minSelections="1" maxSelections="1">',
                    '<focusChoice identifier="a">A</focusChoice>',
                    '<focusChoice identifier="b">B</focusChoice>',
                    '</focusTaskInteraction>'
                ].join("\n");

            var r = getInteraction(node);
            return { scope: r.scope.$$childHead, element:  r.element.find("[focusChoice]") };
        };

        it('inits', function () {
            var interaction = getFocusChoiceInteraction();
            expect(interaction.scope).not.toBeNull();
            expect(interaction.scope).not.toBeUndefined();
            expect(interaction.element.length).toBe(2);
            expect(interaction.scope.value).toEqual("a");
        });

        it('highlights correct response when its the users response and correct response highlighting is disabled', function() {
            var interaction = getFocusChoiceInteraction();
            helper.setSessionSettings( rootScope, { highlightUserResponse: true, highlightCorrectResponse: false});
            interaction.scope.setChosenItem("a", true);
            rootScope.itemSession.responses = [{"id":"question","value":["a","b"],"outcome":{}}];
            helper.setCorrectResponseOnScope(rootScope, "question","a");
            expect(interaction.element.attr('class').contains('shouldHaveBeenSelected')).toBe(true);
            expect(interaction.element.attr('class').contains('selected')).toBe(true);
        });

        it('highlights correct response', function() {
            var interaction = getFocusChoiceInteraction();
            helper.setSessionSettings( rootScope, { highlightCorrectResponse: true});
            rootScope.itemSession.isFinished = true;
            interaction.scope.setChosenItem("a", true);
            rootScope.itemSession.responses = [{"id":"question","value":["a"],"outcome":{}}];
            helper.setCorrectResponseOnScope(rootScope, "question","a");
            expect(interaction.element.attr('class').contains('shouldHaveBeenSelected')).toBe(true);
        });

        it('highlights incorrect selection', function(){
            var interaction = getFocusChoiceInteraction();
            helper.setSessionSettings(rootScope, { highlightUserResponse : true});
            interaction.scope.setChosenItem("a", true);
            rootScope.itemSession.responses = [{"id":"question","value":["a"],"outcome":{}}];
            helper.setCorrectResponseOnScope( rootScope, "question","b");
            expect(interaction.element.attr('class').contains('shouldNotHaveBeenSelected')).toBe(true);
        });

        it('does not highlight incorrect selection if disabled', function(){
            var interaction = getFocusChoiceInteraction();
            helper.setSessionSettings(rootScope, { highlightUserResponse : false});
            interaction.scope.setChosenItem("a", true);
            rootScope.itemSession.responses = [{"id":"question","value":["a"],"outcome":{}}];
            helper.setCorrectResponseOnScope( rootScope, "question","b");
            expect(interaction.element.attr('class').contains('shouldNotHaveBeenSelected')).toBe(false);
        });

        it('does not highlight correct response if not enabled', function() {
            var interaction = getFocusChoiceInteraction();
            helper.setSessionSettings( rootScope, { highlightCorrectResponse: false});
            interaction.scope.setChosenItem("a", true);
            rootScope.itemSession.responses = [{"id":"question","value":["a"],"outcome":{}}];
            helper.setCorrectResponseOnScope(rootScope, "question","a");
            expect(interaction.element.attr('class').contains('shouldHaveBeenSelected')).toBe(false);
        });


        it('responds to click', function () {
            var interaction = getFocusChoiceInteraction();

            helper.setSessionSettings( rootScope, { highlightCorrectResponse: true});
            rootScope.itemSession.isFinished = true;

            interaction.scope.click();

            expect(interaction.scope.controller.scope.chosenItem).toEqual(["a"]);

            rootScope.itemSession.responses = [{"id":"question","value":["a"],"outcome":{}}];
            helper.setCorrectResponseOnScope( rootScope, "question","a");
            expect(interaction.element.attr('class').contains('selected')).toBe(true);

            interaction.scope.click();

            rootScope.itemSession.responses = [{"id":"question","value":["a"],"outcome":{}}];
            helper.setCorrectResponseOnScope( rootScope, "question","b");
            expect(interaction.element.attr('class').contains('selected')).toBe(false);
        });

        it('resets ui', function(){
            var interaction = getFocusChoiceInteraction();
            helper.setSessionSettings( rootScope, { highlightCorrectResponse: true});
            rootScope.itemSession.isFinished = true;
            rootScope.itemSession.responses = [{"id":"question","value":["a"],"outcome":{}}];

            interaction.scope.click();

            helper.setCorrectResponseOnScope( rootScope, "question", "a");
            expect(interaction.scope.shouldHaveBeenSelected).toBeTruthy();

            rootScope.$broadcast('resetUI');
            expect(interaction.scope.shouldHaveBeenSelected).toBeFalsy();
        });
    });

    describe('focusChoice on non checked interaction', function () {

        var getFocusChoiceInteraction = function () {
            var node = [
                    '<focusTaskInteraction responseIdentifier="question" checkIfCorrect="no" minSelections="2" maxSelections="3">',
                    '<focusChoice identifier="a">A</focusChoice>',
                    '<focusChoice identifier="b">B</focusChoice>',
                    '<focusChoice identifier="c">C</focusChoice>',
                    '<focusChoice identifier="d">D</focusChoice>',
                    '</focusTaskInteraction>'
                ].join("\n");

            var r = getInteraction(node);
            return { scope: r.scope.$$childHead, element:  r.element.find("[focusChoice]") };
        };

        it('inits', function () {
            var interaction = getFocusChoiceInteraction();
            expect(interaction.scope).not.toBeNull();
            expect(interaction.scope).not.toBeUndefined();
            expect(interaction.element.length).toBe(4);
            expect(interaction.scope.value).toEqual("a");
        });

        it('highlights correct response when its the users response and correct response highlighting is disabled', function() {
            var interaction = getFocusChoiceInteraction();
            helper.setSessionSettings( rootScope, { highlightUserResponse: true, highlightCorrectResponse: false});
            interaction.scope.setChosenItem("a", true);
            interaction.scope.setChosenItem("b", true);
            rootScope.itemSession.responses = [{"id":"question","value":["a","b"],"outcome":{}}];
            helper.setCorrectResponseOnScope(rootScope, "question","a");
            expect(interaction.element.attr('class').contains('shouldHaveBeenSelected')).toBe(true);
            expect(interaction.element.attr('class').contains('selected')).toBe(true);
        });

        it('doesnt highlight anything if number of selection is less or more than required', function() {
            var interaction = getFocusChoiceInteraction();
            helper.setSessionSettings( rootScope, { highlightUserResponse: true, highlightCorrectResponse: false});
            interaction.scope.setChosenItem("a", true);
            rootScope.itemSession.responses = [{"id":"question","value":["a","b"],"outcome":{responsesBelowMin: true}}];
            helper.setCorrectResponseOnScope(rootScope, "question","a");
            expect(interaction.element.attr('class').contains('shouldHaveBeenSelected')).toBe(false);
            expect(interaction.element.attr('class').contains('selected')).toBe(true);

            rootScope.itemSession.responses = [{"id":"question","value":["a","b"],"outcome":{responsesExceedMax: true}}];
            helper.setCorrectResponseOnScope(rootScope, "question","a");
            expect(interaction.element.attr('class').contains('shouldHaveBeenSelected')).toBe(false);
            expect(interaction.element.attr('class').contains('selected')).toBe(true);
        });

    });

});
