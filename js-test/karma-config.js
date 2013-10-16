basePath = '..';
// list of files / patterns to load in the browser
files = [
    JASMINE,
    JASMINE_ADAPTER,
    'js-test/lib/jquery-1.7.2.min.js',
    'js-test/lib/jquery-ui.js',
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
    'public/js/corespring/**/*.js',
    'js-test/lib/play.mock.routes.js',
    'js-test/unit/**/*.js'
];
// list of files to exclude
exclude = ["public/js/corespring/qti/directives/printing/*.js",
  "public/js/corespring/qti/directives/aggregate/*.js",
  "public/js/corespring/qti/directives/instructor/*.js"];

// use dots reporter, as travis terminal does not support escaping sequences
// possible values: 'dots' || 'progress'
reporter = 'dots';
browsers = ['PhantomJS'];
// these are default values, just to show available options
// web server port
port = 8080;
// cli runner port
runnerPort = 9100;
// enable / disable colors in the output (reporters and logs)
colors = true;
// level of logging
// possible values: LOG_DISABLE || LOG_ERROR || LOG_WARN || LOG_INFO || LOG_DEBUG
logLevel = LOG_ERROR;
// enable / disable watching file and executing tests whenever any file changes
autoWatch = true;
// polling interval in ms (ignored on OS that support inotify)
autoWatchInterval = 0;
singleRun = false;
