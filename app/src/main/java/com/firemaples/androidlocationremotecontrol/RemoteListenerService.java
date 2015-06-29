package com.firemaples.androidlocationremotecontrol;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.io.IOException;
import java.net.InetSocketAddress;

public class RemoteListenerService extends Service {
    private final IBinder mBinder = new RemoteListenerBinder();
    private OnRemoteListenerCallback callback;
    private boolean socketConnected = false;
    private WebSocket mWebSocketClient;
    private int port = 8888;
    private String socketName = "/live";

    private SimpleSocketServer socketServer;

    public RemoteListenerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setCallback(OnRemoteListenerCallback callback) {
        this.callback = callback;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startListening();
        return super.onStartCommand(intent, flags, startId);
    }

    public void startListening() {
        socketServer = new SimpleSocketServer(this, new InetSocketAddress(port), socketName, socketServerCallback);
        socketServerRunTask.execute();

        if (callback != null) {
            callback.onServicePrepared(this);
        }
    }


    public int getPort() {
        return port;
    }

    public String getSocketName() {
        return socketName;
    }

    public WebSocket getmWebSocketClient() {
        return mWebSocketClient;
    }

    private AsyncTask<Void, Void, Void> socketServerRunTask = new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
            socketServer.run();
            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            try {
                socketServer.stop();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    private SimpleSocketServer.OnSocketServerCallback socketServerCallback = new SimpleSocketServer.OnSocketServerCallback() {
        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            Utils.makeTestLog(RemoteListenerService.this, "Server get a client");
            if (socketConnected) {
                Utils.makeTestLog(RemoteListenerService.this, "Server server refuse a client because other client is connected");
                return;
            }

            socketConnected = true;
            mWebSocketClient = conn;

            if (callback != null) {
                callback.onConnected(RemoteListenerService.this);
            }
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            socketConnected = false;

            if (callback != null) {
                callback.onDisconnected(RemoteListenerService.this);
            }
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            Utils.makeTestLog(RemoteListenerService.this, "Get a message from client: " + message);
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {

        }
    };

    public class RemoteListenerBinder extends Binder {
        RemoteListenerService getService() {
            return RemoteListenerService.this;
        }
    }

    public interface OnRemoteListenerCallback {
        void onServicePrepared(RemoteListenerService service);

        void onConnected(RemoteListenerService service);

        void onDisconnected(RemoteListenerService service);
    }
}
