require(['jquery', 'kbaseUserJobState', 'kbaseLogin', 'jquery.cookie'], function($) {
    $(function() {

        $(document).on('loggedOut.kbase', function(event, token) {
            console.debug("logged out")
            $('#job-state-container').kbaseUserJobState('refresh');
        });

        // Function that sets a cookie compatible with the current narrative
        // (it expects to find user_id and token in the cookie)
        var set_cookie = function() {
            var c = $("#login-widget").kbaseLogin('get_kbase_cookie');
            console.log( 'Setting kbase_session cookie');
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

        $('#job-state-container').kbaseUserJobState();

        var loginWidget = $("#login-widget").kbaseLogin({ 
            style: "narrative",
            rePrompt: false,

            login_callback: function(args) {
                set_cookie();
                $('#job-state-container').kbaseUserJobState('setAuth', args);
            },

            logout_callback: function(args) {
                $.removeCookie('kbase_session');
                $('#job-state-container').kbaseUserJobState('setAuth');
            },

            prior_login_callback: function(args) {
                set_cookie();
                $('#job-state-container').kbaseUserJobState('setAuth', args);
            },
        });
    });

});