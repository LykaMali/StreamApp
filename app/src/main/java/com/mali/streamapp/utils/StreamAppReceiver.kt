package com.mali.streamapp.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class StreamAppReceiver: BroadcastReceiver() {

    // TODO: 13.05.2022 On Receive
    override fun onReceive(context: Context?, intent: Intent?) {
        log_e("received intent detail = $context, $intent")
        if (context != null && intent != null) {
            Toast.makeText(context, "Receive success = ${intent.data}", Toast.LENGTH_LONG).show()
            handleIntent(intent, context)
        } else {
            Toast.makeText(context, "An error occured from receiver!!", Toast.LENGTH_SHORT).show()
        }
    }

    // TODO: 13.05.2022 Functions
    private fun handleIntent(intent: Intent, context: Context) {
        handleActionIntent(intent, context)
    }

    private fun handleActionIntent(intent: Intent, context: Context) {
        val intentAction = intent.getStringExtra("action")


    }
}