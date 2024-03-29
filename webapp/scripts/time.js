// time.js

(function($){

$(document).ready(function() {
    var now = new Date();
    $(document.getElementsByTagName('time')).each(function() {
        changeDateLocale(this, now);
    });
});
$(document).bind("DOMNodeInserted", handle);

function handle(event) {
    var now = new Date();
    var target = event.target ? event.target : document;
    if (target.getElementsByTagName) {
        var time = $(target.getElementsByTagName('time'));
        if ($(target).is('time')) {
            time = time.add(target);
        }
        time.each(function(i, node) {
            changeDateLocale(node, now);
        });
    }
}

function changeDateLocale(node, now) {
    var node = $(node);
    if (!node.attr("datetime")) {
        var ie8 = false;
        var text = node.text();
        if (!text && node[0].nextSibling && node[0].nextSibling.nodeValue) {
            text = node[0].nextSibling.nodeValue;
            ie8 = true;
        }
        if (!text) return;
        var timestamp = parseDateTime(text, now);
        if (!isNaN(timestamp)) {
            node.attr("content", text);
            node.attr("datetime", text);
            var date = new Date(timestamp);
            if ((date.getHours() > 0 || date.getMinutes() > 0) && /^\s*(\d{4})-?(\d{2})-?(\d{2})\s*$/.exec(text)) {
                date = new Date(parseDateTime(text + "T00:00:00"));
            }
            var formatted = null;
            if (node.is(".abbreviated")) {
                formatted = local(date, now, node);
            } else if (node.is(".datetime")) {
                formatted =  date.toLocaleString();
            } else if (node.is(".datetime-local")) {
                formatted = local(date, now);
            } else if (node.is(".date")) {
                formatted = date.toDateString();
            } else if (node.is(".month")) {
                formatted = month(date);
            } else if (node.is(".year")) {
                formatted = date.getFullYear();
            } else if (node.is(".time")) {
                formatted = date.toLocaleTimeString();
            } else if (text.indexOf('T') < 0) {
                formatted = date.toDateString();
            } else {
                formatted = date.toString();
            }
            if (ie8) {
                node[0].nextSibling.nodeValue = formatted;
            } else {
                node.text(formatted);
            }
        }
    }
}

function parseDateTime(text, now) {
    if (/^\s*([\d]+\-?){1,2}\s*$/.exec(text))
        return NaN;
    var struct = /(?:(\d{4})-?(\d{2})-?(\d{2}))?(?:[T ]?(\d{2}):?(\d{2}):?(\d{2})(?:\.(\d{3,}))?)?(?:(Z)|([+\-])(\d{2})(?::?(\d{2}))?)?/.exec(text);
    if (!struct)
        return Date.parse(text);
    var minutesOffset = 0;
    if (struct[8] !== 'Z' && struct[9]) {
        minutesOffset = +struct[10] * 60 + (+struct[11]);
        if (struct[9] === '+') {
            minutesOffset = 0 - minutesOffset;
        }
    }
    if (struct[1]) {
        if (struct[4] || struct[5] || struct[6]) {
            if (struct[8] || struct[9]) {
                timestamp = Date.UTC(+struct[1], +struct[2] - 1, +struct[3], +struct[4], +struct[5] + minutesOffset, +struct[6], 0);
            } else {
                timestamp = new Date(+struct[1], +struct[2] - 1, +struct[3], +struct[4], +struct[5], +struct[6], 0).getTime();
            }
        } else if (struct[8] || struct[9]) {
            timestamp = Date.UTC(+struct[1], +struct[2] - 1, +struct[3], 0, minutesOffset, 0, 0);
        } else {
            timestamp = new Date(+struct[1], +struct[2] - 1, +struct[3]).getTime();
        }
    } else if (struct[4] || struct[5] || struct[6]) {
        if (struct[8] || struct[9]) {
            timestamp = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate(), +struct[4], +struct[5] + minutesOffset, +struct[6], 0);
        } else {
            timestamp = new Date(now.getFullYear(), now.getMonth(), now.getDate(), +struct[4], +struct[5], +struct[6], 0).getTime();
        }
    }
    return timestamp;
}

function local(date, now, node) {
    var locale = '';
    var minutes = '';
    if (!node || !node.is('.date') && !node.is('.month')) {
        if (date.getMinutes() >= 10) {
            minutes = ':' + date.getMinutes();
        } else if (!node || date.getHours() > 0 || date.getMinutes() > 0) {
            minutes = ':0' + date.getMinutes();
        }
        if (date.getHours() >= 10) {
            locale = date.getHours() + minutes;
        } else if (date.getHours() > 0) {
            locale = "0" + date.getHours() + minutes;
        } else if (minutes) {
            locale = "00" + minutes;
        }
    }
    if (!node || !node.is('.time')) {
        if (!node || locale == '' || date.getDate() != now.getDate() || date.getMonth() != now.getMonth() || date.getFullYear() != now.getFullYear()) {
            if (locale) {
                locale += ", ";
            }
            var month = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"][date.getMonth()];
            if (!node || !node.is('.month')) {
                locale += date.getDate() + ' ';
            }
            locale += month;
            if (!node || date.getFullYear() != now.getFullYear()) {
                locale += ' ' + date.getFullYear();
            }
        }
    }
    return locale;
}

function month(date) {
    var month = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"][date.getMonth()];
    return month + ' ' + date.getFullYear();
}

})(window.jQuery);

