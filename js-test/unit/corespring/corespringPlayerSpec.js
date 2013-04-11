describe('CoreSpringPlayer', function () {
  'use strict';


  var myDiv;

  beforeEach(function () {
    myDiv = $('<div id="myDiv"></div>');
  });

  describe("initialization", function () {

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

  describe("behavior", function () {
    it("sets correct url for rendering by session id", function () {
      com.corespring.TestPlayer.init(myDiv, {corespringUrl: "csurl", sessionId: "sid", width: "500px", height: "500px"});
      var iframe = myDiv.find("iframe");
      var url = iframe.attr('src');
      expect(url).toBe("csurl/testplayer/session/sid/render");
    });

    it("sets correct url for rendering by session id", function () {
      com.corespring.TestPlayer.init(myDiv, {corespringUrl: "csurl", itemId: "iid", width: "500px", height: "500px"});
      var iframe = myDiv.find("iframe");
      var url = iframe.attr('src');
      expect(url).toBe("csurl/testplayer/item/iid/run");
    });

    it("sets width and height", function () {
      com.corespring.TestPlayer.init(myDiv, {corespringUrl: "something", sessionId: "something", width: "500px", height: "500px"});
      var iframe = myDiv.find("iframe");
      expect(iframe).toBe("iframe");
      expect(myDiv).toHaveCss({"width":"500px"});
      expect(myDiv).toHaveCss({"height":"500px"});
    });

  });
});

