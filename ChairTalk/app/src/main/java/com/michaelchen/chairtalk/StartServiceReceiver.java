package com.michaelchen.chairtalk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by michael on 4/27/15.
 */
public class StartServiceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent smapQueryTask = new Intent(context, UpdateService.class);
        context.startService(smapQueryTask);
        Log.d("StartServiceReceiver", "Called context.startService from AlarmReceiver.onReceive");
    }
}
