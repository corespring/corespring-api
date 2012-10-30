(function(){

    window.com = (window.com || {});
    com.qti = (com.qti || {});
    com.qti.helpers = (com.qti.helpers || {});

    com.qti.helpers.QtiHelper = function(){
        var that = this;
        this.assessmentItemWrapper = [
            '<assessmentitem ',
            'cs:feedbackEnabled="true" ',
            'cs:itemSessionId="itemSessionId" ',
            'cs:itemId="itemId" ',
            'print-mode="false" ',
            'cs:noResponseAllowed="true"',
            '>',
            '${contents}',
            '</assessmentitem>'].join("\n");

        this.wrap = function (content) {
            return this.assessmentItemWrapper.replace("${contents}", content);
        };

        this.prepareBackend = function ($backend) {

            var urls = [
                {
                    method:'GET',
                    url:'/api/v1/items/itemId/sessions/itemSessionId',
                    response:{"id":"itemSessionId", "itemId":"itemId", "start":1349970769197, "responses":[]}
                }
            ];

            for (var i = 0; i < urls.length; i++) {
                var definition = urls[i];
                $backend.when(definition.method, definition.url).respond(200, definition.response);
            }
        };

        this.compileAndGetScope = function ($rootScope, $compile, node) {
            var element = $compile(that.wrap(node))($rootScope);
            return { element:element.children(), scope:$rootScope.$$childHead};
        };

    };

})();

