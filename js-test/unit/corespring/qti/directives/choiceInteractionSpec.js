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

    var mockApplied = false;

    beforeEach(function () {

        if (mockApplied) {
            return;
        }

        var __this = {

            MockChoiceInteractionController:function () {
                console.log("mock choiceInteraction controller ");
                this.scope = {
                    setChosenItem:function (value) {
                        this.chosenValue = value;
                    }
                };
            }
        };

        /**
         * a directive that allows you to mock the outer controller of a directive.
         * eg: if a directive <a/> requires a controller from a directive <x/> (aka <x><a></a></x>)
         * Then instead of wrapping it (x itself may need other directives),
         * you can just go: <mock-controller node="x" ctrl="XMock"><a></a></mock-controller>
         * where XMock is defined above
         */
        angular.module('qti')
            .directive('mockController', function () {

                return {
                    restrict:'E',
                    transclude:true,
                    template:'<span ng-transclude></span>',
                    compile:function (element, attrs, transclude) {
                        console.log("add mock controller to element...");
                        var nodeName = attrs["node"];
                        element.data('$' + nodeName + 'Controller', new __this[attrs["ctrl"]]());
                        return function ($scope, element, attrs) {
                        }
                    }
                };
            });
        mockApplied = true;
    });

    beforeEach(module('qti'));

    var rootScope, compile;

    beforeEach(inject(function ($compile, $rootScope, _$httpBackend_) {
        helper.prepareBackend(_$httpBackend_);
        rootScope = $rootScope.$new();
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
            var node = ['<mock-controller node="choiceinteraction" responseidentifier="rid" ctrl="MockChoiceInteractionController">',
                '<div><simplechoice identifier="a">hello</simplechoice></div>',
                '</mock-controller>'].join("\n");
            var element = compile(node)(rootScope);

            var mockScope = rootScope.$$childHead;

            mockScope.isFeedbackEnabled = function() {return true;};

            return {
                scope:mockScope.$$childHead,
                element:element.find('simplechoice'),
                mockScope: mockScope
            };
        };

        it('inits', function () {
            var interaction = getSimpleChoiceInteraction();
            expect(interaction.scope).not.toBeNull();
            expect(interaction.scope).not.toBeUndefined();
            expect(interaction.element.length).toBe(1);
            expect(interaction.scope.value).toEqual("a");
        });

        it('responds to click', function () {
            var interaction = getSimpleChoiceInteraction();
            interaction.scope.onClick();

            expect(interaction.scope.controller.scope.chosenValue).toBe("a");

            rootScope.itemSession = {};
            rootScope.itemSession.sessionData = {};

            rootScope.$apply(function () {
                rootScope.itemSession.sessionData.correctResponses = { rid:"a" }
            });

            expect(interaction.element.attr('class').contains('correct-response')).toBe(true);

            rootScope.$apply(function () {
                rootScope.itemSession.sessionData.correctResponses = { rid:"b" }
            });

            expect(interaction.element.attr('class').contains(' correct-response')).toBe(false);
        });

        it('resets ui', function(){

            var interaction = getSimpleChoiceInteraction();
            interaction.scope.onClick();
            rootScope.$apply(function () {
                rootScope.itemSession = {};
                rootScope.itemSession.sessionData = {};
                rootScope.itemSession.sessionData.correctResponses = { rid:"a" }
            });

            expect(interaction.element.attr('class').contains('correct-response')).toBe(true);

            rootScope.$broadcast('resetUI');

            expect(interaction.element.attr('class').contains('correct-response')).toBe(false);

        });
    });

});
