package edu.nju.ics.btsona;


import java.util.Set;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("NewApi")
public class MainActivity extends ActionBarActivity implements OnClickListener
{

	public static final String folderName = "microphone-sona";

	/**
	 * Buttons
	 */
	private Button button_record_start, button_record_stop;
	private AudioManager m_amAudioManager;
	/**
	 * Recorder and Player
	 */
	private AudioRecorder audioRecorderMic;
	private BTRecorder btRecorderMic;
	private AudioPlayer audioPlayer;
	
	private boolean isBT;
	
	private TextView tv_l;
	private TextView tv_d;

	private BluetoothHeadset bhs;
	private static BluetoothAdapter mAdapter;

	private Handler uiHandler;
    private ThreadWithLooper thread;
    private Runnable showRunable;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		

		this.button_record_start = (Button) findViewById(R.id.button_start);
		this.button_record_stop = (Button) findViewById(R.id.button_stop);
		this.button_record_start.setOnClickListener(this);
		this.button_record_stop.setOnClickListener(this);

		this.button_record_start.setClickable(true);
		this.button_record_stop.setClickable(false);

		this.m_amAudioManager = (AudioManager) this.getBaseContext()
				.getSystemService(Context.AUDIO_SERVICE);

		tv_l = (TextView) findViewById(R.id.LighterView);
		tv_d = (TextView) findViewById(R.id.DeepBreathView);
		
		this.audioRecorderMic = new AudioRecorder(tv_l, tv_d);
		this.btRecorderMic = new BTRecorder(tv_l, tv_d);
		this.audioPlayer = new AudioPlayer();

		this.isBT = false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void turnOnBluetooth() {

		final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				int state = intent.getIntExtra(
						AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
				if (AudioManager.SCO_AUDIO_STATE_CONNECTED == state) {
					System.err.println("bluetooth connected");
					unregisterReceiver(this);

				} else if (AudioManager.SCO_AUDIO_STATE_DISCONNECTED == state) {
					System.err.println("bluetooth disconnected");

				}
			}
		};
		registerReceiver(broadcastReceiver, new IntentFilter(
				AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
		// Start the timer

		try {

			// m_amAudioManager.setMode(AudioManager.MODE_INVALID);
			// Android 2.2 onwards supports BT SCO for non-voice call
			// use
			// case
			// Check the Android version whether it supports or not.

			if (m_amAudioManager.isBluetoothScoAvailableOffCall()) {
				m_amAudioManager.setStreamSolo(AudioManager.STREAM_MUSIC, true);
				if (m_amAudioManager.isBluetoothScoOn()) {
					m_amAudioManager.stopBluetoothSco();
					m_amAudioManager.startBluetoothSco();
					System.err.println("Bluetooth SCO On!");
				} else {
					System.err.println("Bluetooth Sco Off!");
					m_amAudioManager.startBluetoothSco();
				}

			} else {
				System.err.println("Bluetooth SCO not available");
			}
		} catch (Exception e) {
			System.err.println("sco elsepart startBluetoothSCO " + e);
			unregisterReceiver(broadcastReceiver);
		}
	}

	private void turnOffBluetooth() {
		this.m_amAudioManager.setMode(AudioManager.MODE_NORMAL);
		this.m_amAudioManager.stopBluetoothSco();
		this.m_amAudioManager.setBluetoothScoOn(false);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.button_start:
	        uiHandler=new Handler(){
	            @Override
	            public void handleMessage(Message msg) {
	                switch(msg.what){
	                case Messages.MSG_LIGHTER:
	                	//打开蓝牙并且关闭audiorecorder
	                	System.out.println("aaa");
	                	mAdapter = BluetoothAdapter.getDefaultAdapter();
	                	mAdapter.enable();
	                	audioRecorderMic.stopRecording();
	                	turnOffBluetooth();
	                	uiHandler.removeCallbacks(showRunable);
	                	
	                	//10s延迟，用于蓝牙连接
	                	try {
							Thread.sleep(10000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	                	
	                	//重新尝试连接蓝牙sco，并且打开btrecorder
	                	turnOnBluetooth();
	                	audioPlayer.startPlay(m_amAudioManager);
	                	btRecorderMic.setHandler(uiHandler);
	                	
	                	btRecorderMic.startRecording(folderName, "btrecord"
	        					+ AudioRecorder.AUDIO_RECORDER_FILE_EXT_WAV,
	        					MediaRecorder.AudioSource.MIC);
	                	isBT = true;
	                    break;
	                }
	            }
	        };
	        
	        
			this.turnOnBluetooth();
			
			//第一次点击是否开启了蓝牙
			mAdapter = BluetoothAdapter.getDefaultAdapter();
			if(!mAdapter.isEnabled()){
				audioRecorderMic.setHandler(uiHandler);
				
				audioRecorderMic.startRecording(this.folderName, "record"
						+ AudioRecorder.AUDIO_RECORDER_FILE_EXT_WAV,
						MediaRecorder.AudioSource.MIC);
			}
			else{
				btRecorderMic.setHandler(uiHandler);
				
				btRecorderMic.startRecording(this.folderName, "btrecord"
						+ AudioRecorder.AUDIO_RECORDER_FILE_EXT_WAV,
						MediaRecorder.AudioSource.MIC);
			}
			
			this.button_record_start.setClickable(false);
			this.button_record_stop.setClickable(true);

			this.button_record_start.setBackgroundColor(Color.LTGRAY);
			this.button_record_stop.setBackgroundColor(Color.YELLOW);

			break;
		case R.id.button_stop:
			this.button_record_start.setClickable(true);
			this.button_record_stop.setClickable(false);

			this.button_record_start.setBackgroundColor(Color.YELLOW);
			this.button_record_stop.setBackgroundColor(Color.LTGRAY);

			mAdapter = BluetoothAdapter.getDefaultAdapter();
			if(!mAdapter.isEnabled())
				this.audioRecorderMic.stopRecording();
			else{
				this.audioPlayer.stopPlay();
				this.btRecorderMic.stopRecording();
			}
				
			
			this.turnOffBluetooth();
			
			uiHandler.removeCallbacks(showRunable);
			
			break;
		}
	}
}
