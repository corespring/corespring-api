describe('qtiDirectives.draganddropinteraction', function () {
  'use strict';

  var helper = new com.qti.helpers.QtiHelper();

  var basicNode = [
    '<dragAndDropInteraction responseIdentifier="alphabet1">',
    '<prompt>Some prompt</prompt>',
    '<draggableChoice identifier="apple" copyOnDrag="true">Apple</draggableChoice>',
    '<draggableChoice identifier="pear" placeholder-class="placeholder">Pear</draggableChoice>',
    '<draggableChoice identifier="walnut">Walnut</draggableChoice>',
    '<draggableChoice identifier="car">Car</draggableChoice>',
    '<draggableChoice identifier="cow">Cow</draggableChoice>',
    '<draggableChoice identifier="bus">Bus</draggableChoice>',
    '<landingPlace cardinality="ordered" class="blueTile inline" identifier="target1"></landingPlace>',
    '<landingPlace cardinality="multiple" class="blueTile inline" identifier="target2"></landingPlace>',
    '<landingPlace cardinality="single" class="blueTile inline" identifier="target3"></landingPlace>',
    '<landingPlace cardinality="ordered" class="blueTile inline" identifier="target4"></landingPlace>',
    '</dragAndDropInteraction>'
  ].join("\n");

  var getInteraction = function (node) {
    node = (node || basicNode);
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

  describe('dragAndDropInteraction', function () {

    describe("compilation", function () {

      it('inits correctly', function () {
        var interaction = getInteraction();
        expect(interaction.scope).not.toBe(null);

        // has prompt
        expect(interaction.element.find('span.prompt').length).toBe(1);

        // has the answers
        expect(interaction.scope.listAnswers).toEqual([
          { id: 'apple' },
          { id: 'pear' },
          { id: 'walnut' },
          { id: 'car' },
          { id: 'cow' },
          { id: 'bus' }
        ]);

        // has the targets
        expect(interaction.scope.listTargets).toEqual([
          [],  // ordered
          [],  // multiple
          {},  // single
          []   // ordered
        ]);
      });

    });

    describe("behaviour", function () {
      it('sends correct response', function () {
      });
    });
  });
});
