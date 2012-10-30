describe('Login/Logut', function () {

    it('should not login with invalid un / pw', function () {
        browser().navigateTo('/web/login');
        element('#username').val("nothomer");
        element('#password').val("aa");
        element('#loginbutton').click();
        expect(browser().window().path()).toBe('/web/login');
    });

    it('should login with valid un / pw', function () {
        browser().navigateTo('/web/login');
        element('#username').val("homer");
        element('#password').val("aa");
        element('#loginbutton').click();
        sleep(3);
        expect(browser().window().path()).toBe('/web');
    });


    it('should do something', function () {
        browser().navigateTo('/example-content');
        expect(repeater('div').count()).toBe(158);
    });

    it('should do log out', function () {
        browser().navigateTo('/web/logout');
    });
});

describe('Item Preview Page', function () {
    it('item preview should default to profile page', function () {
        browser().navigateTo('/web/item-preview/50083ba9e4b071cb5ef79101');
        expect(element('#itemView').css('display')).toBe('none');
        expect(element('#profileView').css('display')).toBe('block');
    });

    it('opening item should display player', function () {
        element("a:contains('Item')").click();
        expect(element('#profileView').css('display')).toBe('none');
        expect(element('#itemView').css('display')).toBe('block');
    });


});

describe('Item Player', function () {
    it('should not be able to submit when radio not selected', function () {
        browser().navigateTo('/testplayer/item/506c9fbda0eee22b21788986/run?access_token=34dj45a769j4e1c0h4wb');
        element("a:contains('Submit')").click();
        expect(element("div.choice-interaction").attr('class')).toContain("noResponse");
    });

    it('Should click', function () {
        element("input[value='obama']").click();
        element("a:contains('Submit')").click();
        expect(element("simplechoice[identifier='obama']").attr('class')).toContain("incorrect-response");
        expect(element("simplechoice[identifier='calderon']").attr('class')).toContain("correct-response");
    });

});
