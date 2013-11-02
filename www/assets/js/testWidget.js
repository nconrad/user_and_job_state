define(['jquery', 'kbwidget'], function($) {
    $.KBWidget({
        name: "TestWidget",
        options: {},
        init: function(options) {
            this._super(options);
            this.$elem.append($('<div>')
                              .append('woot!')
                              .css({'color' : 'red'}));
            return this;
        },
    });
});