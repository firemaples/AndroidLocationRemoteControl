package com.firemaples.androidlocationremotecontrol;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Date;

public class RemoteListenerService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private final String MSG_GEO = "geo:";

    private final IBinder mBinder = new RemoteListenerBinder();
    private OnRemoteListenerCallback callback;
    private boolean socketConnected = false;
    private WebSocket mWebSocketClient;
    private int port = 8888;
    private String socketName = "/alrc";

    private String locationProvider = LocationManager.GPS_PROVIDER;
    private SimpleSocketServer socketServer;
    private LocationManager locationManager;
    private GoogleApiClient googleApiClient;

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
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disableMockProvider();
        stopListening();
    }

    public void startListening() {
        socketServer = new SimpleSocketServer(this, new InetSocketAddress(port), socketName, socketServerCallback);
        socketServerRunTask.execute();

        callback.onServicePrepared(this);
    }

    public void disconnect() {
        if (socketServer != null) {
            for (WebSocket webSocket : socketServer.connections()) {
                webSocket.close();
            }
        }
    }

    public void stopListening() {
        disconnect();

        if (socketServer != null) {
            try {
                socketServer.stop(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        socketServer = null;
    }

    private void enableMockProvider() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        googleApiClient.connect();

        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        //noinspection MissingPermission
        Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
        if (lastKnownLocation != null) {
            callback.onMockLocationChange(this, lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        }
        locationManager.addTestProvider(locationProvider, false, false,
                false, false, false, false, false, 0, android.location.Criteria.ACCURACY_FINE);
        locationManager.setTestProviderEnabled(locationProvider, true);

        postNotification("");
    }

    private void mockLocation(double lat, double lng) {
        Location location = new Location(locationProvider);
        location.setLatitude(lat);
        location.setLongitude(lng);
        location.setAltitude(0);
        location.setAccuracy(1);
        location.setBearing(0);
        location.setSpeed(0);
        location.setTime(new Date().getTime());
        if (android.os.Build.VERSION.SDK_INT >= 17) {
            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }
        locationManager.setTestProviderLocation(locationProvider, location);
        try {
            //noinspection MissingPermission
            LocationServices.FusedLocationApi.setMockLocation(googleApiClient, location);
        } catch (Exception e) {
            e.printStackTrace();
        }

        postNotification("Current Location:" + lat + ", " + lng);

        callback.onMockLocationChange(this, lat, lng);
    }

    private void disableMockProvider() {
        if (locationManager != null) {
            locationManager.setTestProviderEnabled(locationProvider, false);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            try {
                LocationServices.FusedLocationApi.setMockMode(googleApiClient, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
            locationManager.clearTestProviderEnabled(locationProvider);
            locationManager.clearTestProviderStatus(locationProvider);
            locationManager.clearTestProviderLocation(locationProvider);
        }

        removeNotification();
    }

    private void postNotification(String contentText) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setAutoCancel(false)
                .setSmallIcon(R.drawable.app_notify)
                .setContentTitle("GPS location mocking")
                .setContentText(contentText)
                .setContentIntent(pendingIntent);
        notificationManager.notify(0, builder.build());
    }

    private void removeNotification() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationManager.cancelAll();
    }

    public String getAddress() {
        return socketServer.getAddress().getHostName();
    }

    public int getPort() {
        return socketServer.getAddress().getPort();
    }

    public String getSocketName() {
        return socketName;
    }

    public WebSocket getWebSocketClient() {
        return mWebSocketClient;
    }

    private AsyncTask<Void, Void, Void> socketServerRunTask = new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
            socketServer.run();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            try {
                if (socketServer != null) {
                    socketServer.stop();
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            socketServer = null;
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

            enableMockProvider();

            callback.onConnected(RemoteListenerService.this);
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            socketConnected = false;

            disableMockProvider();

            callback.onDisconnected(RemoteListenerService.this);
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            if (message.startsWith(MSG_GEO)) {
                String[] content = message.substring(MSG_GEO.length()).split(",");
                double lat = Double.parseDouble(content[0].trim());
                double lng = Double.parseDouble(content[1].trim());
                Utils.makeTestLog(RemoteListenerService.this, "Get geo msg from client: " + lat + ", " + lng);

                mockLocation(lat, lng);
            } else {
                Utils.makeTestLog(RemoteListenerService.this, "Get a message from client: " + message);
            }
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {

        }
    };

    @Override
    public void onConnected(Bundle bundle) {
        Utils.makeTestLog(this, "Google api :onConnected");
        startMockMode();
    }

    protected void startMockMode() {
        try {
            //noinspection MissingPermission
            LocationServices.FusedLocationApi.setMockMode(googleApiClient, true);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Please set this app in mock location app", Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Utils.makeTestLog(this, "Google api :onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Utils.makeTestLog(this, "Google api :onConnectionFailed");
    }

    public class RemoteListenerBinder extends Binder {
        RemoteListenerService getService() {
            return RemoteListenerService.this;
        }
    }

    public interface OnRemoteListenerCallback {
        void onServicePrepared(RemoteListenerService service);

        void onConnected(RemoteListenerService service);

        void onDisconnected(RemoteListenerService service);

        void onMockLocationChange(RemoteListenerService service, double lat, double lng);
    }
}
