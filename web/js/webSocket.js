$(function () {
    var webSocket;
    var div_unconnected = $("#div_unconnected"), div_connected = $("#div_connected"),
        tv_connectionInfo = $("#tv_connectionInfo"), tv_errorMsg = $("#tv_errorMsg")

    div_connected.hide();

    $("#bt_connect").bind("click", function () {
        tv_errorMsg.html(null);
        connect();
    });

    $("#bt_disconnect").bind("click", function () {
        webSocket.close();
    });

    function connect() {
        var url = $("#ip_connectionIp").val();
        var protocol = $("#ip_protocol").val();
        webSocket = new WebSocket(url);
        webSocket.onopen = function (evt) {
            console.log("Socket onopen");

            div_unconnected.hide();
            div_connected.show();

            tv_connectionInfo.html("Connected to server: " + evt.target.url);
        };
        webSocket.onclose = function (evt, code, reason, remote) {
            console.log("Socket onclose");

            div_unconnected.show();
            div_connected.hide();
        };
        webSocket.onerror = function (evt) {
            console.log("Socket onerror:" + evt.data);

            tv_errorMsg.html(evt);
        };
        webSocket.onmessage = function (evt) {
            console.log("Socket onmessage:" + evt.data);
        };
    }


});