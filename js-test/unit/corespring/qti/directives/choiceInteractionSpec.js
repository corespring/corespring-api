describe('qtiDirectives.choiceinteraction', function () {
    'use strict';

    var helper = new com.qti.helpers.QtiHelper();

    var basicNode = ['<choiceInteraction responseIdentifier="question" maxChoices="${maxChoices}">',
        '<simpleChoice identifier="a">A</simpleChoice>',
        '<simpleChoice identifier="b">B</simpleChoice>',
        '</choiceInteraction>'
    ].join("\n");


    var getRadioInteraction = function () {
        return getInteraction();
    };

    var getCheckboxInteraction = function () {
        return getInteraction(basicNode.replace("${maxChoices}", 0));
    };

    var getInteraction = function (node) {
        node = (node || basicNode.replace("${maxChoices}", 1));
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

    describe('choiceInteraction', function () {


        describe("compilation", function () {

            it('inits checkboxes correctly', function () {
                var interaction = getCheckboxInteraction();
                expect(interaction.scope).not.toBe(null);
                var element = interaction.element;
                expect(element.find('simplechoice').length).toBe(2);
                expect(element.find('input').attr('type')).toBe('checkbox');
            });

            it('inits radios correctly', function () {
                var interaction = getRadioInteraction();
                expect(interaction.scope).not.toBe(null);
                var element = interaction.element;
                expect(element.find('simplechoice').length).toBe(2);
                expect(element.find('input').attr('type')).toBe('radio');
            });
        });

        describe("behaviour", function () {
            it('sets chosen item for radios', function () {
                var interaction = getRadioInteraction();
                interaction.scope.setChosenItem("a");
                expect(interaction.scope.chosenItem).toBe("a");
                expect(interaction.scope.controller.findItemByKey("question").value).toBe("a");
            });

            it('sets chosen item for checkboxes', function () {
                var interaction = getCheckboxInteraction();
                interaction.scope.setChosenItem("a");
                expect(interaction.scope.chosenItem).toEqual(["a"]);
                expect(interaction.scope.controller.findItemByKey("question").value).toEqual(["a"]);
                interaction.scope.setChosenItem("b");
                expect(interaction.scope.chosenItem).toEqual(["a", "b"]);
                expect(interaction.scope.controller.findItemByKey("question").value).toEqual(["a", "b"]);
                interaction.scope.setChosenItem("b");
                expect(interaction.scope.chosenItem).toEqual(["a"]);
                expect(interaction.scope.controller.findItemByKey("question").value).toEqual(["a"]);
            });


            it('watches showNoResponseFeedback', function () {
                var interaction = getRadioInteraction();

                rootScope.$apply(function () {
                    rootScope.showNoResponseFeedback = true;
                });
                expect(interaction.scope.noResponse).toBe(true);

                rootScope.$apply(function () {
                    rootScope.showNoResponseFeedback = false;
                });
                expect(interaction.scope.noResponse).toBe(false);

                interaction.scope.setChosenItem("a");

                rootScope.$apply(function () {
                    rootScope.showNoResponseFeedback = true;
                });
                expect(interaction.scope.noResponse).toBe(false);
            });
        });
    });

    describe('simplechoice', function () {

        var getSimpleChoiceInteraction = function () {
            var node = [
                '<choiceInteraction responseIdentifier="question" maxChoices="1">',
            '<simpleChoice identifier="a">A</simpleChoice>',
            '</choiceInteraction>'].join("\n");

            var r = getInteraction(node);
            return { scope: r.scope.$$childHead, element:  r.element.find("simpleChoice") };
        };

        it('inits', function () {
            var interaction = getSimpleChoiceInteraction();
            expect(interaction.scope).not.toBeNull();
            expect(interaction.scope).not.toBeUndefined();
            expect(interaction.element.length).toBe(1);
            expect(interaction.scope.value).toEqual("a");
        });

        it('highlights correct response when its the users response and correct response highlighting is disabled', function() {
            var interaction = getSimpleChoiceInteraction();
            helper.setSessionSettings( rootScope, { highlightUserResponse: true, highlightCorrectResponse: false});
            interaction.scope.setChosenItem("a");
            helper.setCorrectResponseOnScope(rootScope, "question","a");
            expect(interaction.element.attr('class').contains('correct-response')).toBe(true);
        });

        it('highlights correct response', function() {
            var interaction = getSimpleChoiceInteraction();
            helper.setSessionSettings( rootScope, { highlightCorrectResponse: true});
            rootScope.itemSession.isFinished = true;
            interaction.scope.setChosenItem("a");
            helper.setCorrectResponseOnScope(rootScope, "question","a");
            expect(interaction.element.attr('class').contains('correct-response')).toBe(true);
        });

        it('highlights incorrect selection', function(){
            var interaction = getSimpleChoiceInteraction();
            helper.setSessionSettings(rootScope, { highlightUserResponse : true});
            interaction.scope.setChosenItem("a");
            helper.setCorrectResponseOnScope( rootScope, "question","b");
            expect(interaction.element.attr('class').contains('incorrect-response')).toBe(true);
        });

        it('does not highlight incorrect selection if disabled', function(){
            var interaction = getSimpleChoiceInteraction();
            helper.setSessionSettings(rootScope, { highlightUserResponse : false});
            interaction.scope.setChosenItem("a");
            helper.setCorrectResponseOnScope( rootScope, "question","b");
            expect(interaction.element.attr('class').contains('incorrect-response')).toBe(false);
        });

        it('does not highlight correct response if not enabled', function() {
            var interaction = getSimpleChoiceInteraction();
            helper.setSessionSettings( rootScope, { highlightCorrectResponse: false});
            interaction.scope.setChosenItem("a");
            helper.setCorrectResponseOnScope(rootScope, "question","a");
            console.log(interaction.element.attr('class'));
            expect(interaction.element.attr('class').contains('correct-response')).toBe(false);
        });


        it('responds to click', function () {
            var interaction = getSimpleChoiceInteraction();

            helper.setSessionSettings( rootScope, { highlightCorrectResponse: true});

            rootScope.itemSession.isFinished = true;

            interaction.scope.onClick();

            expect(interaction.scope.controller.scope.chosenItem).toBe("a");

            helper.setCorrectResponseOnScope( rootScope, "question","a");

            expect(interaction.element.attr('class').contains('correct-response')).toBe(true);

            helper.setCorrectResponseOnScope( rootScope, "question","b");

            expect(interaction.element.attr('class').contains('correct-response')).toBe(false);
        });

        it('resets ui', function(){

            var interaction = getSimpleChoiceInteraction();

            helper.setSessionSettings( rootScope, { highlightCorrectResponse: true});

            rootScope.itemSession.isFinished = true;

            interaction.scope.onClick();

            helper.setCorrectResponseOnScope( rootScope, "question", "a");

            expect(interaction.element.attr('class').contains('correct-response')).toBe(true);

            rootScope.$broadcast('resetUI');

            expect(interaction.element.attr('class').contains('correct-response')).toBe(false);

        });
    });

});
