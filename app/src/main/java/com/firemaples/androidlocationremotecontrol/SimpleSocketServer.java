package com.firemaples.androidlocationremotecontrol;

import android.content.Context;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

/**
 * Created by Louis on 2015/6/29.
 */
public class SimpleSocketServer extends WebSocketServer {
    private final Context context;
    private final String socketName;
    private final OnSocketServerCallback callback;

    public SimpleSocketServer(Context context, InetSocketAddress address, String socketName, OnSocketServerCallback callback) {
        super(address);
        this.context = context;
        if (socketName == null || socketName.isEmpty()) {
            socketName = "/";
        } else if (!socketName.startsWith("/")) {
            socketName = "/" + socketName;
        }
        this.socketName = socketName;
        this.callback = callback;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Utils.makeTestLog(context, "onOpen");
        if (handshake.getResourceDescriptor().equals(socketName)) {
            callback.onOpen(conn, handshake);
        } else {
            conn.closeConnection(404, context.getString(R.string.msg_socketNameNotFound));
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        if (code == 404) return;
        Utils.makeTestLog(context, "onClose");
        callback.onClose(conn, code, reason, remote);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        Utils.makeTestLog(context, "onMessage:" + message);
        callback.onMessage(conn, message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        Utils.makeTestLog(context, "onError:" + ex.getMessage());
        callback.onError(conn, ex);
    }

    public interface OnSocketServerCallback {
        void onOpen(WebSocket conn, ClientHandshake handshake);

        void onClose(WebSocket conn, int code, String reason, boolean remote);

        void onMessage(WebSocket conn, String message);

        void onError(WebSocket conn, Exception ex);
    }
}
