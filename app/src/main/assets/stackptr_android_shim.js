var host = StackPtrAndroidShim.get_host().split("//");
stackptr_server_base_host = host[1].split("/")[0];
stackptr_server_base_protocol = host[0];

stackptr_apikey = StackPtrAndroidShim.get_apikey();