module.exports = function (config) {
  config.set({
    basePath: '..',
    frameworks: ['jasmine'],
    files: [
      'js-test/lib/jquery/jquery-1.9.1.js',
      'js-test/lib/jquery/jquery-ui-1.10.3.js',
      'js-test/lib/js-diff.js',
      'js-test/lib/jasmine-jquery.js',
      'public/js/vendor/ace/ace.js',
      'public/js/vendor/ace/theme-eclipse.js',
      'public/js/vendor/ace/mode-xml.js',
      'public/js/vendor/angular/1.0.1/angular-1.0.1.js',
      'public/js/vendor/angular-ui/angular-ui.js',
      'public/js/vendor/angular-ui-sortable/sortable.js',
      'public/js/vendor/angular-dragdrop/angular-dragdrop.min.js',
      'public/js/vendor/angular/1.0.1/angular-resource-1.0.1.js',
      'public/js/vendor/angular/1.0.1/angular-mocks-1.0.1.js',
      'public/js/vendor/underscore/1.3.3/underscore.js',
      'public/js/vendor/angular-bootstrap-ui/angular-bootstrap-ui.js',
      'public/js/vendor/corespring-ng-components/corespring-ng-components.js',
      'public/js/corespring/qti/services/qtiServices.js',
      'public/js/corespring/**/*.js',
      'js-test/lib/play.mock.routes.js',
      'js-test/unit/**/*.js'
    ],
    exclude: ["public/js/corespring/qti/directives/printing/*.js",
      "public/js/corespring/qti/directives/aggregate/*.js",
      "public/js/corespring/qti/directives/instructor/*.js"],
    browsers: ['PhantomJS'],

    phantomjsLauncher: {
      // Have phantomjs exit if a ResourceError is encountered (useful if karma exits without killing phantom) 
      exitOnResourceError: false
    }
  })
}