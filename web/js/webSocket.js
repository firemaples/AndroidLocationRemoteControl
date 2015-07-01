var div_apiKey = $("#div_apiKey"),
    div_unconnected = $("#div_unconnected"),
    div_connected = $("#div_connected"),
    div_mapBody = $("#div_mapBody"),
    tv_connectionInfo = $("#tv_connectionInfo");
var webSocket;
var key;
var isSupportLocalStorage = typeof (Storage) != "undefined";
$(function () {
    // Material Design for Bootstrap https://github.com/FezVrasta/bootstrap-material-design
    $.material.init();

    alert(null);

    div_connected.hide();
    div_unconnected.hide();

    $("#tv_ApiKey").val(getItem("apiKey"));
    $("#tv_connectionIp").val(getItem("wsIp"));

    $("#bt_apiKeySubmit").bind("click", function () {
        alert(null);
        key = $("#tv_ApiKey").val();
        setItem("apiKey", key);

        if (key == null || key.length == 0) {
            alert("Please input your API KEY");
        } else {
            div_apiKey.hide();
            div_unconnected.show();
        }
    });

    $("#bt_connect").bind("click", function () {
        alert(null);
        connect();
    });

    $("#bt_disconnect").bind("click", function () {
        alert(null);
        webSocket.close();
    });
});

//Connect to app
function connect() {
    var url = $("#tv_connectionIp").val();
    setItem("wsIp", url);

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

        alert(evt.data)
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