var host = StackPtrAndroidShim.get_host().split("//");
stackptr_server_base_host = host[1].split("/")[0];
stackptr_server_base_protocol = host[0];

stackptr_apikey = StackPtrAndroidShim.get_apikey();

stackptr_connection_failed = function(reason, details) {
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