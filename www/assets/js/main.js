var kbasePath = "../external/kbase/js/";

requirejs.config({
	baseUrl: 'assets/js',
	paths: {
		'jquery': '../external/jquery/1.10.2/jquery-1.10.2.min',
		'kbwidget': kbasePath + 'kbwidget',
		'kbaseLogin': kbasePath + 'kbaseLogin',
		'kbasePrompt': kbasePath + 'kbasePrompt',
		'bootstrap': '../external/bootstrap/js/bootstrap.min',
		'header': kbasePath + '/header',
		'jquery.cookie': '../external/jquery.cookie.min',
		'userandjobstate': kbasePath + 'userandjobstate',
		'jquery.dataTables': '../external/dataTables/1.9.4/js/jquery.dataTables',
	},
	shim: {
		'kbwidget': ['jquery'],
		'header': ['jquery'],
		'kbasePrompt': ['jquery', 'kbwidget', 'bootstrap'],
		'kbaseLogin': ['jquery', 'kbwidget', 'kbasePrompt', 'bootstrap'],
		'jquery.cookie': ['jquery'],
		'jquery.dataTables': ['jquery']
	}
});

requirejs(['header', 'userJobStateTest']);