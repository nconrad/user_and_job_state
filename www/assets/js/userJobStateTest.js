require(['jquery', 'kbaseUserJobState', 'kbaseLogin', 'jquery.cookie'], function($) {
    $(function() {

        // Function that sets a cookie compatible with the current narrative
        // (it expects to find user_id and token in the cookie)
        var set_cookie = function() {
            var c = $("#login-widget").kbaseLogin('get_kbase_cookie');
            $.cookie('kbase_session',
                     'un=' + c.user_id
                     + '|'
                     + 'kbase_sessionid=' + c.kbase_sessionid
                     + '|'
                     + 'user_id=' + c.user_id
                     + '|'
                     + 'token=' + c.token.replace(/=/g, 'EQUALSSIGN').replace(/\|/g,'PIPESIGN'),
                     { path: '/',
                       domain: 'kbase.us' });
            $.cookie('kbase_session',
                     'un=' + c.user_id
                     + '|'
                     + 'kbase_sessionid=' + c.kbase_sessionid
                     + '|'
                     + 'user_id=' + c.user_id
                     + '|'
                     + 'token=' + c.token.replace(/=/g, 'EQUALSSIGN').replace(/\|/g,'PIPESIGN'),
                     { path: '/'});
        };

        var loginWidget = $("#signin-button").kbaseLogin({ 
            rePrompt: false,

            login_callback: function(args) {
                set_cookie();
//                $('#job-state-container').kbaseUserJobState('setAuth', args);
            },

            logout_callback: function(args) {
                $.removeCookie('kbase_session');
//                $('#job-state-container').kbaseUserJobState('setAuth');
            },

            prior_login_callback: function(args) {
                set_cookie();
//                $('#job-state-container').kbaseUserJobState('setAuth', args);
            },
        });
        $('#signin-button').css('padding', '0');
        $('#job-state-container').kbaseUserJobState();

    });

});