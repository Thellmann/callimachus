// pdf-edit.js

jQuery(function($){
    bindDrag($('#pages a'));

    function bindDrag(element) {
        var hash = $('body').attr('resource') + '#';
        element.find('button.remove').bind('click', function(e){
            if (e.preventDefault) {
                e.preventDefault();
            }
            $(this).closest('a').remove();
        });
        element.bind('click', function(e){
            if (e.preventDefault) {
                e.preventDefault();
            }
        }).bind('dragstart', function(e){
            var img = $(this).find('img')[0];
            e.dataTransfer.setDragImage(img, img.width / 2, img.height / 2);
            e.dataTransfer.setData('text/uri-list', this.href);
            e.dataTransfer.setData('text/plain', this.href);
        }).bind('dragenter', function(e) {
            $(this).addClass('dragover');
        }).bind('dragover', function(e) {
            if (e.preventDefault) {
                e.preventDefault();
            }
            var url = e.dataTransfer.getData('text/uri-list');
            if (url) {
                if (url.indexOf(hash) === 0) {
                    e.dataTransfer.dropEffect = 'move';
                } else {
                    e.dataTransfer.dropEffect = 'copy';
                }
            }
        }).bind('dragleave', function(e) {
            $(this).removeClass('dragover');
        }).bind('drop', function(e) {
            $(this).removeClass('dragover');
            var url = e.dataTransfer.getData('text/uri-list');
            if (url) {
                if (e.preventDefault) {
                    e.preventDefault();
                }
                if (e.stopPropagation) {
                    e.stopPropagation();
                }
                if (this.href != url) {
                    var a = createPage(url);
                    // element position
                    var targetPosition = $(this).index();
                    var sourcePosition;
                    $('#pages a').each(function(){
                        if (this.href == url) {
                            sourcePosition = $(this).index();
                            $(this).remove();
                        }
                    });
                    if (sourcePosition < targetPosition) {
                        $(this).after(a);
                    } else {
                        $(this).before(a);
                    }
                    bindDrag(a);
                    // align calli:pageNumber content
                    $('#pages span[property="calli:pageNumber"]').each(function(index) {
                        $(this).text(1 + index);
                    });
                }
            }
        }).bind('dragend', function(e) {
            $('#pages .dragover').removeClass('dragover');
        });
    }

    function createPage(url) {
        // PDPage
        var a = $('<a></a>');
        a.attr('class', "col-xs-6 col-sm-4 col-md-3 col-lg-2");
        a.attr('rel', "calli:hasPage");
        a.attr('dropzone', "copy move s:text/uri-list");
        // calli:pageNumber
        var span = $('<span></span>');
        span.attr('property', "calli:pageNumber");
        span.attr('class', "asc hide");
        a.append(span);
        // remove button
        var button = $('<button></button>');
        button.attr('class', "remove btn btn-link glyphicon glyphicon-remove");
        a.append(button);
        // foaf:depiction
        var src = getImageSrc(url);
        var img = $('<img/>');
        img.attr('draggable', "false");
        img.attr('rel', "foaf:depiction");
        img.attr('resource', src);
        img.attr('src', src);
        a.append(img);
        // prov:wasQuotedFrom
        var hash = $('body').attr('resource') + '#';
        var pageId = url;
        if (url.indexOf(hash) !== 0) {
            pageId = hash + '!' + url;
        }
        a.attr('href', pageId);
        if (pageId.indexOf('#!') > 0) {
            var wasQuotedFrom = $('<span></span>');
            wasQuotedFrom.attr('property', "prov:wasQuotedFrom");
            wasQuotedFrom.attr('resource', pageId.substring(pageId.indexOf('#!') + 2));
            a.append(wasQuotedFrom);
        }
        return a;
    }
    function getImageSrc(url) {
        if (url.indexOf('#!') > 0) {
            url = url.substring(url.indexOf('#!') + 2);
        }
        var h = url.indexOf('#');
        var base = url.substring(0, h);
        var id = url.substring(h + 1);
        return base + '?thumbnail=' + encodeURIComponent(id);
    }
});