package com.michaelchen.chairtalk;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;

/**
 * Created by michael on 4/29/15.
 */
abstract class QueryTask extends AsyncTask<String, Void, Boolean> {

    static final String uri = "http://54.215.11.207:38001";
    public static final String QUERY_LINE = "select data before now where uuid = '%s'";
    public static final String uuid = "e20c1de2-bc87-55de-8e8c-e8c895ede8b6";
    public static final String OCC_TIME_TAG = "occTime";
    public static final String OCC = "occupancy_local";
    public static final String QUERY_STRING = "http://54.215.11.207:8079/api/query";


    @Override
    protected Boolean doInBackground(String... params) {
        try {
            DefaultHttpClient httpclient = new DefaultHttpClient();
            String wfMac = params[0];
            HttpGet request = new HttpGet(uri + "?macaddr=" + wfMac);
            HttpResponse httpResponse = httpclient.execute(request);
            InputStream inputStream = httpResponse.getEntity().getContent();
            String response = MainActivity.inputStreamToString(inputStream);
            Log.d("httpGet", response);
            JSONObject jsonResponse = new JSONObject(response);

            try {
                httpclient = new DefaultHttpClient();
                HttpPost httpPostReq = new HttpPost(QUERY_STRING);
                StringEntity se = new StringEntity(String.format(QUERY_LINE, uuid)); // TODO: get uuid dynamically

                httpPostReq.setEntity(se);
                httpResponse = httpclient.execute(httpPostReq);
                inputStream = httpResponse.getEntity().getContent();
                response = MainActivity.inputStreamToString(inputStream);
                Log.d("httpPost", response);
                JSONObject jsonResponse1 = new JSONObject(response.substring(1, response.length() - 1));
                JSONArray readings = ((JSONArray) jsonResponse1.getJSONArray("Readings")).getJSONArray(0);
                int value = readings.getInt(1);
                long time = readings.getLong(0);
                jsonResponse.put(OCC_TIME_TAG, time);
                jsonResponse.put(OCC, value);
            } catch (Exception e) {

            }
            return processJsonObject(jsonResponse);

        } catch (Exception e) {
            Log.e("HTTPGet", "failed", e);
            return false;
        }
    }

    protected abstract boolean processJsonObject(JSONObject jsonResponse);
}
