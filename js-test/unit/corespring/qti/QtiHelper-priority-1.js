(function(){

    window.com = (window.com || {});
    com.qti = (com.qti || {});
    com.qti.helpers = (com.qti.helpers || {});

    com.qti.helpers.QtiHelper = function(){
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
    };

})();