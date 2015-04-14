package com.michaelchen.chairtalk;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends ActionBarActivity {

    private SeekBar seekBottomFan;
    private SeekBar seekBackFan;
    private SeekBar seekBottomHeat;
    private SeekBar seekBackHeat;
    protected Map<String, String> uuidToKey;
    protected Map<String, String> keyToUuid;
//    private static final String uri = "http://shell.storm.pm:38027";
//    private static final String uri = "http://54.215.11.207:38027";
    private static final String uri = "http://54.215.11.207:38001";
    public static final int refreshPeriod = 5000;
    private Timer timer;
    private TimerTask timerTask;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initSeekbarListeners();
        setSeekbarPositions();
//        initSwitch();
        initMaps();
        updateLastUpdate();
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                MainActivity.this.querySmapView(null);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.updateLastPullTime();
                    }
                });
            }
        };
        timer.scheduleAtFixedRate(timerTask, 0, refreshPeriod);
    }

    private void initMaps() {
        uuidToKey = new HashMap<String, String>();
        uuidToKey.put("27e1e889-b749-5cf9-8f90-5cc5f1750ddf", getString(R.string.seek_back_fan));
        uuidToKey.put("33ecc20c-e636-58eb-863f-142717105075", getString(R.string.seek_back_heat));
        uuidToKey.put("a99daf41-f3b3-51a7-97bf-48fb3e7bf130", getString(R.string.seek_bottom_heat));
        uuidToKey.put("b7ef2e98-2e0a-515b-b534-69894fdddf6f", getString(R.string.seek_bottom_fan));

        keyToUuid = new HashMap<String, String>();
        for(Map.Entry<String, String> entry : uuidToKey.entrySet()){
            keyToUuid.put(entry.getValue(), entry.getKey());
        }

    }

    protected void initSeekbarListeners() {
        seekBackFan = (SeekBar) findViewById(R.id.seekBarBackFan);
        seekBackFan.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int currentPosition = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                currentPosition = progress;
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                MainActivity.this.updatePref(getString(R.string.seek_back_fan), currentPosition);
                smapButton(null);
            }
        });

        seekBottomFan = (SeekBar) findViewById(R.id.seekBarBottomFan);
        seekBottomFan.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int currentPosition = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                currentPosition = progress;
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                MainActivity.this.updatePref(getString(R.string.seek_bottom_fan), currentPosition);
                smapButton(null);
            }
        });

        seekBackHeat = (SeekBar) findViewById(R.id.seekBarBackHeat);
        seekBackHeat.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int currentPosition = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                currentPosition = progress;
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                MainActivity.this.updatePref(getString(R.string.seek_back_heat), currentPosition);
                smapButton(null);
            }
        });

        seekBottomHeat = (SeekBar) findViewById(R.id.seekBarBottomHeat);
        seekBottomHeat.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int currentPosition = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                currentPosition = progress;
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                MainActivity.this.updatePref(getString(R.string.seek_bottom_heat), currentPosition);
                smapButton(null);
            }
        });
    }

    protected void setSeekbarPositions() {
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.temp_preference_file_key), Context.MODE_PRIVATE);

        int backFanPos = sharedPref.getInt(getString(R.string.seek_back_fan), 0);
        seekBackFan.setProgress(backFanPos);
        int bottomFanPos = sharedPref.getInt(getString(R.string.seek_bottom_fan), 0);
        seekBottomFan.setProgress(bottomFanPos);

        int backHeatPos = sharedPref.getInt(getString(R.string.seek_back_heat), 0);
        seekBackHeat.setProgress(backHeatPos);
        int bottomHeatPos = sharedPref.getInt(getString(R.string.seek_bottom_heat), 0);
        seekBottomHeat.setProgress(bottomHeatPos); 
    }

    protected void updateLastPullTime() {
        Date time = new Date();
        TextView t = (TextView) findViewById(R.id.textViewlastPullTime);
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        t.setText("Last Update (Pull): " + df.format(time));
    }


    protected void updateLastUpdate() {
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.temp_preference_file_key), Context.MODE_PRIVATE);
        long lastUpdateTime = sharedPref.getLong(getString(R.string.last_server_push_key), -1);
        if (lastUpdateTime != -1) {
            Date time = new Date(lastUpdateTime);
            TextView t = (TextView) findViewById(R.id.textViewlastUpdateTime);
            DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            t.setText("Last Update (Push): " + df.format(time));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    protected boolean updatePref(String key, int value) {
        // update heating or cooling
        SharedPreferences sharedPref = MainActivity.this.getSharedPreferences(
                getString(R.string.temp_preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor e = sharedPref.edit();
        e.putInt(key, value);
        e.apply();
        return e.commit();
    }

    protected boolean updatePref(String key, long value) {
        // update heating or cooling
        SharedPreferences sharedPref = MainActivity.this.getSharedPreferences(
                getString(R.string.temp_preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor e = sharedPref.edit();
        e.putLong(key, value);
        e.apply();
        return e.commit();
    }

    protected boolean updatePref(String key, boolean value) {
        // update heating or cooling
        SharedPreferences sharedPref = MainActivity.this.getSharedPreferences(
                getString(R.string.temp_preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor e = sharedPref.edit();
        e.putBoolean(key, value);
        e.apply();
        return e.commit();
    }

    protected void sendUpdateSmap() {
        SharedPreferences sharedPref = MainActivity.this.getSharedPreferences(
                getString(R.string.temp_preference_file_key), Context.MODE_PRIVATE);
        int backFanPos = sharedPref.getInt(getString(R.string.seek_back_fan), 0);
        int bottomFanPos = sharedPref.getInt(getString(R.string.seek_bottom_fan), 0);
        int backHeatPos = sharedPref.getInt(getString(R.string.seek_back_heat), 0);
        int bottomHeatPos = sharedPref.getInt(getString(R.string.seek_bottom_heat), 0);
        boolean inChair = sharedPref.getBoolean(getString(R.string.in_chair_key), false);
//        JSONObject jsonobj = createJsonObject(backFanPos, bottomFanPos, backHeatPos, bottomHeatPos, inChair);
        JSONObject jsonobj = tempCreateJsonObject(backFanPos, bottomFanPos, backHeatPos, bottomHeatPos, inChair);
        new HttpAsyncTask(jsonobj).execute(uri);
        TextView t = (TextView) findViewById(R.id.textViewJson);
        t.setText(jsonobj.toString());
    }

    private JSONObject tempCreateJsonObject(int backFan, int bottomFan, int backHeat, int bottomHeat, boolean inChair) {
        JSONObject jsonobj = new JSONObject();
        JSONObject header = new JSONObject();
        try {
            header.put("devicemodel", android.os.Build.MODEL); // Device model
            header.put("deviceVersion", android.os.Build.VERSION.RELEASE); // Device OS version
            header.put("language", Locale.getDefault().getISO3Language()); // Language
//            jsonobj.put("header", header);

            jsonobj.put("fans", (backFan + bottomFan)/2);
//            jsonobj.put("bottomFan", bottomFan);
            jsonobj.put("heaters", (backHeat + bottomHeat)/2);
//            jsonobj.put("bottomHeater", bottomHeat);
            jsonobj.put("macaddr", "12345");
        } catch (JSONException e) {

        }
        return jsonobj;
    }

    private void rescheduleTimer() {
//        TODO: Fix timer rescheduling
//        timer.cancel();
//        timer = new Timer();
//        timer.scheduleAtFixedRate(timerTask, refreshPeriod, refreshPeriod);
    }

    public void smapButton(View view) {
        sendUpdateSmap();
        rescheduleTimer();
    }

    private JSONObject createJsonObject(int backFan, int bottomFan, int backHeat, int bottomHeat, boolean inChair) {
        JSONObject jsonobj = new JSONObject();
        JSONObject header = new JSONObject();
        try {
            header.put("devicemodel", android.os.Build.MODEL); // Device model
            header.put("deviceVersion", android.os.Build.VERSION.RELEASE); // Device OS version
            header.put("language", Locale.getDefault().getISO3Language()); // Language
            jsonobj.put("header", header);

            jsonobj.put("occupancy", inChair);
            jsonobj.put("backFan", backFan);
            jsonobj.put("bottomFan", bottomFan);
            jsonobj.put("backHeater", backHeat);
            jsonobj.put("bottomHeater", bottomHeat);
        } catch (JSONException e) {

        }
        return jsonobj;
    }

    private class HttpAsyncTask extends AsyncTask<String, Void, Boolean> {
        private JSONObject jsonobj;
        public HttpAsyncTask(JSONObject jsonobj) {
            super();
            this.jsonobj = jsonobj;
        }
        @Override
        protected Boolean doInBackground(String...urls) {
            try {
                DefaultHttpClient httpclient = new DefaultHttpClient();
                HttpPost httpPostReq = new HttpPost(uri);
                StringEntity se = new StringEntity(jsonobj.toString());

                httpPostReq.setEntity(se);
                httpPostReq.setHeader("Accept", "application/json");
                httpPostReq.setHeader("Content-type", "application/json");
                HttpResponse httpResponse = httpclient.execute(httpPostReq);
                InputStream inputStream = httpResponse.getEntity().getContent();
                final String response = inputStreamToString(inputStream);
                Log.d("httpPost", response);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(), "Post Result: " + response, Toast.LENGTH_SHORT).show();
                        Date now = new Date();
                        long time = now.getTime();
                        MainActivity.this.updatePref(getString(R.string.last_server_push_key), time);
                        MainActivity.this.updateLastUpdate();
                    }
                });

                return true;
            } catch (Exception e) {
                Log.d("httpPost", "failed");
                return false;
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        protected void onPostExecute(String result) {
        }
    }

    private static String inputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }

    private class SmapQueryAsyncTask extends AsyncTask<String, Void, Boolean> {
        private String uuid;
        public static final String QUERY_LINE = "select data before now where uuid = '%s'";
        public SmapQueryAsyncTask(String uuid) {
            super();
            this.uuid = uuid;
        }
        @Override
        protected Boolean doInBackground(String...urls) {
            for(String url: urls) {
                final String uri = url;
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            DefaultHttpClient httpclient = new DefaultHttpClient();
                            HttpPost httpPostReq = new HttpPost(uri);
                            StringEntity se = new StringEntity(String.format(QUERY_LINE, uuid));

                            httpPostReq.setEntity(se);
                            HttpResponse httpResponse = httpclient.execute(httpPostReq);
                            InputStream inputStream = httpResponse.getEntity().getContent();
                            final String response = inputStreamToString(inputStream);
                            Log.d("httpPost", response);
                            JSONObject jsonResponse = new JSONObject(response.substring(1, response.length() - 1));
                            JSONArray readings = ((JSONArray) jsonResponse.getJSONArray("Readings")).getJSONArray(0);
                            String retUuid = jsonResponse.getString("uuid");
                            int value = readings.getInt(1);
                            MainActivity.this.updatePref(MainActivity.this.uuidToKey.get(retUuid), value);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
//                            Toast.makeText(getBaseContext(), "Post Result: " + response, Toast.LENGTH_SHORT).show();
                                    MainActivity.this.setSeekbarPositions();
                                }
                            });
                        } catch (Exception e) {
                            Log.d("httpPost", "failed");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getBaseContext(), "Post Failed", Toast.LENGTH_SHORT).show();
                                }
                            });
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

    public void querySmapView(View v) {
        for(String uuid : uuidToKey.keySet()) {
            querySmap(uuid);
        }
    }

    private void querySmap(String uuid) {
        new SmapQueryAsyncTask(uuid).execute("http://shell.storm.pm:8079/api/query");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
