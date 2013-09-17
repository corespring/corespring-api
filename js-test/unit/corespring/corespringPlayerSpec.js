describe('CoreSpringPlayer', function () {
  'use strict';

  var myDiv;


  beforeEach(function () {
    myDiv = $('<div id="myDiv"></div>');
  });

  describe("initialization", function () {

    it("throws an error if there are 2 elements matching the identifier",function(){

      var multipleDivs = $('<div id="myDiv"></div><div id="myDiv"></div>');
      this.errorFn = function (e) {
        expect(e.code).toBe(com.corespring.players.errors.MULTIPLE_ELEMENTS);
      };

      spyOn(this, "errorFn").andCallThrough();
      new com.corespring.players.ItemPlayer(multipleDivs, {}, this.errorFn);
      expect(this.errorFn).toHaveBeenCalled();
    });

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

    it("needs a valid mode", function(){
      this.errorFn = function (e) {
        expect(e.code).toBe(com.corespring.players.errors.NEED_MODE);
      };
      spyOn(this, "errorFn").andCallThrough();
      new com.corespring.players.ItemPlayer(myDiv, {mode: "banana"}, this.errorFn);
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

    it("is able to submit through submitItem", function(){
      var player = new com.corespring.players.ItemPlayer(myDiv, {mode: 'preview', itemId: "iid", width: "500px", height: "500px"});
      var message = null;
      window.addEventListener("message",function(e){
        message = JSON.parse(e.data);
      },false);
      waitsFor(function(){
        if(message) console.log(message)
        if (message && message.message === "submitItem") {
            return message.myopts == "has stuff";
        } else return false;
      },"a submitItem message with options", 500)
      player.submitItem({"myopts":"has stuff"});
    })

    it("sets width and height", function () {
      new com.corespring.players.ItemPlayer(myDiv, {mode: 'render', sessionId: "something", width: "500px", height: "500px"});
      var iframe = myDiv.find("iframe");
      expect(iframe).toBe("iframe");
      expect(myDiv).toHaveCss({"width":"500px"});
      expect(myDiv).toHaveCss({"height":"500px"});
    });

    var callbackIsTriggered = function(callbackName, messageId){
      var called = false;
      var callbackFn= function() { called = true; };
      runs(function() {
        var options = {mode: 'administer', sessionId: "something", width: "500px", height: "500px"};
        options[callbackName] = callbackFn;
        new com.corespring.players.ItemPlayer(myDiv,  options);
      });
      waitsFor(function() {return called; }, 500);
      window.postMessage('{"message":"'+messageId+'", "session":"something"}',"*");
    };

    it("item session created gets called", function () {
      callbackIsTriggered("onItemSessionCreated", "itemSessionCreated");
    });

    it("item session completed gets triggered by item session retrieved", function () {
      callbackIsTriggered("onItemSessionRetrieved", "itemSessionRetrieved");
    });

    it("item session completed gets triggered by item session completed", function () {
      callbackIsTriggered("onItemSessionCompleted", "sessionCompleted");
    });

  });

});

