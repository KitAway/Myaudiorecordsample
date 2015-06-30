package com.example.d038395.myaudiorecordsample;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ExecutionException;


public class MainActivity extends ActionBarActivity {

    static boolean wRecord =false;
    static boolean wPlayback =false;
    static String mFileName = null;
    static MediaRecorder mRecorder=null;
    static MediaPlayer mPlayer=null;
    static final String LOG_TAG = "AudioRecordTest";
    static final String filename="audio_record_test.mp4";

    static final String targetURL="http://denethor.cdsdom.polito.it:9999";
    static HttpURLConnection httpConn=null;

    public MainActivity(){
        super();
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/"+filename;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView = (TextView) findViewById(R.id.text_result);
        textView.setMovementMethod(new ScrollingMovementMethod());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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
    public void onClickRecord(View view) {
        Button btn_record=(Button)findViewById(R.id.start_record);
        Button btn_playback = (Button) findViewById(R.id.start_playback);
        Button btn_send=(Button) findViewById(R.id.send_server);
        TextView textView=(TextView)findViewById(R.id.text_record);
        ((TextView) findViewById(R.id.text_result)).setText(R.string.hello_world);

        if (wRecord) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
            btn_record.setText(R.string.startRecord);
            textView.setText(R.string.record);
            wRecord =!wRecord;
            btn_playback.setEnabled(true);
            btn_send.setEnabled(true);
        }
        else {
            btn_playback.setEnabled(false);
            btn_send.setEnabled(false);
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mRecorder.setOutputFile(mFileName);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            try {
                mRecorder.prepare();
            } catch (IOException e) {
                Log.e(LOG_TAG, "prepare() failed");
                return;
            }

            mRecorder.start();

            btn_record.setText("Stop");
            textView.setText("Recording, press to stop.");

            wRecord =!wRecord;
        }
    }

