describe('CoreSpringPlayer', function () {
  'use strict';

  describe("inits", function(){
    it("is inited correctly", function() {
      var myDiv = $('<div id="myDiv"></div>');

      com.corespring.TestPlayer.init(myDiv, undefined, function(e) {
         console.log("Error: ", e);
         console.log(e);
      });
      expect(myDiv).toBe('div#myDiv');
      expect(true).toBeTruthy();
    });
  });
});

