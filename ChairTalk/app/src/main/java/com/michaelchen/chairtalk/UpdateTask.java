package com.michaelchen.chairtalk;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by michael on 4/27/15.
 */
public class UpdateTask extends QueryTask {
    private Context context;

    UpdateTask(Context context) {
        this.context = context;
    }

    protected boolean updatePref(String key, int value) {
        // update heating or cooling
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.temp_preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor e = sharedPref.edit();
        e.putInt(key, value);
        e.apply();
        return e.commit();
    }

    @Override
    protected boolean processJsonObject(JSONObject jsonResponse) {
        boolean ret = false;
        int currentTime = 0;
        int prevUpdateTime = context.getSharedPreferences(context.getString(
                R.string.temp_preference_file_key), Context.MODE_PRIVATE).getInt(MainActivity.LAST_TIME, -1);
        for(Map.Entry<String, String> entry : MainActivity.jsonToKey.entrySet()) {
            String jsonKey = entry.getKey();
            String localKey = entry.getValue();

            try {
                int value = jsonResponse.getInt(jsonKey);
                if (localKey.equals(MainActivity.LAST_TIME)) {
                    currentTime = value;
                    if (value > prevUpdateTime) {
                        updatePref(localKey, value);
                        ret = true;
                    } else {
                        Log.d("Update task", "rejected timestamp: " + value + ", needed: " + prevUpdateTime);
                    }
                } else {
                    updatePref(localKey, value);
                }
            } catch (JSONException e) {
                Log.e("JSONHandling", "json parse error", e);
            }
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String name = sp.getString(SettingsActivity.NAME, "");

        return ret;
    }

}
