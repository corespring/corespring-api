describe('qtiDirectives.orderinteraction', function () {
  'use strict';

  var helper = new com.qti.helpers.QtiHelper();

  var basicNode = ['<orderInteraction responseIdentifier="question" maxChoices="${maxChoices}">',
    '<prompt>Prompt text</prompt>',
    '<simpleChoice identifier="a">A</simpleChoice>',
    '<simpleChoice identifier="b">B</simpleChoice>',
    '</orderInteraction>'
  ].join("\n");

  var shuffleNode = ['<orderInteraction responseIdentifier="question" maxChoices="1" shuffle="${shuffle}">',
    '<prompt>Prompt text</prompt>',
    '<simpleChoice identifier="a">A</simpleChoice>',
    '<simpleChoice identifier="b">B</simpleChoice>',
    '<simpleChoice identifier="c" fixed="true">C</simpleChoice>',
    '</orderInteraction>'
  ].join("\n");

  var getInteraction = function (node) {
    node = (node || basicNode.replace("${maxChoices}", 1));
    return helper.compileAndGetScope(rootScope, compile, node);
  };

  var getShuffleInteraction = function (shuffle) {
    var node = shuffleNode.replace("${shuffle}", shuffle);
    return helper.compileAndGetScope(rootScope, compile, node);
  };

  afterEach(function () {
    rootScope = null;
  });


  beforeEach(module('qti'));

  var rootScope, compile;

  beforeEach(inject(function ($compile, $rootScope, _$httpBackend_) {
    helper.prepareBackend(_$httpBackend_);
    rootScope = $rootScope.$new();

    rootScope.highlightCorrectResponse = function () {
      return true
    };
    compile = $compile;
  }));

  describe('orderInteraction', function () {

    describe("compilation", function () {
      it('inits correctly', function () {
        var interaction = getInteraction()
        expect(interaction.scope).not.toBe(null);
        var element = interaction.element;
        expect(interaction.scope.prompt).toBe("Prompt text");
        expect(interaction.scope.orderedList.length).toBe(2);
      });
    });

    describe("behavior", function () {
      it('shuffles if shuffle is true and preserves fixed position', function () {
        var numFirstChoiceIsA = 0, numFirstChoiceIsB = 0;
        var numLastChoiceIsC = 0;
        for (var i = 0; i < 100; i++) {
          inject(function ($rootScope) {
            rootScope = $rootScope.$new();
          });

          var interaction = getShuffleInteraction(true);
          if (interaction.scope.orderedList[0].identifier == 'a') numFirstChoiceIsA++;
          else if (interaction.scope.orderedList[0].identifier == 'b') numFirstChoiceIsB++;
          if (interaction.scope.orderedList[2].identifier == 'c') numLastChoiceIsC++;
        }
        // Distribution should look like: 50% A or B, 50% A or B, 100% C (fixed choice)
        expect(numFirstChoiceIsA).toBeGreaterThan(30);
        expect(numFirstChoiceIsA).toBeLessThan(70);
        expect(numFirstChoiceIsB).toBeGreaterThan(30);
        expect(numFirstChoiceIsB).toBeLessThan(70);
        expect(numLastChoiceIsC).toBe(100);
      });

      it('doesnt shuffle if shuffle is false and preserves fixed position', function () {
        var numFirstChoiceIsA = 0;
        var numLastChoiceIsC = 0;
        for (var i = 0; i < 100; i++) {
          inject(function ($rootScope) {
            rootScope = $rootScope.$new();
          });
          var interaction = getShuffleInteraction(false);
          if (interaction.scope.orderedList[0].identifier == 'a') numFirstChoiceIsA++;
          if (interaction.scope.orderedList[2].identifier == 'c') numLastChoiceIsC++;
        }
        // Distribution should look like: 100% A, 100% B, 100% C
        expect(numFirstChoiceIsA).toBe(100);
        expect(numLastChoiceIsC).toBe(100);
      });
    });
  });
});
