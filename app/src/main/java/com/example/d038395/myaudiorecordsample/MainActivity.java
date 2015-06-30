package com.example.d038395.myaudiorecordsample;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.apache.http.client.methods.HttpPost;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;


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
        Button button=(Button)findViewById(R.id.start_record);
        TextView textView=(TextView)findViewById(R.id.text_record);
        if (wRecord) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
            button.setText(R.string.startRecord);
            textView.setText(R.string.record);
            wRecord =!wRecord;
        }
        else {
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

            button.setText("Stop");
            textView.setText("Recording, press to stop.");

            wRecord =!wRecord;
        }
    }
    public void onClickPlayBack(View view) {
        Button button=(Button) findViewById(R.id.start_playback);
        TextView textView = (TextView) findViewById(R.id.text_playback);
        if (wPlayback) {
            mPlayer.release();
            mPlayer = null;
            button.setText(R.string.startPlay);
            textView.setText(R.string.playback);
            wPlayback =!wPlayback;
        }
        else {
            mPlayer = new MediaPlayer();
            try {
                mPlayer.setDataSource(mFileName);
                mPlayer.prepare();
                mPlayer.start();
            } catch (IOException e) {
                Log.e(LOG_TAG, "prepare() failed");
                return;
            }
            button.setText("Stop");
            textView.setText("Press to stop playing.");
            wPlayback =!wPlayback;
        }
    }
    public void onClickSend(View view) throws IOException{
        final TextView textView = (TextView)findViewById(R.id.text_result);
        UUID uuid=UUID.randomUUID();
        File file= new File(mFileName);
        long fileLength=file.length();
        FileInputStream fis = new FileInputStream(file);
        final byte[] buffer= new byte[(int)fileLength];
        try{
            fis.read(buffer);
            URL url= new URL(targetURL);
            httpConn=(HttpURLConnection)url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("id", uuid.toString());
            httpConn.setRequestProperty("audioname",filename);
            httpConn.setRequestProperty("portBias", "0");
            httpConn.setUseCaches(false);
            httpConn.setDoInput(true);
            httpConn.setDoOutput(true);
            Thread thread = new Thread(new Runnable(){
                @Override
                public void run() {
                    try {
                        //Your code goes here
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
                        textView.setText(response.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            thread.start();

        }
        finally {
            if(httpConn!=null)httpConn.disconnect();
            fis.close();
        }

    }
}
