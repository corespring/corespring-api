describe('CoreSpringPlayer', function () {
  'use strict';


  var myDiv;

  beforeEach(function () {
    myDiv = $('<div id="myDiv"></div>');
  });

  describe("inits with appropriate options", function () {

    it("needs options", function () {
      this.errorFn = function (e) {
        expect(e.code).toBe(com.corespring.TestPlayer.errors.NEED_OPTIONS);
      };
      spyOn(this, "errorFn").andCallThrough();
      com.corespring.TestPlayer.init(myDiv, undefined, this.errorFn);
      expect(this.errorFn).toHaveBeenCalled();
    });

    it("needs corespring url", function () {
      this.errorFn = function (e) {
        expect(e.code).toBe(com.corespring.TestPlayer.errors.NEED_CORESPRING_URL);
      };
      spyOn(this, "errorFn").andCallThrough();
      com.corespring.TestPlayer.init(myDiv, {sessionId: "something"}, this.errorFn);
      expect(this.errorFn).toHaveBeenCalled();
    });

    it("needs session id or item id", function () {
      this.errorFn = function (e) {
        expect(e.code).toBe(com.corespring.TestPlayer.errors.NEED_ITEMID_OR_SESSIONID);
      };
      spyOn(this, "errorFn").andCallThrough();
      com.corespring.TestPlayer.init(myDiv, {corespringUrl: "something"}, this.errorFn);
      expect(this.errorFn).toHaveBeenCalled();
    });

    it("inits properly", function () {
      this.errorFn = function (e) {
        expect(e.code).toBe(com.corespring.TestPlayer.errors.NEED_ITEMID_OR_SESSIONID);
      };
      spyOn(this, "errorFn").andCallThrough();
      com.corespring.TestPlayer.init(myDiv, {corespringUrl: "something", sessionId: "something"}, this.errorFn);
      expect(this.errorFn).not.toHaveBeenCalled();
      expect(myDiv).toContain("iframe");
    });

  });
});

