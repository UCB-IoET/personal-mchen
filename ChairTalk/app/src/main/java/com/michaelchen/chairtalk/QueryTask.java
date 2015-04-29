package com.michaelchen.chairtalk;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.InputStream;

/**
 * Created by michael on 4/29/15.
 */
abstract class QueryTask extends AsyncTask<String, Void, Boolean> {

    static final String uri = "http://54.215.11.207:38001";
    public static final String macAddr = "12345";
    public static final String FAILURE = "";

    @Override
    protected Boolean doInBackground(String... params) {
        try {
            DefaultHttpClient httpclient = new DefaultHttpClient();
            HttpGet request = new HttpGet(uri + "?macaddr=" + macAddr);
            HttpResponse httpResponse = httpclient.execute(request);
            InputStream inputStream = httpResponse.getEntity().getContent();
            final String response = MainActivity.inputStreamToString(inputStream);
            Log.d("httpGet", response);
            JSONObject jsonResponse = new JSONObject(response);
            processJsonObject(jsonResponse);

        } catch (Exception e) {
            Log.e("HTTPGet", "failed", e);
            return false;
        }

        return true;
    }

    protected abstract void processJsonObject(JSONObject jsonResponse);
}
