describe('CoreSpringPlayer', function () {
  'use strict';

  var myDiv;

  beforeEach(function () {
    myDiv = $('<div id="myDiv"></div>');
  });

  describe("initialization", function () {

    it("needs options", function () {
      this.errorFn = function (e) {
        expect(e.code).toBe(com.corespring.players.errors.NEED_OPTIONS);
      };
      spyOn(this, "errorFn").andCallThrough();
      new com.corespring.players.ItemPlayer(myDiv, undefined, this.errorFn);
      expect(this.errorFn).toHaveBeenCalled();
    });

    it("needs mode", function () {
      this.errorFn = function (e) {
        expect(e.code).toBe(com.corespring.players.errors.NEED_MODE);
      };
      spyOn(this, "errorFn").andCallThrough();
      new com.corespring.players.ItemPlayer(myDiv, {}, this.errorFn);
      expect(this.errorFn).toHaveBeenCalled();
    });


    it("needs session id or item id", function () {
      this.errorFn = function (e) {
        expect(e.code).toBe(com.corespring.players.errors.NEED_ITEMID_OR_SESSIONID);
      };
      spyOn(this, "errorFn").andCallThrough();
      new com.corespring.players.ItemPlayer(myDiv, {mode: 'render'}, this.errorFn);
      expect(this.errorFn).toHaveBeenCalled();
    });

    it("inits properly", function () {
      this.errorFn = function (e) {
        expect(e.code).toBe(com.corespring.players.errors.NEED_ITEMID_OR_SESSIONID);
      };
      spyOn(this, "errorFn").andCallThrough();
      new com.corespring.players.ItemPlayer(myDiv, {mode: "render", sessionId: "something"}, this.errorFn);
      expect(this.errorFn).not.toHaveBeenCalled();
      expect(myDiv).toContain("iframe");
    });

  });

  describe("behavior", function () {
    it("sets correct url for rendering by session id", function () {
      new com.corespring.players.ItemPlayer(myDiv, {mode: 'render', sessionId: "sid", width: "500px", height: "500px"});
      var iframe = myDiv.find("iframe");
      var url = iframe.attr('src');
      expect(url).toBe("${baseUrl}/session/sid/render");
    });

    it("sets correct url for rendering by item id", function () {
      new com.corespring.players.ItemPlayer(myDiv, {mode: 'preview', itemId: "iid", width: "500px", height: "500px"});
      var iframe = myDiv.find("iframe");
      var url = iframe.attr('src');
      expect(url).toBe("${baseUrl}/item/iid/preview");
    });


    it("sets width and height", function () {
      new com.corespring.players.ItemPlayer(myDiv, {mode: 'render', sessionId: "something", width: "500px", height: "500px"});
      var iframe = myDiv.find("iframe");
      expect(iframe).toBe("iframe");
      expect(myDiv).toHaveCss({"width":"500px"});
      expect(myDiv).toHaveCss({"height":"500px"});
    });

    it("item session created gets called", function () {
      var created = false;
      var createdFn = function() { created = true; }
      runs(function() {
        new com.corespring.players.ItemPlayer(myDiv, {mode: 'render', onItemSessionCreated: createdFn, sessionId: "something", width: "500px", height: "500px"});
      });
      waitsFor(function() {return created; }, 100);
      window.postMessage('{"message":"itemSessionCreated", "session":"something"}',"*");
    });

    it("item session completed gets triggered by item session retrieved", function () {
      var completed = false;
      var completedFn = function() { completed = true; }
      runs(function() {
        new com.corespring.players.ItemPlayer(myDiv, {mode: 'render', onItemSessionRetrieved: completedFn, sessionId: "something", width: "500px", height: "500px"});
      });
      waitsFor(function() { return completed; }, 100);
      window.postMessage('{"message":"itemSessionRetrieved", "session":"something"}',"*");
    });

    it("item session completed gets triggered by item session completed", function () {
      var completed = false;
      var completedFn = function() { completed = true; }
      runs(function() {
        new com.corespring.players.ItemPlayer(myDiv, {mode: 'render', onItemSessionCompleted: completedFn, sessionId: "something", width: "500px", height: "500px"});
      });
      waitsFor(function() { return completed; }, 100);
      window.postMessage('{"message":"sessionCompleted", "session":"something"}',"*");
    });


  });

});

