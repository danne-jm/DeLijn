package com.danieljm.delijn.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/** Simple BroadcastReceiver placeholder that would show or post a notification. */
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // TODO: build and show a real notification using NotificationManager
        context?.let {
            Toast.makeText(it, "Bus arrival reminder", Toast.LENGTH_SHORT).show()
        }
    }
}