    public void onClickPlayBack(View view) {
        final Button btn_playback=(Button) findViewById(R.id.start_playback);
        final Button btn_record=(Button)findViewById(R.id.start_record);
        final Button btn_send=(Button) findViewById(R.id.send_server);
        final TextView textView = (TextView) findViewById(R.id.text_playback);
        if (wPlayback) {
            mPlayer.release();
            mPlayer = null;
            btn_playback.setText(R.string.startPlay);
            textView.setText(R.string.playback);
            wPlayback =!wPlayback;
            btn_send.setEnabled(true);
            btn_record.setEnabled(true);
        }
        else {
            btn_send.setEnabled(false);
            btn_record.setEnabled(false);
            mPlayer = new MediaPlayer();
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mPlayer.release();
                    mPlayer = null;
                    btn_playback.setText(R.string.startPlay);
                    textView.setText(R.string.playback);
                    wPlayback =!wPlayback;
                    btn_send.setEnabled(true);
                    btn_record.setEnabled(true);
                }
            });
            try {
                mPlayer.setDataSource(mFileName);
                mPlayer.prepare();
                mPlayer.start();
            } catch (IOException e) {
                Log.e(LOG_TAG, "prepare() failed");
                return;
            }
            btn_playback.setText("Stop");
            textView.setText("Press to stop playing.");
            wPlayback =!wPlayback;
        }
    }
    public void onClickSend(View view) throws IOException, ExecutionException, InterruptedException{
        Button btn_playback=(Button) findViewById(R.id.start_playback);
        Button btn_record=(Button)findViewById(R.id.start_record);
        Button btn_send=(Button) findViewById(R.id.send_server);
        btn_playback.setEnabled(false);
        btn_record.setEnabled(false);
        btn_send.setEnabled(false);
        TextView textView = (TextView)findViewById(R.id.text_result);
        textView.setText(R.string.hello_world);

        UUID uuid=UUID.randomUUID();
        File file= new File(mFileName);
        long fileLength=file.length();
        FileInputStream fis = new FileInputStream(file);
        final byte[] buffer= new byte[(int)fileLength];
        try{
            fis.read(buffer);
            URL url= new URL(targetURL);
            myParas mp=new myParas(url,uuid,buffer);
            taskExecute pm=new taskExecute();
            textView.setText(pm.execute(mp).get());
        }
        finally {
            if(httpConn!=null)httpConn.disconnect();
            fis.close();
            btn_playback.setEnabled(true);
            btn_record.setEnabled(true);
            btn_send.setEnabled(true);
            btn_send.setText(R.string.send);
        }

    }

    private static class myParas{
        URL url;
        UUID uuid;
        byte [] buffer;

        myParas(URL url,UUID uuid,byte buffer[]){
            this.url=url;
            this.uuid=uuid;
            this.buffer=buffer;
        }
    }

    private class taskExecute extends AsyncTask<myParas, Void, String> {
        protected String doInBackground(myParas... params) {
            try {
                //Your code goes here
                /*
                post data
                 */
                URL url=params[0].url;
                byte[] buffer=params[0].buffer;
                UUID uuid=params[0].uuid;
                httpConn=(HttpURLConnection)url.openConnection();
                httpConn.setRequestMethod("POST");
                httpConn.setRequestProperty("id", uuid.toString());
                httpConn.setRequestProperty("audioname", filename);
                httpConn.setRequestProperty("portBias",
                        Integer.toString(Character.getNumericValue(uuid.toString().charAt(0))));
                httpConn.setUseCaches(false);
                httpConn.setDoInput(true);
                httpConn.setDoOutput(true);
                DataOutputStream wr =new DataOutputStream(httpConn.getOutputStream());
                wr.write(buffer);
                wr.close();

                InputStream is =httpConn.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                StringBuilder response = new StringBuilder();
                String line;
                while((line=rd.readLine())!=null) {
                    response.append(line);
                    response.append('\r');
                }
                rd.close();

                /*
                Get data
                 */
                boolean loopGetResult=false;
                if (httpConn.getResponseCode() == httpConn.HTTP_OK) {
                    loopGetResult=true;
                }
                httpConn.disconnect();
                while (loopGetResult) {
                    String getURL=targetURL+"/status/"+uuid.toString();
                    url= new URL(getURL);
                    httpConn=(HttpURLConnection)url.openConnection();
                    httpConn.setRequestProperty("portBias",
                            Integer.toString(Character.getNumericValue(uuid.toString().charAt(0))));
                    is=httpConn.getInputStream();
                    rd = new BufferedReader(new InputStreamReader(is));
                    response = new StringBuilder();
                    while((line=rd.readLine())!=null) {
                        response.append(line);
                        response.append('\r');
                    }
                    rd.close();
                    JSONObject jsObj= new JSONObject(response.toString());
                    String status=jsObj.getString("status");
                    switch (status) {
                        case "TRANSCRIBED":
                            return "Result:\n"+readJson(jsObj);
                        case "FAILED":
                            return "!!!TRANSCRIBING FAILED!!!";
                        case "QUEUED":
                        case "TRANSCRIBING":
                            Thread.sleep(1000);
                            break;
                        default:
                            return "!!!UNKNOWN ERROR!!!";
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }


        private String readJson(JSONObject jsObj) {
            JSONObject jsBlock;
            String key;
            StringBuffer sBuffer=new StringBuffer();
            try {
                JSONObject js=jsObj.getJSONObject("channels").
                        getJSONObject("firstChannelLabel").
                        getJSONObject("lattice").getJSONObject("1").
                        getJSONObject("links");
                Iterator<?> keys = js.keys();
                while (keys.hasNext()) {
                    key=(String)keys.next();
                    jsBlock=js.getJSONObject(key);
                    if (jsBlock.getBoolean("best_path")) {
                        key=jsBlock.getString("word");
                        if (key.charAt(0)=='!')
                            continue;
                        sBuffer.append(key + ' ');
                    }
                }
            } catch (JSONException e) {
                Log.e("error","caused by json.");
            }
            return sBuffer.toString();
        }
    }
}
