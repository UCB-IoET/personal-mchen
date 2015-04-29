package com.michaelchen.chairtalk;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Created by michael on 4/27/15.
 */
public class UpdateService extends IntentService {

    public static final String TAG = "UpdateService";

    public UpdateService() {
        super("UpdateService");
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        Context c = getApplicationContext();
        UpdateTask task = new UpdateTask(c);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
