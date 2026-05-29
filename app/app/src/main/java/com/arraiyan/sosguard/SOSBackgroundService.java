package com.arraiyan.sosguard;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.SmsManager;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.ArrayList;
import java.util.List;

public class SOSBackgroundService extends Service {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private FusedLocationProviderClient fusedLocationClient;
    
    // আপনার কেনা রেডিমেড ব্লুটুথ ট্যাগের MAC Address (টেস্ট করার সময় এটি পরিবর্তন করবেন)
    private final String TARGET_TAG_MAC = "AA:BB:CC:11:22:33"; 
    // ইমার্জেন্সি কন্টাক্ট হিসেবে আপনার সঠিক নাম্বার সেট করা হলো
    private final String EMERGENCY_NUMBER = "01704100288"; 

    @Override
    public void onCreate() {
        super.onCreate();
        
        // ব্লুটুথ ইনিশিয়ালাইজেশন
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
        
        // গুগল জিপিএস লোকেশন ক্লায়েন্ট ইনিশিয়ালাইজেশন
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // ১. নোটিফিকেশন চ্যানেল তৈরি ও সার্ভিস জিন্দা রাখা
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, "SOS_CHANNEL")
                .setContentTitle("Ar Raiyan SOS Guard Active")
                .setContentText("Listening for your safety bracelet...")
                .setSmallIcon(android.R.drawable.ic_secure)
                .build();

        startForeground(1, notification);

        // ২. ব্লুটুথ স্ক্যান শুরু করা
        startBLEScanning();

        return START_STICKY;
    }

    private void startBLEScanning() {
        if (bleScanner == null) return;

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();

        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter filter = new ScanFilter.Builder()
                .setDeviceAddress(TARGET_TAG_MAC)
                .build();
        filters.add(filter);

        try {
            bleScanner.startScan(filters, settings, scanCallback);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    // ব্লুটুথ সিগন্যাল পাওয়ার কলব্যাক লজিক
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            
            // সিগন্যাল পাওয়া মাত্রই জিপিএস লোকেশন রিড করা শুরু হবে
            fetchCurrentLocationAndSendSOS();
        }
    };

    @SuppressLint("MissingPermission")
    private void fetchCurrentLocationAndSendSOS() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMaxUpdates(1) 
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(com.google.android.gms.location.LocationResult locationResult) {
                if (locationResult == null) {
                    sendFallbackLocation();
                    return;
                }
                
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        sendSOSMessage(location.getLatitude(), location.getLongitude());
                        break;
                    }
                }
            }
        }, Looper.getMainLooper());
    }

    @SuppressLint("MissingPermission")
    private void sendFallbackLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                sendSOSMessage(location.getLatitude(), location.getLongitude());
            } else {
                sendSOSMessage(0.0, 0.0);
            }
        });
    }

    private void sendSOSMessage(double latitude, double longitude) {
        String message;
        if (latitude == 0.0 && longitude == 0.0) {
            message = "ALERT! Emergency SOS Triggered. GPS Location could not be fetched immediately!";
        } else {
            String mapLink = "https://maps.google.com/?q=" + latitude + "," + longitude;
            message = "ALERT! Emergency SOS Triggered. My Live Location: " + mapLink;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(EMERGENCY_NUMBER, null, message, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "SOS_CHANNEL", "SOS Service Channel", NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (bleScanner != null) {
            try {
                bleScanner.stopScan(scanCallback);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }
}
