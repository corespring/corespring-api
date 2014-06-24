var expect = require('chai').expect;

describe('corespring-app', function() {

  it('does have a home link', function(done) {
    browser
      .url(regressionTestRunnerGlobals.getUrl("/"))
      .isVisible('a[href="/"]', function(err, result) {
        expect(err).to.be.null;
        expect(result).not.to.be.null;
      })
      .call(done);
  });

});
