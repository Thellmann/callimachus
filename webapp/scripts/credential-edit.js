// credential-edit.js

jQuery(function($){

$('form[typeof~="calli:Credential"]').submit(function(event){
    var form = this;
    var resource = $(form).attr('resource');
    var password = $('#password').val();
    if (password) {
        event.preventDefault();
        $.ajax({
            type: 'POST',
            url: resource + '?password',
            contentType: 'text/plain',
            data: rstr2b64(str2rstr_utf8(password)),
            xhrFields: calli.withCredentials,
            dataType: "text",
            success: function(url) {
                $('#password').val('');
                $(form).submit();
            }
        });
    }
});

});
