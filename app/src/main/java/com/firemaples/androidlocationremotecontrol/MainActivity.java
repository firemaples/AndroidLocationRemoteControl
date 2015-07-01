package com.firemaples.androidlocationremotecontrol;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {
    private RemoteListenerService mService;
    private boolean mBounds = false;

    private TextView tv_state, tv_position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(this, RemoteListenerService.class);
        bindService(intent, mServiceConn, Context.BIND_AUTO_CREATE);

        setViews();
    }

    private void setViews() {
        tv_state = (TextView) findViewById(R.id.tv_state);
        tv_position = (TextView) findViewById(R.id.tv_position);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mService.stopListening();
        unbindService(mServiceConn);
    }

    private ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RemoteListenerService.RemoteListenerBinder mBinder = (RemoteListenerService.RemoteListenerBinder) service;
            mService = mBinder.getService();
            mBounds = true;

            mService.setCallback(serviceCallback);
            mService.startListening();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBounds = false;
        }
    };

    private RemoteListenerService.OnRemoteListenerCallback serviceCallback = new RemoteListenerService.OnRemoteListenerCallback() {
        @Override
        public void onServicePrepared(RemoteListenerService service) {
            showWaitingString(service);
        }

        @Override
        public void onConnected(final RemoteListenerService service) {
            final String msg = String.format(getString(R.string.msg_connectedByClient), service.getWebSocketClient().getRemoteSocketAddress().getHostName());
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv_state.setText(msg);
                }
            });
        }

        @Override
        public void onDisconnected(final RemoteListenerService service) {
            showWaitingString(service);
        }

        @Override
        public void onMockLocationChange(RemoteListenerService service, double lat, double lng) {
            final String msg = String.format(getString(R.string.msg_positionText), lat, lng);
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv_position.setText(msg);
                }
            });
        }

        private void showWaitingString(RemoteListenerService service) {
            WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
            String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

            Utils.makeTestLog(MainActivity.this, "Current ip: " + ip);

            final String msg = String.format(getString(R.string.msg_waitingForClient), ip, service.getPort(), service.getSocketName());

            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv_state.setText(msg);
                    tv_position.setText(null);
                }
            });
        }
    };
//
//    private void testConnection() {
//        AsyncHttpClient.getDefaultInstance().websocket("ws://192.168.14.101:8888/live", "", new AsyncHttpClient.WebSocketConnectCallback() {
//
//            @Override
//            public void onCompleted(Exception ex, WebSocket webSocket) {
//                Utils.makeTestLog(MainActivity.this, "Client connection completed");
//                webSocket.send("Test Send!!!");
//            }
//        });
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
