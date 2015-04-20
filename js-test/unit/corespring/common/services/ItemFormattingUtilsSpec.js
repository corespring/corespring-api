describe('common.ItemFormattingUtils', function () {
  'use strict';

  var ctrl, scope, $httpBackend;

  beforeEach(module('corespring-utils'));

  beforeEach(inject(function (_$httpBackend_, $rootScope, $controller) {
    $httpBackend = _$httpBackend_;
    scope = $rootScope.$new();

    function MockController($scope, ItemFormattingUtils){
      angular.extend($scope, ItemFormattingUtils);
    }

    MockController.$inject = ['$scope', 'ItemFormattingUtils'];
    try {
      ctrl = $controller(MockController, {$scope: scope});
    } catch (e) {
      throw("Error with the controller: " + e);
    }
  }));


  describe('ItemFormattingUtils', function () {


    it("creates a sorted grade level string", function(){

      expect(
        scope.createGradeLevelString(["01","KG","Other"]) )
      .toEqual( "KG,01,Other");
    });

    it("generates a copyright image url ", function(){
      var item = {
        copyrightOwner: "New York State Education Department"
      };
      expect(
        scope.getCopyrightUrl(item) )
      .toEqual( "/assets/images/copyright/nysed.png");
    });

    it("creates a primary subject label", function(){

      var subj = {
        category: "Category",
        subject: "Subject"
      };

      expect(
        scope.getPrimarySubjectLabel( subj )
        ).toBe( subj.category + ": " + subj.subject);

      var subjNoCategory = {
        subject: "Subject"
      };

      expect(
        scope.getPrimarySubjectLabel(subjNoCategory)
        ).toBe(subjNoCategory.subject);

      var subjNoSubject = {
        category: "Category"
      };

      expect(
        scope.getPrimarySubjectLabel(subjNoSubject)
        ).toBe(subjNoSubject.category);
    });

    it("builds title with an ellipsis", function(){
      var fullTitle = "This is my awesome title";

      if(fullTitle.length > 50){
        expect(scope.buildTitleEllipsis(fullTitle)).toBe(fullTitle.substr(0,50) + "...");
      } else {
        expect(scope.buildTitleEllipsis(fullTitle)).toBe(fullTitle);
      }
    });

    it("builds title tooltip", function(){
      var fullTitle = "This is my title tooltip text ";

      if(fullTitle.length > 50){
        expect(scope.buildTitleTooltip(fullTitle)).toBe(fullTitle);
      } else {
        expect(scope.buildTitleTooltip(fullTitle)).toBe("");
      }
    });

    it("builds description with an ellipsis", function(){
      var fullDescription = "This is my awesome description";

      if(fullDescription.length > 100){
        expect(scope.buildDescriptionEllipsis(fullDescription)).toBe(fullDescription.substr(0,50) + "...");
      } else {
        expect(scope.buildDescriptionEllipsis(fullDescription)).toBe(fullDescription);
      }
    });

    it("builds description tooltip", function(){
      var fullDescription = "This is my description tooltip text ";

      if(fullDescription.length > 100){
        expect(scope.buildDescriptionTooltip(fullDescription)).toBe(fullDescription);
      } else {
        expect(scope.buildDescriptionTooltip(fullDescription)).toBe("");
      }
    });

    it("builds a standard label", function(){

      expect(scope.buildStandardLabel([])).toBe("");

      var s = [{ dotNotation: "dotNotation"} ];

      expect(scope.buildStandardLabel(s)).toBe(s[0].dotNotation);
      s.push( { dotNotation: "dotNotation" } );
      expect(scope.buildStandardLabel(s)).toBe(s[0].dotNotation + " plus 1 more");
    });

    it("builds a standards tooltip", function(){

      expect(scope.buildStandardTooltip([])).toBe("<span></span>");

      var s = [
      { standard: "s", dotNotation: "dn"}
      ];

      expect(scope.buildStandardTooltip(s)).toBe("<span>s</span>");
      s.push({ standard: "a b c d e f g", dotNotation: "dn2"});
      expect(scope.buildStandardTooltip(s)).toBe("<span>dn: s, dn2: a b c d e f...</span>");
    });

    it('should get short subject label', function(){

      var subj = {
        category: "Mathematics",
        subject: "Blah"
      };

      var out = scope.getShortSubjectLabel(subj);
      expect(out).toBe("Math: Blah");

      var subj = {
        category: "English Language Arts",
        subject: "Blah"
      };

      var out = scope.getShortSubjectLabel(subj);
      expect(out).toBe("ELA: Blah");
    });

    it('should show item type', function(){
       var out = scope.showItemType({ itemType : "Other", itemTypeOther: "Blah"});
       expect(out).toBe("Other: Blah");

       expect(scope.showItemType({itemType: "B"}) ).toBe("B");
    });

    it('should show item type abbreviated', function(){

      expect(
        scope.showItemTypeAbbreviated({itemType: "Multiple Choice"})
        ).toBe("MC");

      expect(
        scope.showItemTypeAbbreviated({itemType: "Other"})
        ).toBe("OTH");
    });

    it('abbreviates contributor names', function(){

      expect( scope.getAuthorAbbreviation("Ed")).toBe("Ed");
      expect( scope.getAuthorAbbreviation("Ed is cool")).toBe("EIC");
      expect( scope.getAuthorAbbreviation("State of New Jersey Department of Education")).toBe("NJDOE");
      expect( scope.getAuthorAbbreviation("New York State Education Department")).toBe("NYSED");
      expect( scope.getAuthorAbbreviation("Illustrative Mathematics")).toBe("Illustrative");
      expect( scope.getAuthorAbbreviation("TIMSS")).toBe("TIMSS");
    });

  });

});