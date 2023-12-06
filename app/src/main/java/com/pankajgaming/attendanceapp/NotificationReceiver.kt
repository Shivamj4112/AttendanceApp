package com.pankajgaming.attendanceapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // Handle the notification logic here
        Toast.makeText(context, "Time for your attendance update!", Toast.LENGTH_SHORT).show()
        // You can also trigger the notification here
        // For more advanced notification handling, you may want to use NotificationManager
    }
}

