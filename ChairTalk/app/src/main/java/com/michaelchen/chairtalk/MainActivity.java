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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.jar.Pack200;


public class MainActivity extends ActionBarActivity {

    private SeekBar seekBottomFan;
    private SeekBar seekBackFan;
    private SeekBar seekBottomHeat;
    private SeekBar seekBackHeat;
//    private static final String uri = "http://shell.storm.pm:38027";
    private static final String uri = "http://54.215.11.207:38027";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initSeekbarListeners();
        initSeekbarPositions();
        initSwitch();
        updateLastUpdate();
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
            }
        });
    }

    protected void initSeekbarPositions() {
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


    protected void updateLastUpdate() {
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.temp_preference_file_key), Context.MODE_PRIVATE);
        long lastUpdateTime = sharedPref.getLong(getString(R.string.last_server_push_key), -1);
        if (lastUpdateTime != -1) {
            Date time = new Date(lastUpdateTime);
            TextView t = (TextView) findViewById(R.id.textViewlastUpdateTime);
            DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            t.setText("Last Update: " + df.format(time));
        }
    }

    protected void initSwitch() {
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.temp_preference_file_key), Context.MODE_PRIVATE);
        boolean inChair = sharedPref.getBoolean(getString(R.string.in_chair_key), false);
        Switch inChairSwitch = (Switch) findViewById(R.id.switchInChair);
        inChairSwitch.setChecked(inChair);
        inChairSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MainActivity.this.updatePref(getString(R.string.in_chair_key), isChecked);
            }
        });

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
        JSONObject jsonobj = createJsonObject(backFanPos, bottomFanPos, backHeatPos, bottomHeatPos, inChair);
        new HttpAsyncTask(jsonobj).execute(uri);
        TextView t = (TextView) findViewById(R.id.textViewJson);
        t.setText(jsonobj.toString());
    }

    public void smapButton(View view) {
        sendUpdateSmap();
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

                client(jsonobj);
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

    public static void client(JSONObject jsonObject) throws IOException {
        DatagramSocket clientSocket = new DatagramSocket(2362);
        InetAddress IPAddress =  InetAddress.getByName("storm.rocks");

        byte[] sendData = jsonObject.toString().getBytes();

        DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length, IPAddress, 2362);
        clientSocket.send(sendPacket);
//        byte[] receiveData = new byte[1024];
//        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
//        clientSocket.receive(receivePacket);
//        String modifiedSentence = new String(receivePacket.getData());
//        Log.d("UDP", modifiedSentence);
        clientSocket.close();

//        Socket socket = new Socket("192.168.1.102", 2362);
//        OutputStream out = socket.getOutputStream();
//        PrintWriter output = new PrintWriter(out);
//        output.print(jsonObject.toString());
//        out.flush();
//        out.close();

        Log.d("UDP", "send ok");

        // }

    }
}
