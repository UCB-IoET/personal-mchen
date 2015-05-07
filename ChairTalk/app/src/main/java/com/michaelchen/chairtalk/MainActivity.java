package com.michaelchen.chairtalk;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends ActionBarActivity {

    public static final String TAG = "MainActivity";
    private SeekBar seekBottomFan;
    private SeekBar seekBackFan;
    private SeekBar seekBottomHeat;
    private SeekBar seekBackHeat;
    private static final String uri = "http://54.215.11.207:38001";
    public static final int refreshPeriod = 10000;
    public static final int smapDelay = 20000;
    private Timer timer = null;
    private BluetoothManager bluetoothManager = null;
    private Date lastUpdate; //TODO: check to make sure smap loop never happens

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    protected static final int REQUEST_OK = 1;
    protected static final int BLUETOOTH_REQUEST = 33;

    public static final long smapUpdateTime = 15000; //AlarmManager.INTERVAL_HALF_HOUR;

    static final String BACK_FAN = "Back Fan";
    static final String BOTTOM_FAN = "Bottom Fan";
    static final String BACK_HEAT = "Back Heat";
    static final String BOTTOM_HEAT = "Bottom Heat";
    static final String LAST_TIME = "Last Time";
    static final String WF_KEY = "wifi_mac";
    static final String FIRST_LAUNCH = "first_launch";
    static final String SKIP_BL = "skip_bluetooth";

    static final Map<String, String> uuidToKey;
    static final Map<String, String> keyToUuid;
    static final Map<String, String> jsonToKey;

    static {
        Map<String, String> temp = new HashMap<>();
        temp.put("27e1e889-b749-5cf9-8f90-5cc5f1750ddf", BACK_FAN);
        temp.put("33ecc20c-e636-58eb-863f-142717105075", BACK_HEAT);
        temp.put("a99daf41-f3b3-51a7-97bf-48fb3e7bf130", BOTTOM_HEAT);
        temp.put("b7ef2e98-2e0a-515b-b534-69894fdddf6f", BOTTOM_FAN);
        uuidToKey = Collections.unmodifiableMap(temp);
        temp = new HashMap<>();
        for(Map.Entry<String, String> entry : uuidToKey.entrySet()){
            temp.put(entry.getValue(), entry.getKey());
        }
        keyToUuid = Collections.unmodifiableMap(temp);
        temp = new HashMap<>();
        temp.put("backf", BACK_FAN);
        temp.put("bottomf", BOTTOM_FAN);
        temp.put("backh", BACK_HEAT);
        temp.put("bottomh", BOTTOM_HEAT);
        temp.put("time", LAST_TIME);
        jsonToKey = Collections.unmodifiableMap(temp);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.temp_preference_file_key), Context.MODE_PRIVATE);
        if (sharedPref.getBoolean(FIRST_LAUNCH, true)) {
            Intent i = new Intent(this, Tutorial.class);
            startActivity(i);
            finish();
            return;
        }
        setContentView(R.layout.activity_main);
        initSeekbarListeners();
        setSeekbarPositions();
        updateLastUpdate();
        lastUpdate = new Date();
        setRecurringAlarm(smapUpdateTime);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String name = sp.getString(SettingsActivity.NAME, "");
        TextView tv = (TextView) findViewById(R.id.textViewVoice);
        tv.setText(getString(R.string.hello) + " " + name);
    }

    private void initBle() {
        final Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRAS_DEVICE_NAME) && intent.hasExtra(EXTRAS_DEVICE_ADDRESS)) {
//            String mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
            String mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
            bluetoothManager = new BluetoothManager(this, mDeviceAddress);
            TextView t = (TextView) findViewById(R.id.chair_desc);
            t.setText(getString(R.string.chair_desc) + mDeviceAddress);
        } else {
            SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.temp_preference_file_key), Context.MODE_PRIVATE);
            String mac = sharedPreferences.getString(BluetoothManager.MAC_KEY, "");
            if (!mac.equals("")) {
                Intent i = new Intent(this, BluetoothActivity.class);
                startActivity(i);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent i = getIntent();
        if (i != null && i.getBooleanExtra(SKIP_BL, false)) {

        } else if (bluetoothManager == null || !bluetoothManager.isConnected()) {
            initBle();
            return;
        }
        rescheduleTimer(0);
        if (bluetoothManager != null) bluetoothManager.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timer != null) timer.cancel();
        if (bluetoothManager != null) bluetoothManager.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothManager != null) bluetoothManager.onDestroy();
    }

    protected void initSeekbarListeners() {
        seekBackFan = (SeekBar) findViewById(R.id.seekBarBackFan);
        seekBackFan.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int currentPosition = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                currentPosition = progress;
            }

            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                MainActivity.this.updatePref(BACK_FAN, currentPosition);
                sendUpdateLocal();
            }
        });

        seekBottomFan = (SeekBar) findViewById(R.id.seekBarBottomFan);
        seekBottomFan.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int currentPosition = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                currentPosition = progress;
            }

            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                MainActivity.this.updatePref(BOTTOM_FAN, currentPosition);
                sendUpdateLocal();
            }
        });

        seekBackHeat = (SeekBar) findViewById(R.id.seekBarBackHeat);
        seekBackHeat.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int currentPosition = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                currentPosition = progress;
            }

            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                MainActivity.this.updatePref(BACK_HEAT, currentPosition);
                sendUpdateLocal();
            }
        });

        seekBottomHeat = (SeekBar) findViewById(R.id.seekBarBottomHeat);
        seekBottomHeat.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int currentPosition = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                currentPosition = progress;
            }

            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                MainActivity.this.updatePref(BOTTOM_HEAT, currentPosition);
                sendUpdateLocal();
            }
        });
    }

    protected void setSeekbarPositions() {
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.temp_preference_file_key), Context.MODE_PRIVATE);

        int backFanPos = sharedPref.getInt(BACK_FAN, 0);
        seekBackFan.setProgress(backFanPos);
        int bottomFanPos = sharedPref.getInt(BOTTOM_FAN, 0);
        seekBottomFan.setProgress(bottomFanPos);

        int backHeatPos = sharedPref.getInt(BACK_HEAT, 0);
        seekBackHeat.setProgress(backHeatPos);
        int bottomHeatPos = sharedPref.getInt(BOTTOM_HEAT, 0);
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

    protected void sendUpdateSmap(boolean fromFS) {
        SharedPreferences sharedPref = MainActivity.this.getSharedPreferences(
                getString(R.string.temp_preference_file_key), Context.MODE_PRIVATE);
        int backFanPos = sharedPref.getInt(BACK_FAN, 0);
        int bottomFanPos = sharedPref.getInt(BOTTOM_FAN, 0);
        int backHeatPos = sharedPref.getInt(BACK_HEAT, 0);
        int bottomHeatPos = sharedPref.getInt(BOTTOM_HEAT, 0);
        boolean inChair = sharedPref.getBoolean(getString(R.string.in_chair_key), false);
        int temp = sharedPref.getInt(getString(R.string.temp_key), 0);
        int humidity = sharedPref.getInt(getString(R.string.humidity_key), 0);
        JSONObject jsonobj = createJsonObject(backFanPos, bottomFanPos, backHeatPos, bottomHeatPos, inChair, temp, humidity, fromFS);
        HttpAsyncTask task = new HttpAsyncTask(jsonobj);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, uri);
    }

    protected void updateJsonText(JSONObject jsonobj) {
        TextView t = (TextView) findViewById(R.id.textViewJson);
        t.setText(jsonobj.toString());
    }

    private void rescheduleTimer() {
        rescheduleTimer(smapDelay);
    }

    private void rescheduleTimer(int delay) {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                MainActivity.this.querySmap();
            }
        };
        timer.scheduleAtFixedRate(timerTask, delay, refreshPeriod);
    }

    public void sendUpdateLocal() {
        // used to update everything if user changes value from app
        rescheduleTimer();
        sendUpdateBle();
        sendUpdateSmap(false);
        lastUpdate = new Date();
    }

    private boolean validUpdateTime() {
        Date currentTime = new Date();
        long seconds = (currentTime.getTime()-lastUpdate.getTime());
        return seconds > smapDelay;
    }

    void setBleStatus(byte[] status) {
        if (status.length < 9 || !validUpdateTime()) {
            return;
        }

        SharedPreferences sharedPref = MainActivity.this.getSharedPreferences(
                getString(R.string.temp_preference_file_key), Context.MODE_PRIVATE);

        SharedPreferences.Editor e = sharedPref.edit();
        e.putInt(BACK_HEAT, status[0]);
        e.putInt(BOTTOM_HEAT, status[1]);
        e.putInt(BACK_FAN, status[2]);
        e.putInt(BOTTOM_FAN, status[3]);
        e.putBoolean(getString(R.string.in_chair_key), (status[4] != 0));
        int temp = ((unsignedByteToInt(status[5])) << 8) + unsignedByteToInt(status[6]);
        int humidity = ((unsignedByteToInt(status[7])) << 8) + unsignedByteToInt(status[8]);
        e.putInt(getString(R.string.temp_key), temp);
        e.putInt(getString(R.string.humidity_key), humidity);
        e.apply();
        e.commit();
        rescheduleTimer();
        sendUpdateSmap(true);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.setSeekbarPositions();
            }
        });
    }

    public static int unsignedByteToInt(byte b) {
        return b & 0xff;
    }

    private byte[] getByteStatus() {
        SharedPreferences sharedPref = MainActivity.this.getSharedPreferences(
                getString(R.string.temp_preference_file_key), Context.MODE_PRIVATE);
        int backFanPos = sharedPref.getInt(BACK_FAN, 0);
        int bottomFanPos = sharedPref.getInt(BOTTOM_FAN, 0);
        int backHeatPos = sharedPref.getInt(BACK_HEAT, 0);
        int bottomHeatPos = sharedPref.getInt(BOTTOM_HEAT, 0);
        byte[] ret = {(byte) backHeatPos, (byte) bottomHeatPos, (byte) backFanPos, (byte) bottomFanPos, 1};
        return ret;
    }

    private void sendUpdateBle() {
        boolean result = false;
        if (bluetoothManager != null) {
            if (bluetoothManager.writeData(getByteStatus())) {
                result = true;
            }
        }
        if (!result) Toast.makeText(getApplicationContext(), getString(R.string.no_bl), Toast.LENGTH_SHORT).show();
    }

    void disconnectBluetoothManager() {
        bluetoothManager = null;
        recreate();
    }

    private JSONObject createJsonObject(int backFan, int bottomFan, int backHeat, int bottomHeat, boolean inChair,
                                        int temp, int humidity, boolean fromFS) {
        JSONObject jsonobj = new JSONObject();
        JSONObject header = new JSONObject();
        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            String name = sp.getString(SettingsActivity.NAME, "Bob the Builder");
            header.put("devicemodel", android.os.Build.MODEL); // Device model
            header.put("deviceVersion", android.os.Build.VERSION.RELEASE); // Device OS version
            header.put("language", Locale.getDefault().getISO3Language()); // Language
            header.put("name", name);
            jsonobj.put("header", header);

            jsonobj.put("occupancy", inChair);
            jsonobj.put("backf", backFan);
            jsonobj.put("bottomf", bottomFan);
            jsonobj.put("backh", backHeat);
            jsonobj.put("bottomh", bottomHeat);
            jsonobj.put("temperature", temp);
            jsonobj.put("humidity", humidity);
            jsonobj.put("fromFS", fromFS);
            SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.temp_preference_file_key), Context.MODE_PRIVATE);
            String wfmac = sharedPreferences.getString(WF_KEY, "");
            jsonobj.put("macaddr", wfmac);
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
//                        Toast.makeText(getBaseContext(), "Post Result: " + response, Toast.LENGTH_SHORT).show();
                        Date now = new Date();
                        long time = now.getTime();
                        int secTime = (int) (System.currentTimeMillis()/1000);
                        MainActivity.this.updatePref(getString(R.string.last_server_push_key), time);
                        MainActivity.this.updatePref(LAST_TIME, secTime);
                        MainActivity.this.updateLastUpdate();
                        MainActivity.this.updateJsonText(jsonobj);
                    }
                });

                return true;
            } catch (Exception e) {
                Log.d("httpPost", "failed");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    Toast.makeText(getBaseContext(), "Please Check Connection", Toast.LENGTH_SHORT).show();
                    }
                });
                return false;
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        protected void onPostExecute(String result) {
        }
    }

    static String inputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }

    //@Deprecated, but allows direct query of smap should server fail
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
                        long time = readings.getLong(0);
                        MainActivity.this.updatePref(MainActivity.this.uuidToKey.get(retUuid), value);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                            MainActivity.this.setSeekbarPositions();
                            }
                        });
                    } catch (Exception e) {
                        Log.d("httpPost", "failed");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getBaseContext(), "Please Check Connection", Toast.LENGTH_SHORT).show();
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
        protected void onPostExecute(Boolean result) {
            MainActivity.this.signalTaskComplete();
        }
    }

    private class ServerQueryTask extends UpdateTask {
        public ServerQueryTask() {
            super(MainActivity.this);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                sendUpdateBle();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setSeekbarPositions();
                        MainActivity.this.updateLastPullTime();
                    }
                });
            }
        }
    }

    private int numTasksComplete;

    private void querySmap() {
        QueryTask task = new ServerQueryTask();
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.temp_preference_file_key), Context.MODE_PRIVATE);
        String wfmac = sharedPreferences.getString(WF_KEY, "");
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, wfmac);
//        numTasksComplete = 0;
//        for(String uuid : uuidToKey.keySet()) {
//            SmapQueryAsyncTask task =  new SmapQueryAsyncTask(uuid);
//            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, QUERY_STRING);
//        }
    }

    private void signalTaskComplete() {
        numTasksComplete++;
        if (numTasksComplete == uuidToKey.size()) {
            sendUpdateBle();
        }
    }

    private void setRecurringAlarm(long period) {
        Intent intent = new Intent(getApplicationContext(), StartServiceReceiver.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), period, pendingIntent);
        Log.d(TAG, "Start repeating alarm");
    }

    void setBluetoothConnected(boolean connected) {
        if (connected) {

        } else {
            bluetoothManager = null;
            invalidateOptionsMenu();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (bluetoothManager == null) {
            menu.findItem(R.id.action_disconnect).setVisible(false);
            menu.findItem(R.id.action_bluetooth).setVisible(true);
        } else {
            menu.findItem(R.id.action_disconnect).setVisible(true);
            menu.findItem(R.id.action_bluetooth).setVisible(false);
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch(id) {
            case R.id.action_settings:
                return true;
            case R.id.action_bluetooth:
                startActivity(new Intent(this, BluetoothActivity.class));
                return true;
            case R.id.action_disconnect:
                if (bluetoothManager != null) {
                    bluetoothManager.disconnect();
                    bluetoothManager = null;
                }
                invalidateOptionsMenu();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    //Extras for some voice commands
    public void onVoiceClick(MenuItem item) {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
        try {
            startActivityForResult(i, REQUEST_OK);
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing speech to text engine.", Toast.LENGTH_LONG).show();
        }
    }

    public void onSettingsClick(MenuItem item) {
        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==REQUEST_OK  && resultCode==RESULT_OK) {
            ArrayList<String> voiceData = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (voiceData.size() > 0) {
                TextView t = (TextView) findViewById(R.id.textViewVoice);
                t.setText(voiceData.get(0));
                String text = voiceData.get(0).toLowerCase();
                if (text.contains("fan") || text.contains("hot")) {
                    coolDown();
                } else if (text.contains("heat") || text.contains("cold")) {
                    heatUp();
                }

            }
        } else if (requestCode == BLUETOOTH_REQUEST && resultCode != RESULT_OK) {
            Toast.makeText(this, "No Bluetooth Connection", Toast.LENGTH_SHORT);
        }
    }

    private static final int MAX_SEEKBAR_POS = 100;
    private static final int MIN_SEEKBAR_POS = 0;

    private void coolDown() {
        MainActivity.this.updatePref(BACK_FAN, MAX_SEEKBAR_POS);
        MainActivity.this.updatePref(BOTTOM_FAN, MAX_SEEKBAR_POS);
        MainActivity.this.updatePref(BACK_HEAT, MIN_SEEKBAR_POS);
        MainActivity.this.updatePref(BOTTOM_HEAT, MIN_SEEKBAR_POS);
        sendUpdateLocal();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.setSeekbarPositions();
            }
        });
    }

    private void heatUp() {
        MainActivity.this.updatePref(BACK_FAN, MIN_SEEKBAR_POS);
        MainActivity.this.updatePref(BOTTOM_FAN, MIN_SEEKBAR_POS);
        MainActivity.this.updatePref(BACK_HEAT, MAX_SEEKBAR_POS);
        MainActivity.this.updatePref(BOTTOM_HEAT, MAX_SEEKBAR_POS);
        sendUpdateLocal();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.setSeekbarPositions();
            }
        });
    }
}
