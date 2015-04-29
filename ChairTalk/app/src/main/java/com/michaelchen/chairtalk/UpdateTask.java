package com.michaelchen.chairtalk;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by michael on 4/27/15.
 */
public class UpdateTask extends AsyncTask<Void, Void, Boolean> {
    public static final String QUERY_LINE = "select data before now where uuid = '%s'";
    private Context context;
    protected Map<String, String> uuidToKey;
    protected Map<String, String> keyToUuid;

    UpdateTask(Context context) {
        this.context = context;
        initMaps();
    }

    private void initMaps() {
        uuidToKey = new HashMap<>();
        uuidToKey.put("27e1e889-b749-5cf9-8f90-5cc5f1750ddf", context.getString(R.string.seek_back_fan));
        uuidToKey.put("33ecc20c-e636-58eb-863f-142717105075", context.getString(R.string.seek_back_heat));
        uuidToKey.put("a99daf41-f3b3-51a7-97bf-48fb3e7bf130", context.getString(R.string.seek_bottom_heat));
        uuidToKey.put("b7ef2e98-2e0a-515b-b534-69894fdddf6f", context.getString(R.string.seek_bottom_fan));

        keyToUuid = new HashMap<>();
        for (Map.Entry<String, String> entry : uuidToKey.entrySet()) {
            keyToUuid.put(entry.getValue(), entry.getKey());
        }
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
    protected Boolean doInBackground(Void... useless) {
        final String url = "http://shell.storm.pm:8079/api/query";
        Log.d("UpdateTask", "starting background task");
        for(final String uuid : uuidToKey.keySet()) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        DefaultHttpClient httpclient = new DefaultHttpClient();
                        HttpPost httpPostReq = new HttpPost(url);
                        StringEntity se = new StringEntity(String.format(QUERY_LINE, uuid));

                        httpPostReq.setEntity(se);
                        HttpResponse httpResponse = httpclient.execute(httpPostReq);
                        InputStream inputStream = httpResponse.getEntity().getContent();
                        final String response = MainActivity.inputStreamToString(inputStream);
                        Log.d("httpPost Update", response);
                        JSONObject jsonResponse = new JSONObject(response.substring(1, response.length() - 1));
                        JSONArray readings = ((JSONArray) jsonResponse.getJSONArray("Readings")).getJSONArray(0);
                        String retUuid = jsonResponse.getString("uuid");
                        int value = readings.getInt(1);
                        long time = readings.getLong(0);
                        UpdateTask.this.updatePref(UpdateTask.this.uuidToKey.get(retUuid), value);


                    } catch (Exception e) {
                        Log.d("httpPost", "failed");

                    }
                }
            });
            t.run();
        }

        return true;
    }
    // onPostExecute displays the results of the AsyncTask.
    protected void onPostExecute(String result) {
    }
}
