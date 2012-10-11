describe('textentryinteraction', function () {
    'use strict';

    var prepareBackend = function ($backend) {

        var urls = [
            {
                method:'GET',
                url:'/api/v1/items/itemId/sessions/itemSessionId?access_token=34dj45a769j4e1c0h4wb',
                response:{"id":"itemSessionId", "itemId":"itemId", "start":1349970769197, "responses":[]}
            }
        ];

        for (var i = 0; i < urls.length; i++) {
            var definition = urls[i];
            $backend.when(definition.method, definition.url).respond(200, definition.response);
        }
    };

    var assessmentItemWrapper = [
        '<assessmentitem ',
        'cs:feedbackEnabled="true" ',
        'cs:itemSessionId="itemSessionId" ',
        'cs:itemId="itemId" ',
        'print-mode="false" ',
        'cs:noResponseAllowed="true"',
        '>',
        '${contents}',
        '</assessmentitem>'].join("\n");

    var wrap = function (content) {
        return assessmentItemWrapper.replace("${contents}", content);
    };

    var compileAndGetScope = function ($rootScope, $compile, node) {
        $compile(wrap('<textentryinteraction responseIdentifier="rid" expectedLength="1"/>'))($rootScope);
        return $rootScope.$$childHead;
    };


    beforeEach(module('qti'));

    var rootScope, compile;

    beforeEach(inject(function ($compile, $rootScope, _$httpBackend_) {
        prepareBackend(_$httpBackend_);
        rootScope = $rootScope.$new();
        compile = $compile;
    }));


    it('inits correctly', function () {
        var node = '<textentryinteraction responseIdentifier="rid" expectedLength="1"/>';
        var scope = compileAndGetScope(rootScope, compile, node);
        expect(scope.expectedLength).toBe('1');
    });

    it('interacts with controller', function () {
        var node = '<textentryinteraction responseIdentifier="rid" expectedLength="1"/>';
        var scope = compileAndGetScope(rootScope, compile, node);

        var response = "here's a response";
        scope.$apply(function () {
            scope.textResponse = response;
        });

        expect(scope.textResponse).toBe(response);
        expect(scope.controller.findItemByKey("rid").value).toBe(response);
    });

    it('shows/hides no response feedback', function () {
        var node = '<textentryinteraction responseIdentifier="rid" expectedLength="1"/>';
        var scope = compileAndGetScope(rootScope, compile, node);
        scope.textResponse = "";
        scope.$apply(function () {
            scope.showNoResponseFeedback = true;
        });

        expect(scope.noResponse).toBe(true);
    });
});