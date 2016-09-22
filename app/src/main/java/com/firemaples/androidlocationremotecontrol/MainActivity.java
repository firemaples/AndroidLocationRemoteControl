package com.firemaples.androidlocationremotecontrol;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.commons.lang3.ArrayUtils;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private final int REQUEST_CODE_REQUEST_LOCATION_PERMISSION = 101;

    private final String FORMAT_POSITION = "%f, %f";
    private RemoteListenerService mService;

    private TextView tv_ip, tv_port;
    private TextView tv_connectionInfo, tv_position;
    private View bt_disconnect;
    private View view_unconnected, view_connected;

    private GoogleMap googleMap;
    private Marker marker;

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mService.disconnect();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setViews();
    }

    private void setViews() {
        tv_ip = (TextView) findViewById(R.id.tv_ip);
        tv_port = (TextView) findViewById(R.id.tv_port);

        tv_connectionInfo = (TextView) findViewById(R.id.tv_connectionInfo);
        tv_position = (TextView) findViewById(R.id.tv_position);

        bt_disconnect = findViewById(R.id.bt_disconnect);
        bt_disconnect.setOnClickListener(onClickListener);

        view_unconnected = findViewById(R.id.view_unconnected);
        view_connected = findViewById(R.id.view_connected);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    public void onBackPressed() {
        if (mService != null) {
            mService.disconnect();
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            mService.disconnect();
            mService.stopListening();
            unbindService(mServiceConn);
            stopService(new Intent(this, RemoteListenerService.class));
        }
    }

    private ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RemoteListenerService.RemoteListenerBinder mBinder = (RemoteListenerService.RemoteListenerBinder) service;
            mService = mBinder.getService();

            mService.setCallback(serviceCallback);
            mService.startListening();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
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
                    view_unconnected.setVisibility(View.GONE);
                    view_connected.setVisibility(View.VISIBLE);
                    tv_position.setText(null);
                    tv_connectionInfo.setText(msg);
                }
            });
        }

        @Override
        public void onDisconnected(final RemoteListenerService service) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (marker != null) {
                        marker.remove();
                        marker = null;
                    }
                    view_unconnected.setVisibility(View.VISIBLE);
                    view_connected.setVisibility(View.GONE);
                }
            });
            showWaitingString(service);
        }

        @Override
        public void onMockLocationChange(RemoteListenerService service, final double lat, final double lng) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (googleMap != null) {
                        LatLng latLng = new LatLng(lat, lng);
                        googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                        if (marker == null) {
                            marker = googleMap.addMarker(new MarkerOptions().position(latLng));
                        } else {
                            marker.setPosition(latLng);
                        }
                    }
                    tv_position.setText(String.format(FORMAT_POSITION, lat, lng));
                }
            });
        }

        @Override
        public void onMockModeStartingFailed(String message) {
            AlertDialog.Builder ab = new AlertDialog.Builder(MainActivity.this);
            ab.setTitle("Error");
            ab.setMessage(message);
            ab.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });
            ab.setCancelable(false);
            ab.show();
        }

        private void showWaitingString(RemoteListenerService service) {
            new ShowConnectionInfoThread(service).start();
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

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//
//        if (id == R.id.action_settings) {
//
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        MainActivityPermissionsDispatcher.checkAllNeededPermissionWithCheck(this);
    }

    @NeedsPermission({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    protected void checkAllNeededPermission() {
        boolean mockSettingsON = Utils.isMockSettingsON(this);
        boolean areThereMockPermissionApps = Utils.areThereMockPermissionApps(this);
//        if (!mockSettingsON || !areThereMockPermissionApps) {
//            onNotSetToMockApp();
//            return;
//        }

        Intent intent = new Intent(this, RemoteListenerService.class);
        bindService(intent, mServiceConn, Context.BIND_AUTO_CREATE);

        initGoogleMap();
    }

    @OnPermissionDenied({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    @OnNeverAskAgain({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    protected void onPermissionNotReady() {
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setTitle("Permission needed!");
        ab.setMessage("Some permissions are needed for running this app, try to request permission again by click ok button or close this app by click cancel");
        ab.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                MainActivityPermissionsDispatcher.checkAllNeededPermissionWithCheck(MainActivity.this);
            }
        });
        ab.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        ab.setCancelable(false);
        ab.show();
    }

    protected void onNotSetToMockApp() {
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setTitle("Mock location not open");
        ab.setMessage("Please open mock location service and set this app in mock location application, click ok to open mock location setting page or click close to close this app");
        ab.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
            }
        });
        ab.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        ab.setCancelable(false);
        ab.show();
    }

    protected void initGoogleMap() {
//        hideLocationPermissionErrorMsg();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        googleMap.setMyLocationEnabled(true);
        UiSettings uiSettings = googleMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setMapToolbarEnabled(true);

        googleMap.moveCamera(CameraUpdateFactory.zoomTo(17));
    }

//    @OnPermissionDenied({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
//    @OnNeverAskAgain({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
//    protected void showDeniedForLocation() {
//        Toast.makeText(this, R.string.permission_location_denied, Toast.LENGTH_LONG).show();
//        showLocationPermissionErrorMsg(R.string.permission_location_denied, R.string.request_permission_again);
//    }
//
//    private void showLocationPermissionErrorMsg(int msgRes, int btnTextRes) {
//        findViewById(R.id.view_locationNonPermissionWrapper).setVisibility(View.VISIBLE);
//        findViewById(R.id.map).setVisibility(View.GONE);
//        ((TextView) findViewById(R.id.tv_locationNonPermissionMsg)).setText(msgRes);
//        ((Button) findViewById(R.id.bt_requestLocationPermission)).setText(btnTextRes);
//    }
//
//    private void hideLocationPermissionErrorMsg() {
//        findViewById(R.id.view_locationNonPermissionWrapper).setVisibility(View.GONE);
//        findViewById(R.id.map).setVisibility(View.VISIBLE);
//    }

    class ShowConnectionInfoThread extends Thread {
        private final RemoteListenerService service;

        public ShowConnectionInfoThread(RemoteListenerService service) {
            this.service = service;
        }

        @Override
        public void run() {
            super.run();
            WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);

            byte[] myIPAddress = BigInteger.valueOf(wm.getConnectionInfo().getIpAddress()).toByteArray();
            ArrayUtils.reverse(myIPAddress);
            try {
                InetAddress myIAddress = InetAddress.getByAddress(myIPAddress);

                final String myIP = myIAddress.getHostAddress();

                final String port = String.valueOf(service.getPort());

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv_ip.setText(myIP);
                        tv_port.setText(port);
                    }
                });
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }
}
