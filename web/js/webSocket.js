var div_apiKey = $("#div_apiKey"),
    div_unconnected = $("#div_unconnected"),
    div_connected = $("#div_connected"),
    div_mapBody = $("#div_mapBody"),
    tv_connectionInfo = $("#tv_connectionInfo"),
    tv_ApiKey = $("#tv_ApiKey"),
    tv_connectionIp = $("#tv_connectionIp"),
    tv_connectionPort = $("#tv_connectionPort");
    
var KEY_MAP_API = "apiKey",
    KEY_WS_IP = "wsIp",
    KEY_WS_PORT = "wsPort";
    
var webSocket;
var key;
var isSupportLocalStorage = typeof (Storage) != "undefined";

$(function () {
    // Material Design for Bootstrap https://github.com/FezVrasta/bootstrap-material-design
    $.material.init();
    tv_connectionIp.mask('099.099.099.099');

    //For clear error message
    alert(null);

    div_mapBody.hide();
    div_connected.hide();
    div_unconnected.hide();

    tv_ApiKey.val(getItem(KEY_MAP_API));
    tv_connectionIp.val(getItem(KEY_WS_IP));
    tv_connectionPort.val(getItem(KEY_WS_PORT))
    
    if(tv_ApiKey.val().length == 0 && location.href.match("^http://web-firemaples.rhcloud.com/web/AndroidLocationRemoteControl/")){
        tv_ApiKey.val("AIzaSyDvzXUq7BpqMgVu960kL854aJQsH0U1Kfw");
    }
    if(tv_connectionPort.val().length == 0){
        tv_connectionPort.val("8888");
    }

    $("#bt_apiKeySubmit").bind("click", function () {
        //For clear error message
        alert(null);
        key = tv_ApiKey.val();

        if (key == null || key.length == 0) {
            alert("Please input your Google Api Key");
        } else {
            setItem(KEY_MAP_API, key);
            div_apiKey.hide();
            div_unconnected.show();
        }
    });

    $("#bt_connect").bind("click", function () {
        //For clear error message
        alert(null);
        if(tv_connectionIp.val().length == 0){
            alert("Please input your device connection ip from the app");
        }else if(tv_connectionPort.val.length == 0){
            alert("Please input your device connection port from the app");
        }else if(tv_connectionIp.val().match(/^(?!0)(?!.*\.$)((1?\d?\d|25[0-5]|2[0-4]\d)(\.|$)){4}$/) == null){
            alert("Invalid IP address");
        }else{
            setItem(KEY_WS_IP, tv_connectionIp.val());
            setItem(KEY_WS_PORT, tv_connectionPort.val());
            connect();
        }
    });

    $("#bt_disconnect").bind("click", function () {
        //For clear error message
        alert(null);
        webSocket.close();
    });
});

//Connect to app
function connect() {
    var url = "ws://"+tv_connectionIp.val()+":"+tv_connectionPort.val()+"/alrc";

    webSocket = new WebSocket(url);
    webSocket.onopen = function (evt) {
        console.log("Socket onopen");

        loadGoogleMapApi(key, function () {
            div_unconnected.hide();
            div_connected.show();
            div_mapBody.show();

            tv_connectionInfo.html("Connected to server: " + evt.target.url);
        });
    };
    webSocket.onclose = function (evt, code, reason, remote) {
        console.log("Socket onclose");

        div_unconnected.show();
        div_connected.hide();
        div_mapBody.hide();
    };
    webSocket.onerror = function (evt) {
        console.log("Socket onerror:" + evt.data);
        alert("Connection failed: " + evt.data + " ,please recheck the ip address and port number is correct.")
    };
    webSocket.onmessage = function (evt) {
        console.log("Socket onmessage:" + evt.data);
    };
}

function sendMsg(msg) {
    console.log("Send message: " + msg);
    webSocket.send(msg);
}

//Show alert message
function alert(msg) {
    var tv_errorMsg = $("#tv_errorMsg")
    if (msg == null) {
        tv_errorMsg.hide();
    } else {
        tv_errorMsg.html(msg);
        tv_errorMsg.show();
    }
}

function getItem(key) {
    if (isSupportLocalStorage) {
        return localStorage.getItem(key);
    } else {
        return null;
    }
}

function setItem(key, value) {
    if (isSupportLocalStorage) {
        return localStorage.setItem(key, value);
    }
}