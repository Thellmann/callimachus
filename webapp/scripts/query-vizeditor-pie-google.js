
/* query-vizeditor-pie-google.js */

(function($) {
    
    var pLib = window.queryviz;
    var moduleName = 'pie-google';
    
    var lib = pLib[moduleName] = {
        
        getConfig: function() {
            var config = pLib.getConfig();
            config.options = {
                'title': config.options['title'],
                'mode': config.options['mode'],
                'legend': config.options['legend']
            }
            return config;
        },

        renderPreview: function() {
            lib.renderModule(pLib.getConfig());
        },
        
        renderModule: function(config) {
            $.getScript('//www.google.com/jsapi', function() {
                google.load('visualization', '1.0', {packages: ['geochart'], callback: function() {
                    // viz container and styling
                    var container = $('#' + config.vizId);
                    var containerHeight = (window == top) ? $(window).height() / 2 : $(window).height() - 30; // larger in iframe embeds
                    container.width('100%').height(containerHeight).css('overflow', 'hidden').html('');
                    // data source
                    var queryUrl = location.pathname;
                    if (!queryUrl.match(/\.rq$/)) {// context is not the query view but the chart page, use config
                        queryUrl = config.query;
                    }
                    queryUrl += '?results&' + $.param(config.params) + location.search.replace('?view', '&') + '&tqx=out:table';
                    // chart options
                    var options = {
                        width: '100%',
                        title: config.options['title'] || '',
                        is3D: config.options['mode'] && config.options['mode'] == '3d' ? true : false,
                        pieHole: config.options['mode'] && config.options['mode'] == 'donut' ? 0.3 : 0,
                        legend: {position: config.options['legend'] || 'none'}
                    };
                    var chart = new google.visualization.ChartWrapper({
                        chartType: "PieChart",
                        containerId: config.vizId,
                        dataSourceUrl: queryUrl,
                        options: options
                    });
                    chart.draw();                
                }});
            });
        },

        createOptionsPane: function() {
            $('#calli-viz-editor .options-pane').each(function() {
                $(this).append('<h3>Chart Options</h3>');
                lib.createOptionsMarkup($(this));
                pLib.createParamsMarkup($(this), pLib.config ? pLib.config.params : null);
            });
        },
        
        createOptionsMarkup: function(container) {
            $('<form class="calli-viz-options form-horizontal" action="#" method="post"></form>')
                .appendTo(container)
                .each(function() {
                    var container = $(this);
                    pLib.createHiddenOption(container, 'module', moduleName);
                    pLib.createInfoOption(container, 'The first field in the query results should contain the slice label. The second field should be a number.');
                    pLib.createTextOption(container, 'title', 'Chart title', '');
                    pLib.createSelectOption(container, 'mode', 'Display mode', ['pie', '3d', 'donut']);
                    pLib.createSelectOption(container, 'legend', 'Legend', ['left', 'right', 'top', 'bottom', 'none']);
                })
            ;
        },

        onOptionChange: function() {
            lib.renderPreview();
        },
        
        onParamChange: function() {
            lib.renderPreview();
        },
        
        run: function() {
            lib.createOptionsPane();
            lib.renderPreview();
        },
        
        /**
         * Initializes the module.
         */ 
		init: function() {
		}
	
	};
	
	$(lib.init);	
 	
})(jQuery);
