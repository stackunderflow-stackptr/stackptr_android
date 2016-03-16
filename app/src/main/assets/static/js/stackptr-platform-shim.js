stackptr_android = true;
var host = StackPtrAndroidShim.get_host().split("//");
stackptr_server_base_host = host[1].split("/")[0];
stackptr_server_base_protocol = host[0];

$('head').append($("<link>", {rel: 'stylesheet', type: 'text/css', href: 'static/css/stackptr_android.css'}));

stackptr_apikey = StackPtrAndroidShim.get_apikey();

stackptr_connection_failed = function(reason, details) {
    console.log("connection failed - reason: " + reason + " details: " + details);
    if (reason != "lost" && reason != "closed") {
        StackPtrAndroidShim.showLogin();
    }
}

stackptr_share_user = function(obj) {
    var lat = obj.loc[0];
    var lon = obj.loc[1];
    var name = obj.username;

    StackPtrAndroidShim.shareLocation(lat,lon,name);
}

function StackPtrBackPressed() {
    StackPtrAndroidShim.doBackPressed();
}