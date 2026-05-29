package com.arraiyan.sosguard;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // অ্যাপ ওপেন হওয়া মাত্রই ব্যাকগ্রাউন্ড সার্ভিস চালু করার কমান্ড
        Intent serviceIntent = new Intent(this, SOSBackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        finish(); // ব্যাকগ্রাউন্ডে সার্ভিস চালু করে মেইন স্ক্রিন বন্ধ করে দেবে
    }
}
