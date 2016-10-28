package com.example.hj.myrecording;


import android.app.Activity;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

public class AudioRecording extends Activity {

    /*------ setting audio recording ------*/
    private static final int SAMPLE_RATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private boolean isRecording = true;
    private AudioRecord recorder = null;
    private Thread recordingThread;
    private AudioTrack player;

    /*------ about socket communication ------*/
    public DatagramSocket socket;
    private int port = 7979;
    String IP = "192.168.0.4";


    String LOG_Audio = "Recording";
    String LOG_NW = "Networking";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_recording);

        setButtonHandlers();
        enableButtons(false);

    }

    private void setButtonHandlers() {
        ((Button) findViewById(R.id.record_btn)).setOnClickListener(btnClick);
        ((Button) findViewById(R.id.stop_btn)).setOnClickListener(btnClick);

    }

    private void enableButton(int id, boolean isEnable) {
        ((Button) findViewById(id)).setEnabled(isEnable);
    }

    private void enableButtons(boolean isRecording) {
        enableButton(R.id.record_btn, !isRecording);
        enableButton(R.id.stop_btn, isRecording);
    }


    private void startStreaming() {
        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {

                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

                try {
                    /*------about socket------*/
                    socket = new DatagramSocket();
                    Log.d(LOG_NW, "Socket Created!");
                    DatagramPacket packet;

                    InetAddress destination = InetAddress.getByName(IP);
                    Log.d(LOG_NW, "Address retrieved!");


                   /*------setting recording && playing------*/
                    //get MinBufferSize for audio recording
                    int Buffer_Size = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                            RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
                    Log.d(LOG_Audio, "Min buffer size is " + Buffer_Size);

                    if (Buffer_Size == AudioRecord.ERROR || Buffer_Size == AudioRecord.ERROR_BAD_VALUE) {
                        Buffer_Size = SAMPLE_RATE * 2;
                    }


                    recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                            SAMPLE_RATE, RECORDER_CHANNELS,
                            RECORDER_AUDIO_ENCODING, Buffer_Size);

                    if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                        Log.d(LOG_Audio, "Audio Record can't initialize!");
                        return;
                    }


                    player = new AudioTrack(AudioManager.STREAM_MUSIC,
                            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                            RECORDER_AUDIO_ENCODING, Buffer_Size,
                            AudioTrack.MODE_STREAM);
                    Log.d(LOG_Audio, "ready for playing music by using audiotrack");

                    player.setPlaybackRate(SAMPLE_RATE);

                    byte[] audioBuffer = new byte[Buffer_Size];
                    Log.d(LOG_Audio, "AudioBuffer created of size " + Buffer_Size);

                    recorder.startRecording();
                    Log.d(LOG_Audio, "Start Recording!");

                    player.play();
                    Log.d(LOG_Audio, "Start Playing!");

                    int count =0;

                    while (isRecording == true) {
                        //reading data from MIC into buffer
                        recorder.read(audioBuffer, 0, audioBuffer.length);
                        player.write(audioBuffer, 0, audioBuffer.length);


                        //putting buffer in the packet
                        packet = new DatagramPacket(audioBuffer, audioBuffer.length, destination, port);

                        socket.send(packet);
                        count++;
                        Log.d(LOG_NW, "packet sending to  " + destination + " with port : " + port);


                    }
                    Log.d(LOG_NW, "total # of packet  " + count);

                    if(isRecording == false) {
                        byte[] stopBuffer = ("stop").getBytes();
                        DatagramPacket stop_packet = new DatagramPacket(stopBuffer, stopBuffer.length, destination, port);

                        socket.send(stop_packet);
                        Log.d(LOG_NW, "send stop message to server!");
                        socket.close();
                        Log.d(LOG_NW, "socket close!");
                    }

                } catch (UnknownHostException e) {
                    Log.d(LOG_Audio, "UnknownHostException");
                } catch (IOException e) {
                    Log.d(LOG_Audio, "IOException");
                }
            }
        }); // end of recordingThread

        recordingThread.start();
    }


    private void stopRecording() throws UnknownHostException {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;

            recorder.stop();
            player.pause();
            recorder.release();
            player.release();
            Log.d(LOG_Audio, "Recorder && Player Release!");

            recorder = null;
            player = null;
            recordingThread = null;
        }
    }


    private View.OnClickListener btnClick = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.record_btn: {
                    enableButtons(true);
                    startStreaming();
                    break;
                }
                case R.id.stop_btn: {
                    enableButtons(false);
                    try {
                        stopRecording();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    break;
                }

            }
        }
    };

    // onClick of back button finishes the activity.
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }


}
